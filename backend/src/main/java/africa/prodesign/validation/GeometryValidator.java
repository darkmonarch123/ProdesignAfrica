package africa.prodesign.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Validates the structural integrity of a canvas state JSON payload before it is
 * persisted as a DrawingSnapshot. This is a structural/schema check, not a full
 * geometry engine re-implementation (that logic lives client-side in
 * engine/geometryValidation.ts) — the goal here is to reject obviously malformed
 * or malicious payloads before they hit the database.
 *
 * Expected top-level shape (schemaVersion 1.x):
 * {
 *   "schemaVersion": "1.1",
 *   "elements": [ { "id": "...", "type": "WALL|ROOM|DOOR|WINDOW|...", ... } ],
 *   "meta": { ... }
 * }
 */
@Component
public class GeometryValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validate(String canvasStateJson, String schemaVersion) {
        if (canvasStateJson == null || canvasStateJson.isBlank()) {
            throw new IllegalArgumentException("canvasStateJson must not be empty");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(canvasStateJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("canvasStateJson is not valid JSON: " + e.getMessage());
        }

        if (!root.isObject()) {
            throw new IllegalArgumentException("canvasStateJson root must be an object");
        }
        if (!root.has("elements") || !root.get("elements").isArray()) {
            throw new IllegalArgumentException("canvasStateJson must contain an 'elements' array");
        }
        if (!root.has("schemaVersion") || !root.get("schemaVersion").isTextual()) {
            throw new IllegalArgumentException("canvasStateJson must contain a 'schemaVersion' string");
        }

        for (JsonNode element : root.get("elements")) {
            if (!element.has("id") || !element.get("id").isTextual()) {
                throw new IllegalArgumentException("Every element requires a string 'id'");
            }
            if (!element.has("type") || !element.get("type").isTextual()) {
                throw new IllegalArgumentException("Every element requires a string 'type'");
            }
            String type = element.get("type").asText();
            switch (type) {
                case "WALL" -> validateWall(element);
                case "ROOM" -> validateRoom(element);
                case "DOOR", "WINDOW" -> validateOpening(element);
                default -> {
                    // Unknown element types are tolerated for forward-compatibility across
                    // schema versions, as long as the required generic fields are present.
                }
            }
        }
    }

    private void validateWall(JsonNode wall) {
        requireNumber(wall, "x1");
        requireNumber(wall, "y1");
        requireNumber(wall, "x2");
        requireNumber(wall, "y2");
        requireNumber(wall, "thickness");
        double x1 = wall.get("x1").asDouble();
        double y1 = wall.get("y1").asDouble();
        double x2 = wall.get("x2").asDouble();
        double y2 = wall.get("y2").asDouble();
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length < 0.01) {
            throw new IllegalArgumentException("Wall element has zero or near-zero length");
        }
    }

    private void validateRoom(JsonNode room) {
        if (!room.has("points") || !room.get("points").isArray() || room.get("points").size() < 3) {
            throw new IllegalArgumentException("Room element requires at least 3 points");
        }
    }

    private void validateOpening(JsonNode opening) {
        requireNumber(opening, "x");
        requireNumber(opening, "y");
        requireNumber(opening, "width");
        if (!opening.has("hostWallId") || !opening.get("hostWallId").isTextual()) {
            throw new IllegalArgumentException("Door/window elements require a 'hostWallId'");
        }
    }

    private void requireNumber(JsonNode node, String field) {
        if (!node.has(field) || !node.get(field).isNumber()) {
            throw new IllegalArgumentException("Element missing required numeric field '" + field + "'");
        }
    }
}
