package com.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubscriptionPlanStatusRequest(
        @NotBlank
        @Pattern(regexp = "^(ACTIVE|INACTIVE|ARCHIVED)$")
        String status
) {
}
