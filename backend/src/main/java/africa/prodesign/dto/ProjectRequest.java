package africa.prodesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProjectRequest(
        @NotBlank String name,
        String location,
        String description,
        String complianceRuleCode,
        @Positive Double plotWidthMeters,
        @Positive Double plotDepthMeters
) {}
