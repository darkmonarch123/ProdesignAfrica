package africa.prodesign.dto;

import africa.prodesign.enums.CollabRole;

import java.time.Instant;

public record CollaboratorResponse(
        String userId,
        String email,
        String fullName,
        CollabRole role,
        Instant invitedAt
) {}
