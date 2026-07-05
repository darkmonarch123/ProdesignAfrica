package africa.prodesign.dto;

public record BoqLineItem(
        String category,
        String description,
        double quantity,
        String unit,
        double rate,
        double amount
) {}
