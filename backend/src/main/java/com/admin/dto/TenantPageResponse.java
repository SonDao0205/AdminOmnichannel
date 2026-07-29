package com.admin.dto;

import java.util.List;

public record TenantPageResponse(
        List<TenantListItemResponse> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
