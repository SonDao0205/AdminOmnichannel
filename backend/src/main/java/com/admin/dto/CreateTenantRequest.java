package com.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank(message = "Tên tenant không được để trống.")
        @Size(min = 2, max = 255, message = "Tên tenant phải có từ 2 đến 255 ký tự.")
        String tenantName,

        @Size(max = 255, message = "Tên pháp lý không được vượt quá 255 ký tự.")
        String legalName,

        @Email(message = "Email liên hệ không đúng định dạng.")
        @Size(max = 255, message = "Email liên hệ không được vượt quá 255 ký tự.")
        String contactEmail,

        @NotBlank(message = "Múi giờ không được để trống.")
        @Size(max = 64, message = "Múi giờ không hợp lệ.")
        String timezoneName,

        @NotBlank(message = "Mã tiền tệ không được để trống.")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Mã tiền tệ phải gồm đúng 3 chữ cái.")
        String defaultCurrency,

        @NotBlank(message = "Mã gói không được để trống.")
        @Pattern(
                regexp = "^[A-Za-z0-9_]{2,50}$",
                message = "Mã gói phải có từ 2 đến 50 ký tự và chỉ chứa chữ, số hoặc dấu gạch dưới.")
        String subscriptionPlanCode,

        @Min(value = 1, message = "Số ngày dùng thử phải là số nguyên từ 1 đến 365.")
        @Max(value = 365, message = "Số ngày dùng thử phải là số nguyên từ 1 đến 365.")
        int trialDays,

        @NotBlank(message = "Email đăng nhập không được để trống.")
        @Email(message = "Email đăng nhập không đúng định dạng.")
        @Size(max = 255, message = "Email đăng nhập không được vượt quá 255 ký tự.")
        String ownerEmail,

        @NotBlank(message = "Họ tên quản lý không được để trống.")
        @Size(min = 2, max = 255, message = "Họ tên quản lý phải có từ 2 đến 255 ký tự.")
        String ownerDisplayName
) {
    public CreateTenantRequest {
        tenantName = normalize(tenantName);
        legalName = normalize(legalName);
        contactEmail = normalizeLower(contactEmail);
        timezoneName = normalize(timezoneName);
        defaultCurrency = normalizeUpper(defaultCurrency);
        subscriptionPlanCode = normalizeUpper(subscriptionPlanCode);
        ownerEmail = normalizeLower(ownerEmail);
        ownerDisplayName = normalize(ownerDisplayName);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizeLower(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
