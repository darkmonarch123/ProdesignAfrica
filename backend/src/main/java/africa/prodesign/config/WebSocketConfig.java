package africa.prodesign.config;

import africa.prodesign.websocket.JwtHandshakeInterceptor;
import africa.prodesign.websocket.StompHandshakeHandler;
import africa.prodesign.websocket.WebSocketAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompHandshakeHandler stompHandshakeHandler;
    private final WebSocketAuthorizationInterceptor webSocketAuthorizationInterceptor;

    @Value("${prodesign.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigin)
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(stompHandshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Enforces project access control on every SUBSCRIBE, not just on SEND —
        // see WebSocketAuthorizationInterceptor's Javadoc for the gap this closes.
        registration.interceptors(webSocketAuthorizationInterceptor);
    }
}
