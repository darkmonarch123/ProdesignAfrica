package africa.prodesign.controller;

import africa.prodesign.dto.ComplianceReportResponse;
import africa.prodesign.dto.ComplianceRuleSetResponse;
import africa.prodesign.entity.ComplianceRuleSet;
import africa.prodesign.repository.ComplianceRuleSetRepository;
import africa.prodesign.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceRuleSetRepository ruleSetRepository;
    private final ComplianceService complianceService;

    @GetMapping("/api/compliance/rulesets")
    public ResponseEntity<List<ComplianceRuleSetResponse>> listRuleSets() {
        List<ComplianceRuleSetResponse> response = ruleSetRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/projects/{projectId}/compliance")
    public ResponseEntity<ComplianceReportResponse> evaluate(Authentication auth, @PathVariable String projectId) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(complianceService.evaluate(userId, projectId));
    }

    private ComplianceRuleSetResponse toResponse(ComplianceRuleSet r) {
        return new ComplianceRuleSetResponse(r.getCode(), r.getCountry(), r.getRegion(), r.getName(),
                r.getFrontSetbackMeters(), r.getSideSetbackMeters(), r.getRearSetbackMeters(),
                r.getRoadReserveMeters(), r.getMaxPlotCoveragePercent(), r.getMaxHeightMeters());
    }
}
