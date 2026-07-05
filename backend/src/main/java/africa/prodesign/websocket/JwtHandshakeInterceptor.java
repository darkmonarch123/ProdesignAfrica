package africa.prodesign.websocket;

import africa.prodesign.entity.User;
import africa.prodesign.repository.UserRepository;
import africa.prodesign.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * Authenticates the WebSocket handshake using a JWT access token passed as a
 * `?token=` query parameter (browsers can't set custom headers on the SockJS
 * handshake request, so a query param is the standard workaround). Rejects
 * the handshake outright if the token is missing or invalid, rather than
 * allowing an anonymous connection through.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            log.debug("WebSocket handshake rejected: no token provided");
            return false;
        }
        if (!jwtService.isTokenValid(token) || !"access".equals(jwtService.extractTokenType(token))) {
            log.debug("WebSocket handshake rejected: invalid or expired token");
            return false;
        }
        String userId = jwtService.extractUserId(token);
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.debug("WebSocket handshake rejected: user {} no longer exists", userId);
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("fullName", user.get().getFullName());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
