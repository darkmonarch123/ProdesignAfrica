package africa.prodesign.dto;

public record ComplianceCheckResult(
        String code,
        String label,
        String status, // OK | WARN | FAIL | INFO
        String detail
) {}
