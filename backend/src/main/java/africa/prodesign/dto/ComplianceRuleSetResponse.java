package africa.prodesign.dto;

public record ComplianceRuleSetResponse(
        String code,
        String country,
        String region,
        String name,
        double frontSetbackMeters,
        double sideSetbackMeters,
        double rearSetbackMeters,
        double roadReserveMeters,
        double maxPlotCoveragePercent,
        double maxHeightMeters
) {}
