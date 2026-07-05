package africa.prodesign.controller;

import africa.prodesign.dto.*;
import africa.prodesign.enums.CollabRole;
import africa.prodesign.service.CollaboratorService;
import africa.prodesign.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CollaboratorService collaboratorService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(Authentication auth) {
        return ResponseEntity.ok(projectService.listForUser(userId(auth)));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(Authentication auth, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(userId(auth), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(projectService.get(userId(auth), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(Authentication auth, @PathVariable String id,
                                                    @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(userId(auth), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable String id) {
        projectService.delete(userId(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<List<CollaboratorResponse>> listCollaborators(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(collaboratorService.list(userId(auth), id));
    }

    @PostMapping("/{id}/collaborators")
    public ResponseEntity<CollaboratorResponse> invite(Authentication auth, @PathVariable String id,
                                                         @Valid @RequestBody CollaboratorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collaboratorService.invite(userId(auth), id, request));
    }

    @PutMapping("/{id}/collaborators/{userId}")
    public ResponseEntity<CollaboratorResponse> updateRole(Authentication auth, @PathVariable String id,
                                                             @PathVariable("userId") String targetUserId,
                                                             @RequestBody CollabRoleUpdate body) {
        return ResponseEntity.ok(collaboratorService.updateRole(userId(auth), id, targetUserId, body.role()));
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable String id,
                                        @PathVariable("userId") String targetUserId) {
        collaboratorService.remove(userId(auth), id, targetUserId);
        return ResponseEntity.noContent().build();
    }

    public record CollabRoleUpdate(CollabRole role) {}

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
