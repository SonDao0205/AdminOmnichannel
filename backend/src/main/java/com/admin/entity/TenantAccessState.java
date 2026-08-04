package com.admin.entity;

public record TenantAccessState(
        String tenantId,
        String tenantStatus,
        String subscriptionStatus
) {
}
