package africa.prodesign.websocket;

import lombok.Getter;

import java.security.Principal;

/**
 * Minimal Principal implementation carrying the authenticated user's id and
 * display name across the WebSocket session, set during the STOMP handshake
 * by JwtHandshakeInterceptor + the custom HandshakeHandler in WebSocketConfig.
 */
@Getter
public class StompPrincipal implements Principal {

    private final String userId;
    private final String fullName;

    public StompPrincipal(String userId, String fullName) {
        this.userId = userId;
        this.fullName = fullName;
    }

    @Override
    public String getName() {
        return userId;
    }
}
