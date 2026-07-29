package com.admin.dto;

import java.time.Instant;

public record AdminLoginResponse(
        String id,
        String email,
        String displayName,
        String actorType,
        Instant expiresAt
) {
}
