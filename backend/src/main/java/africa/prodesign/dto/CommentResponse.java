package africa.prodesign.dto;

import java.time.Instant;

public record CommentResponse(
        String id,
        String projectId,
        String authorId,
        double canvasX,
        double canvasY,
        String text,
        boolean resolved,
        Instant createdAt
) {}
