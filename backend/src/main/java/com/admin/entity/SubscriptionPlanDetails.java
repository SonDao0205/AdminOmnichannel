package com.admin.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionPlanDetails(
        String id,
        String code,
        String name,
        String billingPeriod,
        BigDecimal priceAmount,
        String currency,
        String limitsJson,
        String featuresJson,
        String status,
        Instant createdAt,
        Instant updatedAt,
        int activeTenantsCount
) {
}
