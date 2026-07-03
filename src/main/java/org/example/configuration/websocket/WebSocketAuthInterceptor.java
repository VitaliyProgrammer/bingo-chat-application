package org.example.configuration.websocket;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetailsService;
import org.example.security.UserSecurity;
import org.example.security.jwt.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authenticationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authenticationHeader != null && authenticationHeader.startsWith("Bearer ")) {

                String token = authenticationHeader.substring(7);

                String email = jwtUtil.getUsernameFromToken(token);

                UserSecurity userDetails =
                        (UserSecurity) userDetailsService.loadUserByUsername(email);

                Long userId = userDetails.getId();

                accessor.setUser(new WebSocketPrincipal(userId));
            }
        }

        return message;
    }
}
