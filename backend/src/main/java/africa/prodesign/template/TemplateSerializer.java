package africa.prodesign.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import africa.prodesign.template.ProceduralTemplateGenerator.DoorData;
import africa.prodesign.template.ProceduralTemplateGenerator.GeneratedLayout;
import africa.prodesign.template.ProceduralTemplateGenerator.RoomData;
import africa.prodesign.template.ProceduralTemplateGenerator.WallData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String toCanvasStateJson(GeneratedLayout layout) {
        List<Map<String, Object>> elements = new java.util.ArrayList<>();

        for (WallData wall : layout.walls()) {
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("id", wall.id());
            el.put("type", "WALL");
            el.put("x1", wall.x1());
            el.put("y1", wall.y1());
            el.put("x2", wall.x2());
            el.put("y2", wall.y2());
            el.put("thickness", wall.thickness());
            el.put("height", 3.0);
            elements.add(el);
        }

        for (RoomData room : layout.rooms()) {
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("id", room.id());
            el.put("type", "ROOM");
            List<Map<String, Object>> points = new java.util.ArrayList<>();
            for (double[] p : room.points()) {
                Map<String, Object> pt = new LinkedHashMap<>();
                pt.put("x", p[0]);
                pt.put("y", p[1]);
                points.add(pt);
            }
            el.put("points", points);
            el.put("label", room.label());
            el.put("floorColor", room.floorColor());
            elements.add(el);
        }

        for (DoorData door : layout.doors()) {
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("id", door.id());
            el.put("type", door.type());
            el.put("hostWallId", door.hostWallId());
            el.put("x", door.x());
            el.put("y", 0.0);
            el.put("width", door.width());
            el.put("swing", "RIGHT");
            elements.add(el);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("unit", "m");
        meta.put("gridSize", 0.5);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", "1.1");
        root.put("elements", elements);
        root.put("meta", meta);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize generated template layout", e);
        }
    }

    /** A simple schematic SVG for the template gallery thumbnail — colored room rectangles, no text (illegible at thumbnail scale anyway). */
    public String toThumbnailSvg(GeneratedLayout layout, double plotWidth, double plotDepth) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "<svg viewBox=\"0 0 %.1f %.1f\" xmlns=\"http://www.w3.org/2000/svg\">",
                plotWidth, plotDepth));
        sb.append(String.format(
                "<rect x=\"0\" y=\"0\" width=\"%.1f\" height=\"%.1f\" fill=\"#F7F5F0\"/>",
                plotWidth, plotDepth));
        for (RoomData room : layout.rooms()) {
            double minX = room.points().stream().mapToDouble(p -> p[0]).min().orElse(0);
            double minY = room.points().stream().mapToDouble(p -> p[1]).min().orElse(0);
            double maxX = room.points().stream().mapToDouble(p -> p[0]).max().orElse(0);
            double maxY = room.points().stream().mapToDouble(p -> p[1]).max().orElse(0);
            sb.append(String.format(
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\" stroke=\"#1A1A2E\" stroke-width=\"0.08\"/>",
                    minX, minY, maxX - minX, maxY - minY, room.floorColor()));
        }
        sb.append("</svg>");
        return sb.toString();
    }
}
