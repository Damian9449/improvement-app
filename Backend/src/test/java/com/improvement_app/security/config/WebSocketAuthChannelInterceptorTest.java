package com.improvement_app.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WebSocketAuthChannelInterceptor")
class WebSocketAuthChannelInterceptorTest {

    private final WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor();
    private final Principal user = new UsernamePasswordAuthenticationToken("user", null, List.of());

    @Test
    @DisplayName("CONNECT i SUBSCRIBE na /user/queue/** z zalogowanym użytkownikiem przechodzą")
    void shouldAllowAuthenticatedConnectAndSubscribe() {
        Message<?> connect = frame(StompCommand.CONNECT, user, null);
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, user, "/user/queue/messages");

        assertThat(interceptor.preSend(connect, null)).isSameAs(connect);
        assertThat(interceptor.preSend(subscribe, null)).isSameAs(subscribe);
    }

    @Test
    @DisplayName("CONNECT i SUBSCRIBE bez użytkownika są odrzucane")
    void shouldRejectAnonymousFrames() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/messages"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE poza /user/queue/** (np. broadcast /topic/**) jest odrzucany")
    void shouldRejectSubscriptionOutsideTopic() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user, "/app/message"), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user, "/topic/messages"), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, user, "/queue/messages"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SEND od klienta jest zawsze odrzucany — również do kolejki innego użytkownika")
    void shouldRejectClientSend() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, user, "/user/other/queue/messages"), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, user, "/app/message"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("DISCONNECT przechodzi bez ograniczeń")
    void shouldAllowDisconnect() {
        Message<?> disconnect = frame(StompCommand.DISCONNECT, null, null);
        assertThat(interceptor.preSend(disconnect, null)).isSameAs(disconnect);
    }

    private static Message<?> frame(StompCommand command, Principal user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setUser(user);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
