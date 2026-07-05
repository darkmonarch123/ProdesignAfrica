package africa.prodesign.service;

import africa.prodesign.dto.AiLayoutRequest;
import africa.prodesign.dto.AiLayoutResponse;
import africa.prodesign.entity.Project;
import africa.prodesign.template.ProceduralTemplateGenerator;
import africa.prodesign.template.ProceduralTemplateGenerator.GeneratedLayout;
import africa.prodesign.template.ProceduralTemplateGenerator.LayoutStyle;
import africa.prodesign.template.TemplateSerializer;
import africa.prodesign.validation.GeometryValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Generates a starter 2D layout (walls + rooms, matching the frontend's
 * CanvasState schema exactly) from a natural-language brief, using the
 * Anthropic Messages API.
 *
 * FREE FALLBACK: if ANTHROPIC_API_KEY isn't configured, this falls back to
 * the same procedural generator that powers the template marketplace's 1,088
 * templates instead of failing outright. It's a real, honest tradeoff, not a
 * disguised substitute — the fallback can only respond to bedroom count and
 * plot size, not a nuanced free-text brief ("open-plan with a reading nook"),
 * and the response says so explicitly via `usedFreeFallback` and `note` so
 * the frontend can be upfront about which path generated a given layout.
 *
 * This is a starting point for the user to refine in the editor, not a
 * finished design — the prompt asks for a reasonable, code-plausible layout,
 * not a compliance-checked one. The generated state still passes through
 * GeometryValidator before being returned, so at minimum it's always
 * structurally valid (parseable, non-degenerate walls, closed rooms).
 */
@Service
@Slf4j
public class AiLayoutService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryValidator geometryValidator;
    private final ProceduralTemplateGenerator fallbackGenerator = new ProceduralTemplateGenerator();
    private final TemplateSerializer templateSerializer = new TemplateSerializer();

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public AiLayoutService(
            GeometryValidator geometryValidator,
            @Value("${prodesign.anthropic.api-key}") String apiKey,
            @Value("${prodesign.anthropic.model}") String model,
            @Value("${prodesign.anthropic.base-url}") String baseUrl
    ) {
        this.geometryValidator = geometryValidator;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public AiLayoutResponse generate(Project project, AiLayoutRequest request) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-ant-placeholder")) {
            return generateWithFreeFallback(project, request);
        }

        double plotWidth = project.getPlotWidthMeters() != null ? project.getPlotWidthMeters() : 12.0;
        double plotDepth = project.getPlotDepthMeters() != null ? project.getPlotDepthMeters() : 20.0;

        String prompt = buildPrompt(request, plotWidth, plotDepth);
        String rawText = callAnthropic(prompt);
        String canvasStateJson = extractJson(rawText);

        try {
            geometryValidator.validate(canvasStateJson, "1.1");
        } catch (Exception e) {
            log.warn("AI-generated layout failed geometry validation: {}", e.getMessage());
            throw new IllegalArgumentException(
                    "The AI generated an invalid layout. Please try again or rephrase your brief.");
        }

        return new AiLayoutResponse(canvasStateJson, model, false, null);
    }

    /**
     * Uses the same procedural generator behind the template marketplace to
     * produce a real, validated layout without calling any external API —
     * zero cost, no API key required. Only reacts to bedroom count and plot
     * size (the free-text brief is ignored beyond that), which is exactly
     * the honest limitation stated in this method's response `note`.
     */
    private AiLayoutResponse generateWithFreeFallback(Project project, AiLayoutRequest request) {
        int bedrooms = request.bedrooms() != null ? Math.max(1, Math.min(6, request.bedrooms())) : 3;

        double plotWidth = project.getPlotWidthMeters() != null ? project.getPlotWidthMeters() : minWidthFor(bedrooms);
        double plotDepth = project.getPlotDepthMeters() != null ? project.getPlotDepthMeters() : 12.0;

        // A little variety without needing real AI: pick a layout ordering style
        // deterministically from the brief text so the same brief always yields
        // the same layout, but different briefs are likely to differ.
        LayoutStyle[] styles = bedrooms >= 2
                ? new LayoutStyle[]{LayoutStyle.A, LayoutStyle.B, LayoutStyle.C}
                : new LayoutStyle[]{LayoutStyle.A, LayoutStyle.B};
        int styleIndex = Math.abs((request.brief() == null ? "" : request.brief()).hashCode()) % styles.length;
        LayoutStyle style = styles[styleIndex];

        GeneratedLayout layout = fallbackGenerator.generate(bedrooms, plotWidth, plotDepth, style);
        if (layout == null) {
            // Requested plot is too small for the bedroom count — widen it to the
            // generator's own minimum instead of failing outright.
            plotWidth = Math.max(plotWidth, minWidthFor(bedrooms));
            plotDepth = Math.max(plotDepth, 12.0);
            layout = fallbackGenerator.generate(bedrooms, plotWidth, plotDepth, style);
        }
        if (layout == null) {
            throw new IllegalStateException(
                    "Could not generate a layout for this plot size and bedroom count. Try a larger plot.");
        }

        String canvasStateJson = templateSerializer.toCanvasStateJson(layout);
        geometryValidator.validate(canvasStateJson, "1.1"); // should always pass — same generator as the template catalog

        String note = "No AI API key is configured on this server, so this layout was generated by the same "
                + "built-in generator that powers the template marketplace — it used your bedroom count and plot "
                + "size, but not the wording of your brief. Set ANTHROPIC_API_KEY to enable full AI generation "
                + "from a free-text brief.";
        return new AiLayoutResponse(canvasStateJson, "builtin-generator", true, note);
    }

    private double minWidthFor(int bedrooms) {
        double raw = bedrooms * 2.6 + 2.2 + 1.8 + 1.0;
        if (bedrooms >= 3) raw += 1.6;
        if (bedrooms >= 4) raw += 1.8;
        return Math.ceil(raw / 2.0) * 2.0;
    }

    private String buildPrompt(AiLayoutRequest request, double plotWidth, double plotDepth) {
        return """
                You are a floor-plan layout generator for an architectural CAD tool. Generate a single-storey \
                starter floor plan as JSON only — no prose, no markdown code fences, no explanation.

                Plot dimensions: %.1fm wide (x-axis) by %.1fm deep (y-axis). The road-facing (front) edge is at \
                y = 0. Keep the entire building footprint within x in [0, %.1f] and y in [0, %.1f], leaving at \
                least 2 meters of setback from every plot edge.

                Building type: %s
                Bedrooms: %s
                Brief from the client: %s

                Respond with ONLY a JSON object matching this exact schema:
                {
                  "schemaVersion": "1.1",
                  "elements": [
                    {"id": "wall_1", "type": "WALL", "x1": 0, "y1": 0, "x2": 5, "y2": 0, "thickness": 0.2, "height": 3},
                    {"id": "room_1", "type": "ROOM", "points": [{"x":0,"y":0},{"x":5,"y":0},{"x":5,"y":4},{"x":0,"y":4}], "label": "Living Room", "floorColor": "#E1F5EE"}
                  ],
                  "meta": {"unit": "m", "gridSize": 0.5}
                }

                Rules:
                - Every wall needs a unique id, positive length, thickness around 0.15-0.25, height 2.8-3.2.
                - Every room needs at least 3 points forming a closed, non-self-intersecting polygon, and should \
                  correspond to an enclosed area bounded by walls you also included.
                - Give each room a sensible label (Living Room, Master Bedroom, Kitchen, Bathroom, Dining, etc).
                - Produce a coherent, walkable layout appropriate for the brief — rooms should not overlap.
                - Do not include doors or windows; only WALL and ROOM elements.
                - Output raw JSON only, starting with { and ending with }.
                """
                .formatted(plotWidth, plotDepth, plotWidth, plotDepth, request.buildingType(),
                        request.bedrooms() != null ? request.bedrooms().toString() : "unspecified",
                        request.brief());
    }

    private String callAnthropic(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 4000,
                    "temperature", 0.3,
                    "messages", new Object[]{Map.of("role", "user", "content", prompt)}
            );
            String requestJson = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Anthropic API returned {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("The AI layout service is temporarily unavailable. Please try again.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new IllegalStateException("The AI service returned an empty response.");
            }
            return content.get(0).path("text").asText("");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Anthropic API", e);
            throw new IllegalStateException("Could not reach the AI layout service. Please try again.");
        }
    }

    /** Strips accidental markdown code fences in case the model wraps its JSON output despite instructions. */
    private String extractJson(String rawText) {
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
