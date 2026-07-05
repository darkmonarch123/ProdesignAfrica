package africa.prodesign.websocket;

import africa.prodesign.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * Handles the three live-collaboration message types over STOMP:
 *  - join: register presence for this session, broadcast the updated roster
 *  - cursor: rebroadcast a lightweight cursor position to everyone else viewing the project
 *  - state: rebroadcast a client's full canvas state to everyone else viewing the project
 *
 * IMPORTANT LIMITATION: the "state" broadcast is last-write-wins with no
 * merge/CRDT logic — if two people edit at the same moment, whichever state
 * message arrives at a given client last simply overwrites what they had
 * locally. That's fine for "watch a colleague sketch live" and quick paired
 * editing, but it is not safe for two people actively drawing in the same
 * project at the same time — expect lost edits in that case. The durable
 * source of truth remains the autosave snapshot (SnapshotService), which is
 * unaffected by this — a lost live-broadcast edit is not a lost save, as
 * long as the person who made it is still connected long enough to autosave.
 *
 * Presence and cursors are also not access-controlled beyond "you have a
 * valid JWT" — anyone with a valid account who knows a project's id can
 * subscribe to its live channels, since STOMP SUBSCRIBE isn't intercepted
 * here. That's an acceptable gap for now given project ids are non-guessable
 * UUIDs, but it means presence/cursor data isn't enforced the way REST
 * endpoints are via requireAccess/requireEditAccess.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CollabController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceRegistry presenceRegistry;
    private final ProjectService projectService;

    @MessageMapping("/projects/{projectId}/join")
    public void join(@DestinationVariable String projectId, Principal principal,
                      SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal user = (StompPrincipal) principal;
        // Confirms the user has at least view access before letting them join a live session.
        projectService.requireAccess(user.getUserId(), projectId);

        String sessionId = headerAccessor.getSessionId();
        String color = PresenceRegistry.colorForUser(user.getUserId());
        presenceRegistry.register(sessionId, new PresenceEntry(user.getUserId(), user.getFullName(), color, sessionId, projectId));

        broadcastPresence(projectId);
    }

    @MessageMapping("/projects/{projectId}/cursor")
    public void cursor(@DestinationVariable String projectId, CursorInbound inbound, Principal principal) {
        StompPrincipal user = (StompPrincipal) principal;
        String color = PresenceRegistry.colorForUser(user.getUserId());
        CursorOutbound outbound = new CursorOutbound(user.getUserId(), user.getFullName(), color, inbound.x(), inbound.y());
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/cursors", outbound);
    }

    @MessageMapping("/projects/{projectId}/state")
    public void state(@DestinationVariable String projectId, StateInbound inbound, Principal principal) {
        StompPrincipal user = (StompPrincipal) principal;
        // Edit access, not just view access — a viewer's broadcast state shouldn't overwrite others' canvases.
        projectService.requireEditAccess(user.getUserId(), projectId);
        StateOutbound outbound = new StateOutbound(user.getUserId(), inbound.schemaVersion(), inbound.canvasStateJson());
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/state", outbound);
    }

    void broadcastPresence(String projectId) {
        List<PresenceOutbound.PresenceUser> users = presenceRegistry.activeForProject(projectId).stream()
                .map(e -> new PresenceOutbound.PresenceUser(e.userId(), e.fullName(), e.color()))
                .distinct()
                .toList();
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/presence", new PresenceOutbound(users));
    }
}
