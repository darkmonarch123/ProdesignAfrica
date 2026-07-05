package africa.prodesign.dto;

public record TemplateSummaryResponse(
        String id,
        String name,
        String category,
        int bedrooms,
        String style,
        double suggestedPlotWidthMeters,
        double suggestedPlotDepthMeters,
        String thumbnailSvg
) {}
