package africa.prodesign.dto;

import java.time.Instant;

public record ProjectResponse(
        String id,
        String name,
        String location,
        String description,
        String ownerId,
        String complianceRuleCode,
        Double plotWidthMeters,
        Double plotDepthMeters,
        Instant createdAt,
        Instant updatedAt
) {}
