package org.example.configuration.websocket;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;

    public WebSocketConfiguration(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {

        this.allowedOrigins = allowedOrigins.toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registration) {

        registration.addEndpoint("/websocket")
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registration) {

        registration.enableSimpleBroker("/topic", "/queue");
        registration.setApplicationDestinationPrefixes("/app");
        registration.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {

        registration.setMessageSizeLimit(64 * 1024);

        registration.setSendBufferSizeLimit(512 * 1024);
    }
}
