package com.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,49}$")
        String tenantCode,

        @NotBlank
        @Size(min = 2, max = 255)
        String tenantName,

        @Size(max = 255)
        String legalName,

        @Email
        @Size(max = 255)
        String contactEmail,

        @NotBlank
        @Size(max = 64)
        String timezoneName,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String defaultCurrency,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{2,50}$")
        String subscriptionPlanCode,

        @Min(1)
        @Max(365)
        int trialDays,

        @NotBlank
        @Email
        @Size(max = 255)
        String ownerEmail,

        @NotBlank
        @Size(min = 2, max = 255)
        String ownerDisplayName
) {
}
