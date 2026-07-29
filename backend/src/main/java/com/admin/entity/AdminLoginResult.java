package com.admin.entity;

import java.time.Instant;

public record AdminLoginResult(
        boolean success,
        String rawToken,
        Instant expiresAt,
        String adminId,
        String email,
        String displayName
) {
    public static AdminLoginResult failed() {
        return new AdminLoginResult(false, null, null, null, null, null);
    }

    public static AdminLoginResult succeeded(
            String rawToken,
            Instant expiresAt,
            String adminId,
            String email,
            String displayName
    ) {
        return new AdminLoginResult(true, rawToken, expiresAt, adminId, email, displayName);
    }
}
