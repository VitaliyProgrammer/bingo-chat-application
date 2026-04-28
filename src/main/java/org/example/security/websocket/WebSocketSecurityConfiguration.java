package org.example.security.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketSecurityConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    private final WebSocketSubscriptionGuard webSocketSubscriptionGuard;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(jwtChannelInterceptor, webSocketSubscriptionGuard);
    }
}
