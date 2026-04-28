package org.example.security.websocket;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.exception.UserNotFoundException;
import org.example.repository.UserRepository;
import org.example.security.jwt.JwtUtil;
import org.example.security.websocket.exception.WebSocketAccessDeniedException;
import org.example.security.websocket.model.WebSocketPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = extractToken(accessor);

            if (!jwtUtil.isValidToken(token)) {
                throw new WebSocketAccessDeniedException("Invalid token!");
            }

            String email = jwtUtil.getUsernameFromToken(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found!"));

            accessor.setUser(new WebSocketPrincipal(user.getId()));
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {

        List<String> authentication = accessor.getNativeHeader("Authorization");

        if (authentication == null || authentication.isEmpty()) {
            throw new WebSocketAccessDeniedException("Missing token!");
        }

        String header = authentication.get(0);

        if (!header.startsWith(BEARER_PREFIX)) {
            throw new WebSocketAccessDeniedException("Invalid authentication header!");
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
