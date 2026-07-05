package africa.prodesign.dto;

public record AiLayoutResponse(
        String canvasStateJson,
        String modelUsed,
        boolean usedFreeFallback,
        String note
) {}
