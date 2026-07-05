package africa.prodesign.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String email,
        String fullName,
        String role
) {}
