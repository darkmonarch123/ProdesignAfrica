package africa.prodesign.dto;

public record TemplateDetailResponse(
        String id,
        String name,
        String category,
        int bedrooms,
        String style,
        double suggestedPlotWidthMeters,
        double suggestedPlotDepthMeters,
        String thumbnailSvg,
        String canvasStateJson
) {}
