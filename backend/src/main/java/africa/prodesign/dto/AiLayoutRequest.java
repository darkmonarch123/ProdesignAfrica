package africa.prodesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiLayoutRequest(
        @NotBlank String buildingType, // e.g. "3-bedroom bungalow", "block of flats"
        Integer bedrooms,
        @NotBlank @Size(max = 2000) String brief
) {}
