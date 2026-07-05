package africa.prodesign.template;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a valid, non-overlapping rectangular floor plan: an entrance
 * porch strip, a living/dining strip, and a back row of rooms (bedrooms +
 * kitchen + bathroom, plus a store and master ensuite on larger homes),
 * stacked front-to-back.
 *
 * This is deliberately simple and robust rather than clever: every generated
 * plan is a grid of rectangles sharing exact edges, which makes it trivial to
 * guarantee walls, rooms, and doors are all geometrically consistent and pass
 * GeometryValidator. The three LayoutStyle variants change back-row room
 * *ordering* (which room sits next to which), which changes real adjacency
 * and door placement — not just cosmetic labels — while reusing the same
 * reliable column-grid math.
 *
 * HONEST SCOPE NOTE (added after reviewing real reference floor plans): real
 * Nigerian/Kenyan house plans often have an entrance porch that only spans
 * part of the frontage (sometimes angled), a corridor with bedrooms doored
 * onto its side walls rather than a shared front wall, and a "sit-out"
 * distinct from the porch. This generator's porch is a full-width shallow
 * strip (a simplification, not a protruding partial-width porch), and
 * bedrooms still door directly onto the living/dining divider rather than
 * onto a side corridor. Modeling true L-shaped/angled footprints and
 * corridor-side doors would need a polygon-based generator rather than this
 * rectangle-grid one — a real next step, not attempted here to avoid
 * risking geometry validity across a 1000+ template catalog.
 *
 * Returns null if the requested plot is too small to fit the requested room
 * count with sane minimum dimensions, rather than generating a degenerate
 * layout.
 */
public class ProceduralTemplateGenerator {

    private static final double BEDROOM_MIN_WIDTH = 2.6;
    private static final double KITCHEN_MIN_WIDTH = 2.2;
    private static final double BATHROOM_MIN_WIDTH = 1.8;
    private static final double ENSUITE_MIN_WIDTH = 1.8;
    private static final double STORE_MIN_WIDTH = 1.6;
    private static final double LIVING_MIN_WIDTH = 3.0;
    private static final double DINING_MIN_WIDTH = 2.6;
    private static final double PORCH_DEPTH = 1.6;
    private static final double FRONT_DEPTH_MIN = 3.0;
    private static final double BACK_DEPTH_MIN = 2.5;
    private static final double PERIMETER_THICKNESS = 0.2;
    private static final double INTERNAL_THICKNESS = 0.15;

    public enum LayoutStyle { A, B, C }

    public record WallData(String id, double x1, double y1, double x2, double y2, double thickness) {}
    public record RoomData(String id, String label, List<double[]> points, String floorColor) {}
    public record DoorData(String id, String type, String hostWallId, double x, double width) {}

    public record GeneratedLayout(List<WallData> walls, List<RoomData> rooms, List<DoorData> doors) {}

    private int wallIdx;
    private int roomIdx;
    private int doorIdx;

    public GeneratedLayout generate(int bedrooms, double plotWidth, double plotDepth, LayoutStyle style) {
        wallIdx = 1;
        roomIdx = 1;
        doorIdx = 1;

        List<ColumnSpec> backColumns = orderBackRowColumns(bedrooms, style);
        double backMinWidthSum = backColumns.stream().mapToDouble(c -> c.minWidth).sum();
        double frontMinWidthSum = LIVING_MIN_WIDTH + DINING_MIN_WIDTH;
        double minWidthSum = Math.max(backMinWidthSum, LIVING_MIN_WIDTH); // living alone must always fit
        if (plotWidth < minWidthSum + 0.5) return null;

        double remainingDepth = plotDepth - PORCH_DEPTH;
        if (remainingDepth < FRONT_DEPTH_MIN + BACK_DEPTH_MIN) return null;

        double frontDepth = round(Math.max(FRONT_DEPTH_MIN, Math.min(remainingDepth - BACK_DEPTH_MIN, remainingDepth * 0.42)));
        double backDepth = remainingDepth - frontDepth;
        if (backDepth < BACK_DEPTH_MIN) return null;
        if (backMinWidthSum > plotWidth + 0.5) return null;

        List<WallData> walls = new ArrayList<>();
        List<RoomData> rooms = new ArrayList<>();
        List<DoorData> doors = new ArrayList<>();

        double porchY0 = 0;
        double porchY1 = PORCH_DEPTH;
        double livingY0 = PORCH_DEPTH;
        double livingY1 = PORCH_DEPTH + frontDepth;
        double backY0 = livingY1;
        double backY1 = plotDepth;

        // Perimeter
        String southWallId = id("wall", wallIdx++);
        walls.add(new WallData(southWallId, 0, 0, plotWidth, 0, PERIMETER_THICKNESS));
        walls.add(new WallData(id("wall", wallIdx++), plotWidth, 0, plotWidth, plotDepth, PERIMETER_THICKNESS));
        walls.add(new WallData(id("wall", wallIdx++), plotWidth, plotDepth, 0, plotDepth, PERIMETER_THICKNESS));
        walls.add(new WallData(id("wall", wallIdx++), 0, plotDepth, 0, 0, PERIMETER_THICKNESS));

        // Porch / living divider
        String porchDividerId = id("wall", wallIdx++);
        walls.add(new WallData(porchDividerId, 0, livingY0, plotWidth, livingY0, INTERNAL_THICKNESS));
        rooms.add(new RoomData(id("room", roomIdx++), "Entrance Porch", rect(0, porchY0, plotWidth, porchY1), "#EDE6D8"));
        doors.add(new DoorData(id("door", doorIdx++), "DOOR", southWallId, round(plotWidth / 2), 1.2));

        // Living/dining divider (front row / back row)
        String frontBackDividerId = id("wall", wallIdx++);
        walls.add(new WallData(frontBackDividerId, 0, backY0, plotWidth, backY0, INTERNAL_THICKNESS));

        boolean splitDining = plotWidth >= frontMinWidthSum + 0.5;
        if (splitDining) {
            double livingWidth = round(plotWidth * 0.58);
            double diningWidth = round(plotWidth - livingWidth);
            rooms.add(new RoomData(id("room", roomIdx++), "Living Room", rect(0, livingY0, livingWidth, livingY1), "#F0EDE4"));
            rooms.add(new RoomData(id("room", roomIdx++), "Dining", rect(livingWidth, livingY0, plotWidth, livingY1), "#F4E9DD"));
            String livingDiningWallId = id("wall", wallIdx++);
            walls.add(new WallData(livingDiningWallId, livingWidth, livingY0, livingWidth, livingY1, INTERNAL_THICKNESS));
            doors.add(new DoorData(id("door", doorIdx++), "DOOR", porchDividerId, round(livingWidth / 2), 1.0));
            doors.add(new DoorData(id("door", doorIdx++), "DOOR", livingDiningWallId, round((livingY1 - livingY0) / 2), 0.9));
        } else {
            rooms.add(new RoomData(id("room", roomIdx++), "Living / Dining", rect(0, livingY0, plotWidth, livingY1), "#F0EDE4"));
            doors.add(new DoorData(id("door", doorIdx++), "DOOR", porchDividerId, round(plotWidth / 2), 1.0));
        }

        // Back row columns, divided by vertical walls
        double backExtraWidth = plotWidth - backMinWidthSum;
        double backTotalWeight = backColumns.stream().mapToDouble(c -> c.weight).sum();
        List<Double> columnWidths = new ArrayList<>();
        for (ColumnSpec c : backColumns) {
            columnWidths.add(round(c.minWidth + backExtraWidth * (c.weight / backTotalWeight)));
        }
        double drift = plotWidth - columnWidths.stream().mapToDouble(Double::doubleValue).sum();
        columnWidths.set(columnWidths.size() - 1, round(columnWidths.get(columnWidths.size() - 1) + drift));

        double cursorX = 0;
        for (int i = 0; i < backColumns.size(); i++) {
            ColumnSpec col = backColumns.get(i);
            double width = columnWidths.get(i);
            double x1 = cursorX;
            double x2 = cursorX + width;

            rooms.add(new RoomData(id("room", roomIdx++), col.label, rect(x1, backY0, x2, backY1), col.floorColor));

            double doorWidth = Math.max(0.7, Math.min(0.9, width - 0.6));
            double doorCenter = x1 + width / 2;
            doors.add(new DoorData(id("door", doorIdx++), "DOOR", frontBackDividerId, round(doorCenter), round(doorWidth)));

            if (i > 0) {
                walls.add(new WallData(id("wall", wallIdx++), x1, backY0, x1, backY1, INTERNAL_THICKNESS));
            }
            cursorX = x2;
        }

        return new GeneratedLayout(walls, rooms, doors);
    }

    private record ColumnSpec(String type, String label, double minWidth, double weight, String floorColor) {}

    private List<ColumnSpec> orderBackRowColumns(int bedrooms, LayoutStyle style) {
        List<ColumnSpec> bedroomCols = new ArrayList<>();
        for (int i = 1; i <= bedrooms; i++) {
            String label = bedrooms == 1 ? "Bedroom" : (i == 1 ? "Master Bedroom" : "Bedroom " + i);
            bedroomCols.add(new ColumnSpec("BEDROOM", label, BEDROOM_MIN_WIDTH, 1.3, "#E1F5EE"));
        }
        ColumnSpec kitchen = new ColumnSpec("KITCHEN", "Kitchen", KITCHEN_MIN_WIDTH, 1.0, "#FBEEE0");
        ColumnSpec bathroom = new ColumnSpec("BATHROOM", "Bathroom", BATHROOM_MIN_WIDTH, 0.8, "#DCEBF5");
        ColumnSpec store = new ColumnSpec("STORE", "Store", STORE_MIN_WIDTH, 0.6, "#EDE6D8");
        ColumnSpec ensuite = new ColumnSpec("ENSUITE", "En-suite (Master)", ENSUITE_MIN_WIDTH, 0.6, "#DCEBF5");

        List<ColumnSpec> ordered = new ArrayList<>();
        switch (style) {
            case A -> {
                ordered.addAll(bedroomCols);
                ordered.add(kitchen);
                ordered.add(bathroom);
            }
            case B -> {
                ordered.add(kitchen);
                ordered.addAll(bedroomCols);
                ordered.add(bathroom);
            }
            case C -> {
                if (bedrooms >= 2) {
                    ordered.add(bedroomCols.get(0));
                    ordered.add(bathroom);
                    ordered.addAll(bedroomCols.subList(1, bedroomCols.size()));
                    ordered.add(kitchen);
                } else {
                    // Style C degenerates to Style A for a single bedroom — the caller skips
                    // generating this combination to avoid a literal duplicate template.
                    ordered.addAll(bedroomCols);
                    ordered.add(kitchen);
                    ordered.add(bathroom);
                }
            }
        }

        // Larger homes get a pantry/store next to the kitchen, and a private
        // ensuite next to the master bedroom (in addition to the shared
        // bathroom already in `ordered`) — matching the pattern in real
        // reference plans of a master suite plus a shared family bathroom.
        if (bedrooms >= 3) {
            int kitchenIdx = ordered.indexOf(kitchen);
            ordered.add(kitchenIdx + 1, store);
        }
        if (bedrooms >= 4) {
            int masterIdx = ordered.indexOf(bedroomCols.get(0));
            ordered.add(masterIdx + 1, ensuite);
        }

        return ordered;
    }

    private String id(String prefix, int n) {
        return prefix + "_" + n;
    }

    private List<double[]> rect(double x1, double y1, double x2, double y2) {
        return List.of(
                new double[]{x1, y1},
                new double[]{x2, y1},
                new double[]{x2, y2},
                new double[]{x1, y2}
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
