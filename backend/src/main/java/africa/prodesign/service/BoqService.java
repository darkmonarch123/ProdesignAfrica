package africa.prodesign.service;

import africa.prodesign.dto.BoqLineItem;
import africa.prodesign.dto.BoqResponse;
import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.entity.MaterialRate;
import africa.prodesign.entity.Project;
import africa.prodesign.enums.Currency;
import africa.prodesign.repository.DrawingSnapshotRepository;
import africa.prodesign.repository.MaterialRateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a Bill of Quantities from a project's latest drawn geometry.
 *
 * SCOPE, STATED PLAINLY: the canvas schema currently models walls, rooms,
 * doors, and windows only — there is no foundation, footing, roofing,
 * electrical, or plumbing geometry to measure. This service therefore prices
 * blockwork, door/window openings, plastering, and floor tiling — the
 * quantities that can be honestly derived from what's actually drawn — and
 * says so explicitly in the response's `notes`, rather than inventing figures
 * for categories the drawing doesn't represent.
 *
 * NOTE ON DUPLICATION: the geometry-parsing logic here overlaps with
 * ComplianceService's canvas parsing. They were kept independent rather than
 * refactored into a shared parser to avoid touching ComplianceService's
 * tested behavior under time pressure — worth consolidating into a shared
 * `CanvasGeometryParser` in a later pass.
 */
@Service
@RequiredArgsConstructor
public class BoqService {

    private static final double DOOR_HEIGHT_M = 2.1;
    private static final double WINDOW_HEIGHT_M = 1.2;

    private final DrawingSnapshotRepository snapshotRepository;
    private final MaterialRateRepository materialRateRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BoqResponse compute(String userId, String projectId, Currency currency) {
        Project project = projectService.requireAccess(userId, projectId);
        List<String> notes = new ArrayList<>();
        notes.add("Covers blockwork, doors, windows, and floor tiling only — foundation, roofing, "
                + "electrical, and plumbing are not yet modeled in the drawing and are excluded from this estimate.");
        notes.add("Material rates are illustrative placeholder figures, not a live market pricing feed.");

        DrawingSnapshot snapshot = snapshotRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
        if (snapshot == null) {
            notes.add("No drawing found for this project yet.");
            return new BoqResponse(projectId, currency.name(), List.of(), 0, Instant.now(), notes);
        }

        Takeoff takeoff = parseTakeoff(snapshot.getCanvasStateJson());
        if (takeoff.isEmpty()) {
            notes.add("The drawing has no walls or rooms yet — draw your building to generate a BOQ.");
            return new BoqResponse(projectId, currency.name(), List.of(), 0, Instant.now(), notes);
        }

        List<BoqLineItem> lineItems = new ArrayList<>();
        double total = 0;

        total += addLineItem(lineItems, "BLOCKWORK_150MM", takeoff.netWallArea, currency);
        total += addLineItem(lineItems, "PLASTERING_2SIDES", takeoff.netWallArea * 2, currency);
        if (takeoff.doorCount > 0) {
            total += addLineItem(lineItems, "DOOR_STANDARD", takeoff.doorCount, currency);
        }
        if (takeoff.windowCount > 0) {
            total += addLineItem(lineItems, "WINDOW_STANDARD", takeoff.windowCount, currency);
        }
        if (takeoff.floorArea > 0) {
            total += addLineItem(lineItems, "FLOOR_TILING", takeoff.floorArea, currency);
        }

        return new BoqResponse(project.getId(), currency.name(), lineItems, round2(total), Instant.now(), notes);
    }

    private double addLineItem(List<BoqLineItem> lineItems, String itemCode, double quantity, Currency currency) {
        MaterialRate rate = materialRateRepository.findById(itemCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded material rate: " + itemCode));
        double unitRate = rateFor(rate, currency);
        double amount = round2(quantity * unitRate);
        lineItems.add(new BoqLineItem(rate.getCategory(), rate.getDescription(), round2(quantity), rate.getUnit(), unitRate, amount));
        return amount;
    }

    private double rateFor(MaterialRate rate, Currency currency) {
        return switch (currency) {
            case NGN -> rate.getRateNgn();
            case GHS -> rate.getRateGhs();
            case KES -> rate.getRateKes();
            case ZAR -> rate.getRateZar();
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Takeoff parseTakeoff(String canvasStateJson) {
        Takeoff takeoff = new Takeoff();
        try {
            JsonNode root = objectMapper.readTree(canvasStateJson);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) return takeoff;

            List<JsonNode> walls = new ArrayList<>();
            for (JsonNode el : elements) {
                String type = el.path("type").asText("");
                if ("WALL".equals(type)) {
                    walls.add(el);
                } else if ("ROOM".equals(type)) {
                    JsonNode points = el.get("points");
                    if (points != null && points.isArray() && points.size() >= 3) {
                        takeoff.floorArea += shoelaceArea(points);
                    }
                } else if ("DOOR".equals(type)) {
                    takeoff.doorCount++;
                } else if ("WINDOW".equals(type)) {
                    takeoff.windowCount++;
                }
            }

            for (JsonNode wall : walls) {
                double x1 = wall.path("x1").asDouble();
                double y1 = wall.path("y1").asDouble();
                double x2 = wall.path("x2").asDouble();
                double y2 = wall.path("y2").asDouble();
                double height = wall.path("height").asDouble(3.0);
                double length = Math.hypot(x2 - x1, y2 - y1);
                double grossArea = length * height;

                double openingsArea = 0;
                String wallId = wall.path("id").asText();
                for (JsonNode el : elements) {
                    String type = el.path("type").asText("");
                    if (("DOOR".equals(type) || "WINDOW".equals(type)) && wallId.equals(el.path("hostWallId").asText())) {
                        double width = el.path("width").asDouble();
                        double openingHeight = "DOOR".equals(type) ? DOOR_HEIGHT_M : WINDOW_HEIGHT_M;
                        openingsArea += width * openingHeight;
                    }
                }
                takeoff.netWallArea += Math.max(0, grossArea - openingsArea);
                takeoff.hasElements = true;
            }
            if (takeoff.floorArea > 0) takeoff.hasElements = true;
        } catch (Exception e) {
            return new Takeoff();
        }
        return takeoff;
    }

    private double shoelaceArea(JsonNode points) {
        List<double[]> pts = new ArrayList<>();
        for (JsonNode p : points) {
            pts.add(new double[]{p.path("x").asDouble(), p.path("y").asDouble()});
        }
        double area = 0;
        for (int i = 0; i < pts.size(); i++) {
            double[] a = pts.get(i);
            double[] b = pts.get((i + 1) % pts.size());
            area += a[0] * b[1];
            area -= b[0] * a[1];
        }
        return Math.abs(area) / 2;
    }

    private static class Takeoff {
        double netWallArea = 0;
        double floorArea = 0;
        int doorCount = 0;
        int windowCount = 0;
        boolean hasElements = false;

        boolean isEmpty() {
            return !hasElements;
        }
    }
}
