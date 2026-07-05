package africa.prodesign.template;

import africa.prodesign.entity.Template;
import africa.prodesign.repository.TemplateRepository;
import africa.prodesign.template.ProceduralTemplateGenerator.GeneratedLayout;
import africa.prodesign.template.ProceduralTemplateGenerator.LayoutStyle;
import africa.prodesign.validation.GeometryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the template marketplace catalog procedurally rather than hand-authoring
 * each one: every (bedroom count) x (plot width) x (plot depth) x (layout style)
 * combination that's geometrically feasible becomes a real, distinct, validated
 * template. This is what "1000+ templates" honestly means here — a large but
 * legitimate combinatorial space of valid floor plans, not 1000 hand-designed
 * unique architectural concepts. Every single one is run through
 * GeometryValidator before being stored, so the catalog can't contain a broken
 * template even at this volume.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateCatalogSeeder {

    private static final int[] BEDROOM_COUNTS = {1, 2, 3, 4, 5, 6};
    private static final double[] WIDTH_STEPS = {0, 2, 4, 6, 8, 10, 12, 14};
    private static final double[] DEPTH_STEPS = {0, 2, 4, 6, 8, 10, 12, 14};

    private final TemplateRepository templateRepository;
    private final GeometryValidator geometryValidator;
    private final ProceduralTemplateGenerator generator = new ProceduralTemplateGenerator();
    private final TemplateSerializer serializer = new TemplateSerializer();

    public void seed() {
        if (templateRepository.count() > 0) {
            log.info("Template catalog already seeded, skipping.");
            return;
        }

        List<Template> batch = new ArrayList<>();
        int generated = 0;
        int skipped = 0;
        int invalid = 0;

        for (int bedrooms : BEDROOM_COUNTS) {
            double baseWidth = minWidthFor(bedrooms);
            double baseDepth = minDepthFor();
            List<LayoutStyle> styles = bedrooms >= 2
                    ? List.of(LayoutStyle.A, LayoutStyle.B, LayoutStyle.C)
                    : List.of(LayoutStyle.A, LayoutStyle.B); // Style C degenerates to A for 1 bedroom

            for (double widthStep : WIDTH_STEPS) {
                double plotWidth = round(baseWidth + widthStep);
                for (double depthStep : DEPTH_STEPS) {
                    double plotDepth = round(baseDepth + depthStep);
                    for (LayoutStyle style : styles) {
                        GeneratedLayout layout = generator.generate(bedrooms, plotWidth, plotDepth, style);
                        if (layout == null) {
                            skipped++;
                            continue;
                        }

                        String canvasStateJson = serializer.toCanvasStateJson(layout);
                        try {
                            geometryValidator.validate(canvasStateJson, "1.1");
                        } catch (Exception e) {
                            invalid++;
                            log.warn("Generated template failed validation ({} bed, {}x{}, style {}): {}",
                                    bedrooms, plotWidth, plotDepth, style, e.getMessage());
                            continue;
                        }

                        String thumbnailSvg = serializer.toThumbnailSvg(layout, plotWidth, plotDepth);
                        String category = bedrooms == 1 ? "Studio / 1 Bedroom" : bedrooms + " Bedroom";
                        String name = String.format("%s Bungalow — %.0fm x %.0fm (Style %s)",
                                category, plotWidth, plotDepth, style.name());

                        batch.add(Template.builder()
                                .name(name)
                                .category(category)
                                .bedrooms(bedrooms)
                                .style(style.name())
                                .suggestedPlotWidthMeters(plotWidth)
                                .suggestedPlotDepthMeters(plotDepth)
                                .canvasStateJson(canvasStateJson)
                                .thumbnailSvg(thumbnailSvg)
                                .build());
                        generated++;
                    }
                }
            }
        }

        templateRepository.saveAll(batch);
        log.info("Seeded {} generated templates ({} skipped as infeasible plot sizes, {} rejected by validation)",
                generated, skipped, invalid);
    }

    private double minWidthFor(int bedrooms) {
        // bedrooms*2.6 + kitchen(2.2) + bathroom(1.8), plus store(1.6) once bedrooms>=3
        // and ensuite(1.8) once bedrooms>=4 (matches ProceduralTemplateGenerator's
        // orderBackRowColumns), plus a small margin, rounded up to an even number.
        double raw = bedrooms * 2.6 + 2.2 + 1.8 + 1.0;
        if (bedrooms >= 3) raw += 1.6;
        if (bedrooms >= 4) raw += 1.8;
        return Math.ceil(raw / 2.0) * 2.0;
    }

    private double minDepthFor() {
        // porch(1.6) + front row min(3.0) + back row min(2.5) + margin
        return 8.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
