package africa.prodesign.websocket;

import africa.prodesign.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Closes a real gap in the collaboration feature: JwtHandshakeInterceptor and
 * the @MessageMapping methods in CollabController both check access on SEND
 * (joining, sending a cursor, sending a state update), but STOMP SUBSCRIBE
 * was never checked at all — any authenticated user who knew a project's
 * UUID could subscribe directly to its /topic/projects/{id}/presence,
 * /cursors, or /state channel and silently observe another team's live
 * cursors and edits without ever calling /app/.../join. This interceptor
 * closes that: every SUBSCRIBE to a project-scoped topic now goes through
 * the same ProjectService.requireAccess check as everything else.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern PROJECT_TOPIC_PATTERN =
            Pattern.compile("^/topic/projects/([^/]+)/(presence|cursors|state)$");

    private final ProjectService projectService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/projects/")) {
            return message; // not a project-scoped channel — nothing for this interceptor to check
        }

        Matcher matcher = PROJECT_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            log.warn("Rejecting SUBSCRIBE to unrecognized project-scoped destination: {}", destination);
            throw new MessagingException("Unrecognized subscription destination");
        }

        String projectId = matcher.group(1);
        Principal principal = accessor.getUser();
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            throw new MessagingException("Not authenticated");
        }

        try {
            projectService.requireAccess(stompPrincipal.getUserId(), projectId);
        } catch (Exception e) {
            log.debug("Rejecting SUBSCRIBE: user {} has no access to project {}", stompPrincipal.getUserId(), projectId);
            throw new MessagingException("You do not have access to this project's live channel");
        }

        return message;
    }
}
