package africa.prodesign.controller;

import africa.prodesign.dto.AiLayoutRequest;
import africa.prodesign.dto.AiLayoutResponse;
import africa.prodesign.dto.ErrorResponse;
import africa.prodesign.entity.Project;
import africa.prodesign.service.AiLayoutService;
import africa.prodesign.service.ProjectService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiLayoutController {

    private final AiLayoutService aiLayoutService;
    private final ProjectService projectService;

    @PostMapping("/api/projects/{projectId}/ai-layout")
    @RateLimiter(name = "aiLayout", fallbackMethod = "rateLimited")
    public ResponseEntity<AiLayoutResponse> generate(
            Authentication auth,
            @PathVariable String projectId,
            @Valid @RequestBody AiLayoutRequest request
    ) {
        String userId = (String) auth.getPrincipal();
        // Requires edit access, not just view access — generation would overwrite the drawing.
        projectService.requireEditAccess(userId, projectId);
        Project project = projectService.requireAccess(userId, projectId);
        return ResponseEntity.ok(aiLayoutService.generate(project, request));
    }

    // Resilience4j fallback — signature must mirror the guarded method plus the thrown exception.
    public ResponseEntity<ErrorResponse> rateLimited(
            Authentication auth, String projectId, AiLayoutRequest request, RequestNotPermitted ex
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("You've reached the AI layout generation limit (5 per hour). Try again later."));
    }
}
