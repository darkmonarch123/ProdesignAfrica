package africa.prodesign.dto;

import java.time.Instant;
import java.util.List;

public record BoqResponse(
        String projectId,
        String currency,
        List<BoqLineItem> lineItems,
        double totalAmount,
        Instant generatedAt,
        List<String> notes
) {}
