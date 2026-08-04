package com.admin.dto;

import java.time.Instant;

public record CreateTenantResponse(
        String tenantId,
        String tenantCode,
        String tenantStatus,
        String subscriptionId,
        String subscriptionStatus,
        Instant trialEndsAt,
        String ownerUserId,
        String ownerEmail,
        String assignedRole,
        boolean credentialsEmailSent,
        boolean mustChangePassword
) {
}
