package com.improvement_app.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;

/**
 * Zabezpiecza ramki STOMP od klientów. Handshake /ws/** wymaga już zalogowania (WebSecurityConfig),
 * tu dodatkowo:
 * - CONNECT/SUBSCRIBE tylko z użytkownikiem przeniesionym z uwierzytelnionego handshake'u,
 * - SUBSCRIBE tylko na własne kolejki (/user/queue/**) — Spring mapuje je na sesje danego
 *   użytkownika, więc nie da się podsłuchać cudzych komunikatów,
 * - SEND zablokowany — kanał służy wyłącznie do powiadomień serwer → klient, więc klient
 *   nie może rozsyłać własnych wiadomości innym użytkownikom (także bezpośrednio do brokera).
 */
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    static final String ALLOWED_SUBSCRIPTION_PREFIX = "/user/queue/";

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
                    throw reject(accessor, "WebSocket requires an authenticated user");
                }
                if (command == StompCommand.SUBSCRIBE) {
                    String destination = accessor.getDestination();
                    if (destination == null || !destination.startsWith(ALLOWED_SUBSCRIPTION_PREFIX)) {
                        throw reject(accessor, "Subscription not allowed: " + destination);
                    }
                }
            }
            case SEND -> throw reject(accessor, "Clients cannot send messages over WebSocket");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK, NACK, heartbeat — bez ograniczeń
            }
        }
        return message;
    }

    // Spring loguje odrzucenie tylko jako ogólne "Failed to send message to ... clientInboundChannel",
    // bez przyczyny — dlatego zapisujemy ją sami
    private static AccessDeniedException reject(StompHeaderAccessor accessor, String reason) {
        Principal user = accessor.getUser();
        log.warn("WebSocket frame rejected: {} (command={}, destination={}, user={}, session={})",
                reason, accessor.getCommand(), accessor.getDestination(),
                user != null ? user.getName() : null, accessor.getSessionId());
        return new AccessDeniedException(reason);
    }
}
