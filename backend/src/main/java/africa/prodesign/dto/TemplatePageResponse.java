package africa.prodesign.dto;

import java.util.List;

public record TemplatePageResponse(
        List<TemplateSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
