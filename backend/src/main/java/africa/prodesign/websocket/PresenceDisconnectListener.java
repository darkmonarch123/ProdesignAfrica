package africa.prodesign.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for STOMP session disconnects (tab close, network drop, browser
 * crash) so presence is cleaned up even when the client never gets a chance
 * to send an explicit "leave" message.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceDisconnectListener {

    private final PresenceRegistry presenceRegistry;
    private final CollabController collabController;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        PresenceEntry removed = presenceRegistry.remove(sessionId);
        if (removed != null) {
            log.debug("User {} disconnected from project {}", removed.userId(), removed.projectId());
            collabController.broadcastPresence(removed.projectId());
        }
    }
}
