package org.example.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Duration;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.repository.ChatRepository;
import org.example.security.audit.SecurityAuditService;
import org.example.security.websocket.exception.WebSocketAccessDeniedException;
import org.example.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class WebSocketSubscriptionGuardTest {

    private static final Long USER_ID = 1L;
    private static final String SEND_KEY = "websocket:send:" + USER_ID;
    private static final String SEND_DESTINATION = "/app/chat.send";

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private ApplicationMetricsService metricsService;

    @InjectMocks
    private WebSocketSubscriptionGuard guard;

    @Test
    void preSend_chatSendWithinLimit_passesThrough() {

        when(redisService.isAllowed(SEND_KEY, 20, Duration.ofSeconds(10))).thenReturn(true);

        Message<?> message = chatSendMessage();

        Message<?> result = guard.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    void preSend_chatSendOverLimit_throwsAndAudits() {

        when(redisService.isAllowed(SEND_KEY, 20, Duration.ofSeconds(10))).thenReturn(false);

        Message<?> message = chatSendMessage();

        assertThatThrownBy(() -> guard.preSend(message, null))
                .isInstanceOf(WebSocketAccessDeniedException.class);

        verify(securityAuditService).webSocketDenied(USER_ID, SEND_DESTINATION,
                "Too many messages!");
        verify(metricsService).incrementWebSocketDenied();
    }

    @Test
    void preSend_otherDestination_isIgnored() {

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.typing");
        accessor.setUser(userPrincipal());
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = guard.preSend(message, null);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(redisService);
    }

    @Test
    void preSend_chatSendUnauthenticated_throwsWithoutRateCheck() {

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(SEND_DESTINATION);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> guard.preSend(message, null))
                .isInstanceOf(WebSocketAccessDeniedException.class);

        verify(redisService, never()).isAllowed(SEND_KEY, 20, Duration.ofSeconds(10));
    }

    private Message<?> chatSendMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(SEND_DESTINATION);
        accessor.setUser(userPrincipal());
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal userPrincipal() {
        return () -> String.valueOf(USER_ID);
    }
}
