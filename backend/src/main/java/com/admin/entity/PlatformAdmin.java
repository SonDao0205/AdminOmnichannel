package com.admin.entity;

import java.time.Instant;

public record PlatformAdmin(
        String id,
        String email,
        String passwordHash,
        String displayName,
        String status,
        int failedLoginCount,
        Instant lockedUntil
) {
}
