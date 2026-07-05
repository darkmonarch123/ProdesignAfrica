package africa.prodesign.dto;

import java.time.Instant;

public record SnapshotResponse(
        String id,
        String projectId,
        String schemaVersion,
        String canvasStateJson,
        String createdByUserId,
        boolean isAutoSave,
        Instant createdAt
) {}
