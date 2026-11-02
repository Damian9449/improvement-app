package com.improvement_app.common.websocket;

import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.security.controllers.AuthCookieManager;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.jwt.JwtUtils;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.services.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.improvement_app.workouts.data.WorkoutTestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prawdziwe połączenia STOMP dwóch użytkowników (JWT w cookie, jak w przeglądarce) —
 * komunikaty postępu importu trafiają tylko do użytkownika, który uruchomił operację.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("UserLogNotifier — izolacja komunikatów WebSocket")
class UserLogNotifierWebSocketIntegrationTest {

    private static final String QUEUE = "/user/queue/messages";

    @LocalServerPort private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserLogNotifier userLogNotifier;
    @Autowired private SimpUserRegistry simpUserRegistry;

    @MockBean private EmailService emailService;

    private WebSocketStompClient stompClient;
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
    }

    @AfterEach
    void tearDown() {
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        stompClient.stop();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Komunikat dla A dociera tylko do A, nie do innego zalogowanego użytkownika B")
    void messageShouldReachOnlyItsRecipient() throws Exception {
        UserEntity a = userRepository.save(user("ws-a"));
        UserEntity b = userRepository.save(user("ws-b"));

        BlockingQueue<String> inboxA = subscribe(connect(a), QUEUE);
        BlockingQueue<String> inboxB = subscribe(connect(b), QUEUE);
        awaitSubscription("ws-a");
        awaitSubscription("ws-b");

        userLogNotifier.send("ws-a", "(1/1) Import treningu: A");

        assertThat(inboxA.poll(5, TimeUnit.SECONDS)).isEqualTo("(1/1) Import treningu: A");
        assertThat(inboxB.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    @DisplayName("Subskrypcja /user/{inny}/queue/... jest odrzucana i nie pozwala podsłuchać cudzych komunikatów")
    void cannotEavesdropOnOtherUsersQueue() throws Exception {
        UserEntity a = userRepository.save(user("ws-a"));
        UserEntity b = userRepository.save(user("ws-b"));

        StompSession sessionA = connect(a);
        BlockingQueue<String> spyA = subscribe(sessionA, "/user/ws-b/queue/messages");
        BlockingQueue<String> inboxB = subscribe(connect(b), QUEUE);
        awaitSubscription("ws-b");

        // Interceptor odpowiada ramką ERROR, po której serwer zamyka sesję A
        awaitDisconnected(sessionA);

        userLogNotifier.send("ws-b", "tylko dla B");

        assertThat(inboxB.poll(5, TimeUnit.SECONDS)).isEqualTo("tylko dla B");
        assertThat(spyA.poll(1, TimeUnit.SECONDS)).isNull();
    }

    private StompSession connect(UserEntity user) throws Exception {
        String jwt = jwtUtils.generateJwtToken(user.getUsername(), List.of("ROLE_USER"));

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Cookie", AuthCookieManager.ACCESS_COOKIE + "=" + jwt);

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws/websocket", handshakeHeaders,
                        new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private BlockingQueue<String> subscribe(StompSession session, String destination) {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add((String) payload);
            }
        });
        return inbox;
    }

    // Prosty broker nie wysyła RECEIPT — czekamy, aż subskrypcja pojawi się w rejestrze użytkowników
    private void awaitSubscription(String username) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            SimpUser simpUser = simpUserRegistry.getUser(username);
            if (simpUser != null && simpUser.getSessions().stream().anyMatch(s -> !s.getSubscriptions().isEmpty())) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Subscription for " + username + " not registered in time");
    }

    private void awaitDisconnected(StompSession session) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (!session.isConnected()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Session was not closed after a forbidden subscription");
    }
}
