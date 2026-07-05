package africa.prodesign.service;

import africa.prodesign.dto.CommentRequest;
import africa.prodesign.dto.CommentResponse;
import africa.prodesign.entity.DrawingComment;
import africa.prodesign.repository.DrawingCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final DrawingCommentRepository commentRepository;
    private final ProjectService projectService;

    public List<CommentResponse> list(String userId, String projectId) {
        projectService.requireAccess(userId, projectId);
        return commentRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse create(String userId, String projectId, CommentRequest request) {
        projectService.requireEditAccess(userId, projectId);
        DrawingComment comment = DrawingComment.builder()
                .projectId(projectId)
                .authorId(userId)
                .canvasX(request.canvasX())
                .canvasY(request.canvasY())
                .text(request.text())
                .build();
        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse resolve(String userId, String projectId, String commentId) {
        projectService.requireEditAccess(userId, projectId);
        DrawingComment comment = getOwnedComment(projectId, commentId);
        comment.setResolved(true);
        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void delete(String userId, String projectId, String commentId) {
        DrawingComment comment = getOwnedComment(projectId, commentId);
        var project = projectService.requireAccess(userId, projectId);
        boolean isOwner = project.getOwnerId().equals(userId);
        boolean isAuthor = comment.getAuthorId().equals(userId);
        if (!isOwner && !isAuthor) {
            throw new AccessDeniedException("Only the project owner or comment author can delete this comment");
        }
        commentRepository.delete(comment);
    }

    private DrawingComment getOwnedComment(String projectId, String commentId) {
        DrawingComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!comment.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Comment does not belong to this project");
        }
        return comment;
    }

    private CommentResponse toResponse(DrawingComment c) {
        return new CommentResponse(c.getId(), c.getProjectId(), c.getAuthorId(),
                c.getCanvasX(), c.getCanvasY(), c.getText(), c.isResolved(), c.getCreatedAt());
    }
}
