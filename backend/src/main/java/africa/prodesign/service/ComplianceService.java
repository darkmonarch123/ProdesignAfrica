package africa.prodesign.service;

import africa.prodesign.dto.ComplianceCheckResult;
import africa.prodesign.dto.ComplianceReportResponse;
import africa.prodesign.entity.ComplianceRuleSet;
import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.entity.Project;
import africa.prodesign.repository.ComplianceRuleSetRepository;
import africa.prodesign.repository.DrawingSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the building footprint drawn in a project's latest snapshot against
 * its assigned ComplianceRuleSet.
 *
 * SIMPLIFYING ASSUMPTION: canvas coordinates are treated as plot-relative, with
 * the plot rectangle spanning (0,0) to (plotWidthMeters, plotDepthMeters) and
 * the "front" (road-facing) edge at y=0. This lets setback/coverage/ROW checks
 * run against real drawn geometry without requiring a dedicated plot-boundary
 * drawing tool yet — that's the natural next step to make this fully accurate
 * for irregular or rotated plots.
 */
@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ComplianceRuleSetRepository ruleSetRepository;
    private final DrawingSnapshotRepository snapshotRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ComplianceReportResponse evaluate(String userId, String projectId) {
        Project project = projectService.requireAccess(userId, projectId);
        List<ComplianceCheckResult> checks = new ArrayList<>();

        if (project.getComplianceRuleCode() == null || project.getComplianceRuleCode().isBlank()) {
            checks.add(info("RULESET", "No compliance ruleset selected for this project."));
            return report(project, checks);
        }

        ComplianceRuleSet rules = ruleSetRepository.findById(project.getComplianceRuleCode()).orElse(null);
        if (rules == null) {
            checks.add(info("RULESET", "Selected ruleset '" + project.getComplianceRuleCode() + "' is not recognized."));
            return report(project, checks);
        }

        if (project.getPlotWidthMeters() == null || project.getPlotDepthMeters() == null) {
            checks.add(info("PLOT", "Set plot width and depth in project settings to enable setback checks."));
            return report(project, checks);
        }

        DrawingSnapshot snapshot = snapshotRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
        if (snapshot == null) {
            checks.add(info("DRAWING", "Draw your building on the canvas to run compliance checks."));
            return report(project, checks);
        }

        Geometry geometry = parseGeometry(snapshot.getCanvasStateJson());
        if (geometry.isEmpty()) {
            checks.add(info("DRAWING", "Draw your building on the canvas to run compliance checks."));
            return report(project, checks);
        }

        double plotWidth = project.getPlotWidthMeters();
        double plotDepth = project.getPlotDepthMeters();

        double frontSetback = geometry.minY;
        double rearSetback = plotDepth - geometry.maxY;
        double leftSetback = geometry.minX;
        double rightSetback = plotWidth - geometry.maxX;
        double sideSetback = Math.min(leftSetback, rightSetback);
        double coveragePercent = plotWidth * plotDepth > 0 ? (geometry.footprintArea / (plotWidth * plotDepth)) * 100 : 0;

        checks.add(setbackCheck("FRONT_SETBACK", "Front setback", frontSetback, rules.getFrontSetbackMeters()));
        checks.add(setbackCheck("REAR_SETBACK", "Rear setback", rearSetback, rules.getRearSetbackMeters()));
        checks.add(setbackCheck("SIDE_SETBACK", "Side setback", sideSetback, rules.getSideSetbackMeters()));
        checks.add(setbackCheck("ROW_CLEARANCE", "ROW clearance", frontSetback, rules.getRoadReserveMeters()));
        checks.add(coverageCheck(coveragePercent, rules.getMaxPlotCoveragePercent()));
        checks.add(heightCheck(geometry.maxWallHeight, rules.getMaxHeightMeters()));

        return report(project, checks);
    }

    private ComplianceCheckResult setbackCheck(String code, String label, double actual, double required) {
        String status;
        if (actual < 0) {
            status = "FAIL"; // building extends outside the plot boundary entirely
        } else if (actual >= required) {
            status = "OK";
        } else if (actual >= required - 0.3) {
            status = "WARN";
        } else {
            status = "FAIL";
        }
        String detail = String.format("%.2fm (required %.2fm)", actual, required);
        return new ComplianceCheckResult(code, label, status, detail);
    }

    private ComplianceCheckResult coverageCheck(double actualPercent, double maxPercent) {
        String status;
        if (actualPercent <= maxPercent) {
            status = "OK";
        } else if (actualPercent <= maxPercent + 5) {
            status = "WARN";
        } else {
            status = "FAIL";
        }
        String detail = String.format("%.0f%% (max %.0f%%)", actualPercent, maxPercent);
        return new ComplianceCheckResult("PLOT_COVERAGE", "Plot coverage", status, detail);
    }

    private ComplianceCheckResult heightCheck(double actualHeight, double maxHeight) {
        String status;
        if (actualHeight <= maxHeight) {
            status = "OK";
        } else if (actualHeight <= maxHeight + 0.3) {
            status = "WARN";
        } else {
            status = "FAIL";
        }
        String detail = String.format("%.2fm (max %.2fm)", actualHeight, maxHeight);
        return new ComplianceCheckResult("MAX_HEIGHT", "Building height", status, detail);
    }

    private ComplianceCheckResult info(String code, String message) {
        return new ComplianceCheckResult(code, "Info", "INFO", message);
    }

    private ComplianceReportResponse report(Project project, List<ComplianceCheckResult> checks) {
        return new ComplianceReportResponse(project.getId(), project.getComplianceRuleCode(), checks, Instant.now());
    }

    private Geometry parseGeometry(String canvasStateJson) {
        Geometry geometry = new Geometry();
        try {
            JsonNode root = objectMapper.readTree(canvasStateJson);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) return geometry;

            for (JsonNode el : elements) {
                String type = el.path("type").asText("");
                if ("WALL".equals(type)) {
                    double x1 = el.path("x1").asDouble();
                    double y1 = el.path("y1").asDouble();
                    double x2 = el.path("x2").asDouble();
                    double y2 = el.path("y2").asDouble();
                    double height = el.path("height").asDouble(3.0);
                    geometry.expand(x1, y1);
                    geometry.expand(x2, y2);
                    geometry.maxWallHeight = Math.max(geometry.maxWallHeight, height);
                    geometry.hasElements = true;
                } else if ("ROOM".equals(type)) {
                    JsonNode points = el.get("points");
                    if (points != null && points.isArray() && points.size() >= 3) {
                        List<double[]> pts = new ArrayList<>();
                        for (JsonNode p : points) {
                            double x = p.path("x").asDouble();
                            double y = p.path("y").asDouble();
                            pts.add(new double[]{x, y});
                            geometry.expand(x, y);
                        }
                        geometry.footprintArea += shoelaceArea(pts);
                        geometry.hasElements = true;
                    }
                }
            }
        } catch (Exception e) {
            // Malformed snapshot JSON — treat as no geometry rather than failing the request.
            return new Geometry();
        }
        return geometry;
    }

    private double shoelaceArea(List<double[]> points) {
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            double[] a = points.get(i);
            double[] b = points.get((i + 1) % points.size());
            area += a[0] * b[1];
            area -= b[0] * a[1];
        }
        return Math.abs(area) / 2;
    }

    private static class Geometry {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double footprintArea = 0;
        double maxWallHeight = 0;
        boolean hasElements = false;

        void expand(double x, double y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        boolean isEmpty() {
            return !hasElements;
        }
    }
}
