package africa.prodesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        @NotNull Double canvasX,
        @NotNull Double canvasY,
        @NotBlank String text
) {}
