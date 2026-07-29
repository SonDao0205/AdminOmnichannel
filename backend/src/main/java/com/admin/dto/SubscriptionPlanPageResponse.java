package com.admin.dto;

import java.util.List;

public record SubscriptionPlanPageResponse(
        List<SubscriptionPlanResponse> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
