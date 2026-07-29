package com.admin.entity;

import java.time.Instant;

public record TenantSummary(
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String contactEmail,
        String timezoneName,
        String defaultCurrency,
        String ownerUserId,
        String ownerEmail,
        String ownerDisplayName,
        String subscriptionPlanCode,
        String subscriptionPlanName,
        String subscriptionStatus,
        Instant trialEndsAt,
        Instant periodEndsAt,
        Instant createdAt
) {
}
