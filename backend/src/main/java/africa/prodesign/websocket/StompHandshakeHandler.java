package africa.prodesign.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Converts the userId/fullName placed into the handshake attributes by
 * JwtHandshakeInterceptor into a real Principal, so `@MessageMapping` methods
 * and SimpMessagingTemplate can address individual users and controllers can
 * read `Principal.getName()` as the authenticated userId.
 */
@Component
public class StompHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");
        String fullName = (String) attributes.get("fullName");
        return new StompPrincipal(userId, fullName);
    }
}
