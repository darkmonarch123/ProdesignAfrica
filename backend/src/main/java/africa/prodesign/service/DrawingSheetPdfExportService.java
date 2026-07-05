package africa.prodesign.service;

import africa.prodesign.entity.DrawingSnapshot;
import africa.prodesign.entity.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Plots the project's latest drawing to a scaled, print-ready architectural
 * sheet: bordered page, overall-envelope dimension lines, a fixed north
 * arrow, an accurate scale bar, and a title block.
 */
@Service
@Slf4j
public class DrawingSheetPdfExportService {

    private static final double POINTS_PER_MM = 72.0 / 25.4;
    private static final double MARGIN_PT = 24;
    private static final double TITLE_BLOCK_HEIGHT_PT = 95;
    private static final double DIMENSION_GUTTER_PT = 46;
    private static final int[] STANDARD_SCALES = {50, 75, 100, 125, 150, 200};

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7);
    private static final Font ROOM_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
    private static final Font ROOM_AREA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 6);
    private static final Font DIM_FONT = FontFactory.getFont(FontFactory.HELVETICA, 6.5f);
    private static final Font DISCLAIMER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 5.5f, new Color(150, 30, 30));
    private static final Color INK = new Color(26, 26, 46);
    private static final Color GREEN = new Color(4, 120, 87);
    private static final Color MUTED = new Color(120, 120, 120);

    public byte[] export(Project project, DrawingSnapshot snapshot, String paperSizeCode, int requestedScale,
                          String sheetTitle, String drawnBy) {
        Geometry geometry = parseGeometry(snapshot.getCanvasStateJson());
        Rectangle pageSize = resolvePageSize(paperSizeCode);

        double drawingAreaWidthPt = pageSize.getWidth() - 2 * MARGIN_PT - DIMENSION_GUTTER_PT;
        double drawingAreaHeightPt = pageSize.getHeight() - 2 * MARGIN_PT - TITLE_BLOCK_HEIGHT_PT - DIMENSION_GUTTER_PT;

        int effectiveScale = fitScale(geometry, requestedScale, drawingAreaWidthPt, drawingAreaHeightPt);
        boolean scaleWasAdjusted = effectiveScale != requestedScale;
        double pointsPerMeter = pointsPerMeterForScale(effectiveScale);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(pageSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();

            double originX = MARGIN_PT + DIMENSION_GUTTER_PT;
            double originY = MARGIN_PT + TITLE_BLOCK_HEIGHT_PT + DIMENSION_GUTTER_PT;

            drawSheetBorder(canvas, pageSize);
            drawDrawing(canvas, geometry, originX, originY, pointsPerMeter);
            drawEnvelopeDimensions(canvas, geometry, originX, originY, pointsPerMeter);
            drawNorthArrow(canvas, pageSize);
            drawScaleBar(canvas, pageSize, effectiveScale, pointsPerMeter);
            drawTitleBlock(canvas, pageSize, project, sheetTitle, effectiveScale, scaleWasAdjusted, requestedScale, drawnBy, geometry);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate drawing sheet PDF", e);
            throw new RuntimeException("Failed to generate drawing sheet PDF", e);
        }
    }

    // ---------- Scale handling ----------

    private double pointsPerMeterForScale(int scaleDenominator) {
        double mmPerMeterOnPaper = 1000.0 / scaleDenominator;
        return mmPerMeterOnPaper * POINTS_PER_MM;
    }

    private int fitScale(Geometry geometry, int requestedScale, double areaWidthPt, double areaHeightPt) {
        if (geometry.isEmpty()) return requestedScale;
        for (int scale : STANDARD_SCALES) {
            if (scale < requestedScale) continue;
            double ppm = pointsPerMeterForScale(scale);
            if (geometry.width() * ppm <= areaWidthPt && geometry.height() * ppm <= areaHeightPt) {
                return scale;
            }
        }
        return STANDARD_SCALES[STANDARD_SCALES.length - 1];
    }

    private Rectangle resolvePageSize(String code) {
        Rectangle base = switch (code == null ? "A3" : code.toUpperCase()) {
            case "A4" -> PageSize.A4;
            case "A2" -> PageSize.A2;
            default -> PageSize.A3;
        };
        return base.rotate();
    }

    // ---------- Drawing ----------

    private void drawSheetBorder(PdfContentByte canvas, Rectangle pageSize) {
        canvas.setColorStroke(INK);
        canvas.setLineWidth(1.2f);
        canvas.rectangle(10f, 10f, pageSize.getWidth() - 20f, pageSize.getHeight() - 20f);
        canvas.stroke();
    }

    private void drawDrawing(PdfContentByte canvas, Geometry geometry, double originX, double originY, double pointsPerMeter) {
        if (geometry.isEmpty()) return;

        for (RoomShape room : geometry.rooms) {
            canvas.setColorFill(parseColorOrDefault(room.floorColor, new Color(225, 245, 238)));
            canvas.moveTo((float) toPx(room.points.get(0)[0], geometry.minX, originX, pointsPerMeter),
                    (float) toPy(room.points.get(0)[1], geometry.minY, originY, pointsPerMeter));
            for (double[] p : room.points.subList(1, room.points.size())) {
                canvas.lineTo((float) toPx(p[0], geometry.minX, originX, pointsPerMeter), (float) toPy(p[1], geometry.minY, originY, pointsPerMeter));
            }
            canvas.closePath();
            canvas.fill();

            double cx = room.points.stream().mapToDouble(p -> p[0]).average().orElse(0);
            double cy = room.points.stream().mapToDouble(p -> p[1]).average().orElse(0);
            double px = toPx(cx, geometry.minX, originX, pointsPerMeter);
            double py = toPy(cy, geometry.minY, originY, pointsPerMeter);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase(room.label, ROOM_LABEL_FONT), (float) px, (float) py + 4f, 0f);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase(String.format("%.1fm²", room.area), ROOM_AREA_FONT), (float) px, (float) py - 5f, 0f);
        }

        for (WallShape wall : geometry.walls) {
            canvas.setColorStroke(INK);
            canvas.setLineWidth((float) Math.max(1.0, wall.thickness * pointsPerMeter));
            canvas.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
            canvas.moveTo((float) toPx(wall.x1, geometry.minX, originX, pointsPerMeter), (float) toPy(wall.y1, geometry.minY, originY, pointsPerMeter));
            canvas.lineTo((float) toPx(wall.x2, geometry.minX, originX, pointsPerMeter), (float) toPy(wall.y2, geometry.minY, originY, pointsPerMeter));
            canvas.stroke();
        }

        for (OpeningShape opening : geometry.openings) {
            WallShape wall = geometry.wallsById.get(opening.hostWallId);
            if (wall == null) continue;
            double wallLen = Math.hypot(wall.x2 - wall.x1, wall.y2 - wall.y1);
            if (wallLen < 1e-6) continue;
            double dirX = (wall.x2 - wall.x1) / wallLen;
            double dirY = (wall.y2 - wall.y1) / wallLen;
            double cx = wall.x1 + dirX * opening.x;
            double cy = wall.y1 + dirY * opening.y;
            double halfW = opening.width / 2;
            double sx = cx - dirX * halfW, sy = cy - dirY * halfW;
            double ex = cx + dirX * halfW, ey = cy + dirY * halfW;

            canvas.setColorStroke("DOOR".equals(opening.type) ? new Color(194, 105, 42) : new Color(24, 95, 165));
            canvas.setLineWidth((float) Math.max(1.5, wall.thickness * pointsPerMeter + 1));
            canvas.moveTo((float) toPx(sx, geometry.minX, originX, pointsPerMeter), (float) toPy(sy, geometry.minY, originY, pointsPerMeter));
            canvas.lineTo((float) toPx(ex, geometry.minX, originX, pointsPerMeter), (float) toPy(ey, geometry.minY, originY, pointsPerMeter));
            canvas.stroke();

            if ("DOOR".equals(opening.type)) {
                double radiusPt = opening.width * pointsPerMeter;
                double hingeX = toPx(sx, geometry.minX, originX, pointsPerMeter);
                double hingeY = toPy(sy, geometry.minY, originY, pointsPerMeter);
                canvas.setLineWidth(0.6f);
                canvas.setColorStroke(new Color(194, 105, 42));
                double angleDeg = Math.toDegrees(Math.atan2(dirY, dirX));
                canvas.arc((float) (hingeX - radiusPt), (float) (hingeY - radiusPt), (float) (hingeX + radiusPt), (float) (hingeY + radiusPt),
                        (float) angleDeg, 90f);
                canvas.stroke();
            }
        }
    }

    private void drawEnvelopeDimensions(PdfContentByte canvas, Geometry geometry, double originX, double originY, double pointsPerMeter) {
        if (geometry.isEmpty()) return;
        canvas.setColorStroke(MUTED);
        canvas.setLineWidth(0.5f);

        double y = originY - 14;
        double x1 = toPx(geometry.minX, geometry.minX, originX, pointsPerMeter);
        double x2 = toPx(geometry.maxX, geometry.minX, originX, pointsPerMeter);
        drawDimensionLine(canvas, x1, y, x2, y, String.format("%.2fm", geometry.width()), true);

        double x = originX - 14;
        double y1 = toPy(geometry.minY, geometry.minY, originY, pointsPerMeter);
        double y2 = toPy(geometry.maxY, geometry.minY, originY, pointsPerMeter);
        drawDimensionLine(canvas, x, y1, x, y2, String.format("%.2fm", geometry.height()), false);
    }

    private void drawDimensionLine(PdfContentByte canvas, double x1, double y1, double x2, double y2, String label, boolean horizontal) {
        canvas.moveTo((float) x1, (float) y1);
        canvas.lineTo((float) x2, (float) y2);
        canvas.stroke();
        double tick = 3;
        canvas.moveTo((float) (x1 - (horizontal ? 0 : tick)), (float) (y1 - (horizontal ? tick : 0)));
        canvas.lineTo((float) (x1 + (horizontal ? 0 : tick)), (float) (y1 + (horizontal ? tick : 0)));
        canvas.stroke();
        canvas.moveTo((float) (x2 - (horizontal ? 0 : tick)), (float) (y2 - (horizontal ? tick : 0)));
        canvas.lineTo((float) (x2 + (horizontal ? 0 : tick)), (float) (y2 + (horizontal ? tick : 0)));
        canvas.stroke();

        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        if (horizontal) {
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase(label, DIM_FONT), (float) midX, (float) midY - 9f, 0f);
        } else {
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase(label, DIM_FONT), (float) midX, (float) midY, 90f);
        }
    }

    private void drawNorthArrow(PdfContentByte canvas, Rectangle pageSize) {
        double x = pageSize.getWidth() - 60;
        double y = pageSize.getHeight() - 50;
        canvas.setColorStroke(INK);
        canvas.setColorFill(INK);
        canvas.setLineWidth(1f);
        canvas.moveTo((float) x, (float) (y - 10));
        canvas.lineTo((float) x, (float) (y + 10));
        canvas.lineTo((float) (x - 4), (float) (y + 4));
        canvas.moveTo((float) x, (float) (y + 10));
        canvas.lineTo((float) (x + 4), (float) (y + 4));
        canvas.stroke();
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase("N", LABEL_FONT), (float) x, (float) y + 14f, 0f);
    }

    private void drawScaleBar(PdfContentByte canvas, Rectangle pageSize, int scale, double pointsPerMeter) {
        double startX = pageSize.getWidth() - 220;
        double y = pageSize.getHeight() - 55;
        canvas.setColorStroke(INK);
        canvas.setLineWidth(1f);
        int[] marks = {0, 1, 2, 3, 4, 5};
        for (int i = 0; i < marks.length - 1; i++) {
            double x1 = startX + marks[i] * pointsPerMeter;
            double x2 = startX + marks[i + 1] * pointsPerMeter;
            if (i % 2 == 0) {
                canvas.setColorFill(INK);
                canvas.rectangle((float) x1, (float) y, (float) (x2 - x1), 4f);
                canvas.fill();
            } else {
                canvas.rectangle((float) x1, (float) y, (float) (x2 - x1), 4f);
                canvas.stroke();
            }
        }
        for (int m : marks) {
            double x = startX + m * pointsPerMeter;
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new com.lowagie.text.Phrase(m + "m", DIM_FONT), (float) x, (float) y + 8f, 0f);
        }
    }

    private void drawTitleBlock(PdfContentByte canvas, Rectangle pageSize, Project project, String sheetTitle,
                                 int effectiveScale, boolean scaleWasAdjusted, int requestedScale, String drawnBy, Geometry geometry) {
        double width = pageSize.getWidth() - 20;
        double x = 10, y = 10;
        canvas.setColorStroke(INK);
        canvas.setLineWidth(1f);
        canvas.rectangle((float) x, (float) y, (float) width, (float) TITLE_BLOCK_HEIGHT_PT);
        canvas.stroke();

        double colWidth = width / 4;
        for (int i = 1; i < 4; i++) {
            canvas.moveTo((float) (x + i * colWidth), (float) y);
            canvas.lineTo((float) (x + i * colWidth), (float) (y + TITLE_BLOCK_HEIGHT_PT));
        }
        canvas.moveTo((float) x, (float) (y + TITLE_BLOCK_HEIGHT_PT * 0.55));
        canvas.lineTo((float) (x + width), (float) (y + TITLE_BLOCK_HEIGHT_PT * 0.55));
        canvas.stroke();

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String scaleLabel = "1:" + effectiveScale + (scaleWasAdjusted ? " (auto-adjusted from 1:" + requestedScale + ")" : "");

        placeLabel(canvas, x + 6, y + TITLE_BLOCK_HEIGHT_PT - 12, "PROJECT", project.getName());
        placeLabel(canvas, x + colWidth + 6, y + TITLE_BLOCK_HEIGHT_PT - 12, "DRAWING", sheetTitle);
        placeLabel(canvas, x + 2 * colWidth + 6, y + TITLE_BLOCK_HEIGHT_PT - 12, "SCALE", scaleLabel);
        placeLabel(canvas, x + 3 * colWidth + 6, y + TITLE_BLOCK_HEIGHT_PT - 12, "DATE", dateStr);

        placeLabel(canvas, x + 6, y + TITLE_BLOCK_HEIGHT_PT * 0.55 - 12, "DRAWN BY", drawnBy == null || drawnBy.isBlank() ? "Not specified" : drawnBy);
        placeLabel(canvas, x + colWidth + 6, y + TITLE_BLOCK_HEIGHT_PT * 0.55 - 12, "SHEET", "1 of 1");
        placeLabel(canvas, x + 2 * colWidth + 6, y + TITLE_BLOCK_HEIGHT_PT * 0.55 - 12, "ARCON REG. NO.", "_______________");

        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new com.lowagie.text.Phrase("This drawing has not been reviewed or stamped by a registered professional. For design development only.", DISCLAIMER_FONT),
                (float) (x + 3 * colWidth + 6), (float) (y + TITLE_BLOCK_HEIGHT_PT * 0.55 - 12), 0f);

        if (geometry.isEmpty()) {
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new com.lowagie.text.Phrase("No drawing found — this sheet shows the title block only.", DISCLAIMER_FONT),
                    (float) (x + 6), (float) (y + TITLE_BLOCK_HEIGHT_PT * 0.55 - 30), 0f);
        }
    }

    private void placeLabel(PdfContentByte canvas, double x, double y, String label, String value) {
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new com.lowagie.text.Phrase(label, LABEL_FONT), (float) x, (float) y, 0f);
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new com.lowagie.text.Phrase(value, TITLE_FONT), (float) x, (float) y - 10f, 0f);
    }

    private Color parseColorOrDefault(String hex, Color fallback) {
        try {
            if (hex == null || !hex.startsWith("#") || hex.length() != 7) return fallback;
            return new Color(Integer.valueOf(hex.substring(1, 3), 16), Integer.valueOf(hex.substring(3, 5), 16), Integer.valueOf(hex.substring(5, 7), 16));
        } catch (Exception e) {
            return fallback;
        }
    }

    private double toPx(double worldX, double minX, double originX, double pointsPerMeter) {
        return originX + (worldX - minX) * pointsPerMeter;
    }

    private double toPy(double worldY, double minY, double originY, double pointsPerMeter) {
        return originY + (worldY - minY) * pointsPerMeter;
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
                    WallShape wall = new WallShape(
                            el.path("id").asText(), el.path("x1").asDouble(), el.path("y1").asDouble(),
                            el.path("x2").asDouble(), el.path("y2").asDouble(), el.path("thickness").asDouble(0.2));
                    geometry.walls.add(wall);
                    geometry.wallsById.put(wall.id, wall);
                    geometry.expand(wall.x1, wall.y1);
                    geometry.expand(wall.x2, wall.y2);
                } else if ("ROOM".equals(type)) {
                    JsonNode pointsNode = el.get("points");
                    if (pointsNode != null && pointsNode.isArray() && pointsNode.size() >= 3) {
                        List<double[]> pts = new ArrayList<>();
                        for (JsonNode p : pointsNode) {
                            double px = p.path("x").asDouble();
                            double py = p.path("y").asDouble();
                            pts.add(new double[]{px, py});
                            geometry.expand(px, py);
                        }
                        double area = shoelaceArea(pts);
                        geometry.rooms.add(new RoomShape(el.path("label").asText("Room"), pts,
                                el.path("floorColor").asText(null), area));
                    }
                } else if ("DOOR".equals(type) || "WINDOW".equals(type)) {
                    geometry.openings.add(new OpeningShape(type, el.path("hostWallId").asText(),
                            el.path("x").asDouble(), el.path("y").asDouble(), el.path("width").asDouble(0.9)));
                }
            }
        } catch (Exception e) {
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

    private record WallShape(String id, double x1, double y1, double x2, double y2, double thickness) {}
    private record RoomShape(String label, List<double[]> points, String floorColor, double area) {}
    private record OpeningShape(String type, String hostWallId, double x, double y, double width) {}

    private static class Geometry {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        List<WallShape> walls = new ArrayList<>();
        java.util.Map<String, WallShape> wallsById = new java.util.HashMap<>();
        List<RoomShape> rooms = new ArrayList<>();
        List<OpeningShape> openings = new ArrayList<>();

        void expand(double x, double y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        boolean isEmpty() {
            return walls.isEmpty() && rooms.isEmpty();
        }

        double width() {
            return isEmpty() ? 0 : maxX - minX;
        }

        double height() {
            return isEmpty() ? 0 : maxY - minY;
        }
    }
}