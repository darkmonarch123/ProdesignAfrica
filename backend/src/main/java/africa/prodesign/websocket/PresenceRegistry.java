package africa.prodesign.websocket;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory registry of who is actively connected to which project's editor
 * session. Keyed by WebSocket session id so a disconnect (tab close, network
 * drop) can clean up presence without requiring the client to explicitly
 * signal "leave". Fine for a single-instance deployment; a multi-instance
 * deployment would need this backed by Redis (pub/sub) instead.
 */
@Component
public class PresenceRegistry {

    private final Map<String, PresenceEntry> bySessionId = new ConcurrentHashMap<>();

    public void register(String sessionId, PresenceEntry entry) {
        bySessionId.put(sessionId, entry);
    }

    public PresenceEntry remove(String sessionId) {
        return bySessionId.remove(sessionId);
    }

    public Collection<PresenceEntry> activeForProject(String projectId) {
        return bySessionId.values().stream()
                .filter(e -> e.projectId().equals(projectId))
                .collect(Collectors.toList());
    }

    /** Deterministic color per user so the same person always gets the same cursor color. */
    public static String colorForUser(String userId) {
        String[] palette = {"#047857", "#C2692A", "#185FA5", "#D4A017", "#7C3AED", "#DB2777", "#0891B2", "#65A30D"};
        int hash = Math.abs(userId.hashCode());
        return palette[hash % palette.length];
    }
}
