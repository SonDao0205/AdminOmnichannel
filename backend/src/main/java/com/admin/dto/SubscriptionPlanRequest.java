package com.admin.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubscriptionPlanRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$")
        String planCode,

        @NotBlank
        @Size(min = 2, max = 150)
        String planName,

        @NotBlank
        @Pattern(regexp = "^(MONTHLY|QUARTERLY|YEARLY|CUSTOM)$")
        String billingPeriod,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 16, fraction = 2)
        BigDecimal priceAmount,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency,

        @NotNull
        @Size(max = 100)
        Map<String, Object> limits,

        @NotNull
        @Size(max = 100)
        Map<String, Object> features,

        @NotBlank
        @Pattern(regexp = "^(ACTIVE|INACTIVE|ARCHIVED)$")
        String status
) {
}
