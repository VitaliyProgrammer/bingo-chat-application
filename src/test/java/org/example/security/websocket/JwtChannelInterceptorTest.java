package org.example.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.entity.User;
import org.example.exception.JwtTokenExpiredException;
import org.example.repository.UserRepository;
import org.example.security.jwt.JwtUtil;
import org.example.security.websocket.exception.WebSocketAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
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
class JwtChannelInterceptorTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String EMAIL = "user@example.com";
    private static final Long USER_ID = 1L;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtChannelInterceptor interceptor;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
    }

    @Test
    void preSend_connectWithValidToken_authenticatesSession() {
        when(jwtUtil.isValidToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Message<?> message = connectMessage(VALID_TOKEN);

        interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void preSend_connectWithExpiredToken_bubblesUpJwtTokenExpiredException() {
        when(jwtUtil.isValidToken(VALID_TOKEN))
                .thenThrow(new JwtTokenExpiredException("JWT token expired!"));

        Message<?> message = connectMessage(VALID_TOKEN);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(JwtTokenExpiredException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void preSend_connectWithoutAuthorizationHeader_throwsAccessDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(WebSocketAccessDeniedException.class);
    }

    @Test
    void preSend_connectWithNonBearerHeader_throwsAccessDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Basic somecredentials");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(WebSocketAccessDeniedException.class);
    }

    @Test
    void preSend_sendCommandWithExpiredToken_isNeverRevalidated() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
        accessor.setDestination("/app/chat.send");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
        verify(jwtUtil, never()).isValidToken(anyString());
    }

    private Message<?> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
