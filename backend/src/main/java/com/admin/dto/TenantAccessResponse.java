package com.admin.dto;

public record TenantAccessResponse(
        String tenantId,
        String tenantStatus,
        boolean locked,
        int revokedSessions
) {
}
