package africa.prodesign.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public ErrorResponse(String message) {
        this(message, Instant.now(), null);
    }

    public ErrorResponse(String message, Map<String, String> fieldErrors) {
        this(message, Instant.now(), fieldErrors);
    }
}
