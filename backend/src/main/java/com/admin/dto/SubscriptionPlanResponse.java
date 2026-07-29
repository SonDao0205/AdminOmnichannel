package com.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record SubscriptionPlanResponse(
        String planId,
        String planCode,
        String planName,
        String billingPeriod,
        BigDecimal priceAmount,
        String currency,
        Map<String, Object> limits,
        Map<String, Object> features,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
