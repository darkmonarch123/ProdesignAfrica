package africa.prodesign.dto;

import jakarta.validation.constraints.NotBlank;

public record SnapshotRequest(
        @NotBlank String schemaVersion,
        @NotBlank String canvasStateJson,
        Boolean isAutoSave
) {}
