package com.improvement_app.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Wysyła komunikaty postępu (np. importu z Google Drive) przez WebSocket tylko do użytkownika,
 * który uruchomił operację. Klient subskrybuje /user/queue/messages — Spring dostarcza wiadomość
 * wyłącznie do sesji WebSocket o tej samej nazwie użytkownika co z handshake'u (JWT).
 */
@Component
@RequiredArgsConstructor
public class UserLogNotifier {

    public static final String USER_LOG_DESTINATION = "/queue/messages";

    private final SimpMessagingTemplate messagingTemplate;

    public void send(String username, String message) {
        messagingTemplate.convertAndSendToUser(username, USER_LOG_DESTINATION, message);
    }
}
