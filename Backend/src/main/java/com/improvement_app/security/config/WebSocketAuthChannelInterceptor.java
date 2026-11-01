package com.improvement_app.security.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

/**
 * Zabezpiecza ramki STOMP od klientów. Handshake /ws/** wymaga już zalogowania (WebSecurityConfig),
 * tu dodatkowo:
 * - CONNECT/SUBSCRIBE tylko z użytkownikiem przeniesionym z uwierzytelnionego handshake'u,
 * - SUBSCRIBE tylko na tematy serwera (/topic/**),
 * - SEND zablokowany — kanał służy wyłącznie do powiadomień serwer → klient, więc klient
 *   nie może rozsyłać własnych wiadomości innym użytkownikom (także bezpośrednio na /topic/**).
 */
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    static final String ALLOWED_SUBSCRIPTION_PREFIX = "/topic/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT, STOMP, SUBSCRIBE -> {
                if (accessor.getUser() == null) {
                    throw new AccessDeniedException("WebSocket requires an authenticated user");
                }
                if (command == StompCommand.SUBSCRIBE) {
                    String destination = accessor.getDestination();
                    if (destination == null || !destination.startsWith(ALLOWED_SUBSCRIPTION_PREFIX)) {
                        throw new AccessDeniedException("Subscription not allowed: " + destination);
                    }
                }
            }
            case SEND -> throw new AccessDeniedException("Clients cannot send messages over WebSocket");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK, NACK, heartbeat — bez ograniczeń
            }
        }
        return message;
    }
}
