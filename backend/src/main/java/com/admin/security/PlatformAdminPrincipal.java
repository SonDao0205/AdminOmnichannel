package com.admin.security;

public record PlatformAdminPrincipal(
        String id,
        String email,
        String displayName,
        String sessionId
) {
}
