package africa.prodesign.controller;

import africa.prodesign.dto.*;
import africa.prodesign.service.CommentService;
import africa.prodesign.service.SnapshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class EditorController {

    private final SnapshotService snapshotService;
    private final CommentService commentService;

    @GetMapping("/snapshot/latest")
    public ResponseEntity<SnapshotResponse> latestSnapshot(Authentication auth, @PathVariable String projectId) {
        SnapshotResponse response = snapshotService.latest(userId(auth), projectId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @PostMapping("/snapshot")
    public ResponseEntity<SnapshotResponse> saveSnapshot(Authentication auth, @PathVariable String projectId,
                                                           @Valid @RequestBody SnapshotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(snapshotService.save(userId(auth), projectId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SnapshotResponse>> history(Authentication auth, @PathVariable String projectId) {
        return ResponseEntity.ok(snapshotService.history(userId(auth), projectId));
    }

    @PostMapping("/restore/{versionId}")
    public ResponseEntity<SnapshotResponse> restore(Authentication auth, @PathVariable String projectId,
                                                      @PathVariable String versionId) {
        return ResponseEntity.ok(snapshotService.restore(userId(auth), projectId, versionId));
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponse>> listComments(Authentication auth, @PathVariable String projectId) {
        return ResponseEntity.ok(commentService.list(userId(auth), projectId));
    }

    @PostMapping("/comments")
    public ResponseEntity<CommentResponse> createComment(Authentication auth, @PathVariable String projectId,
                                                           @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(userId(auth), projectId, request));
    }

    @PutMapping("/comments/{commentId}/resolve")
    public ResponseEntity<CommentResponse> resolveComment(Authentication auth, @PathVariable String projectId,
                                                            @PathVariable String commentId) {
        return ResponseEntity.ok(commentService.resolve(userId(auth), projectId, commentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(Authentication auth, @PathVariable String projectId,
                                               @PathVariable String commentId) {
        commentService.delete(userId(auth), projectId, commentId);
        return ResponseEntity.noContent().build();
    }

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
