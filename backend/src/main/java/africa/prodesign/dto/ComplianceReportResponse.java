package africa.prodesign.dto;

import java.time.Instant;
import java.util.List;

public record ComplianceReportResponse(
        String projectId,
        String ruleSetCode,
        List<ComplianceCheckResult> checks,
        Instant evaluatedAt
) {}
