package africa.prodesign.dto;

import jakarta.validation.constraints.NotBlank;

public record UseTemplateRequest(
        @NotBlank String projectName,
        Double plotWidthMeters,
        Double plotDepthMeters
) {}
