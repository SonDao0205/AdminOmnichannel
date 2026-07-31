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
        @NotBlank(message = "Mã gói không được để trống.")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$",
                message = "Mã gói phải dài 2-50 ký tự, bắt đầu bằng chữ và chỉ chứa chữ, số hoặc dấu gạch dưới.")
        String planCode,

        @NotBlank(message = "Tên gói không được để trống.")
        @Size(min = 2, max = 150, message = "Tên gói phải có từ 2 đến 150 ký tự.")
        String planName,

        @NotBlank(message = "Chu kỳ thanh toán không được để trống.")
        @Pattern(
                regexp = "^(MONTHLY|QUARTERLY|YEARLY|CUSTOM)$",
                message = "Chu kỳ thanh toán không hợp lệ.")
        String billingPeriod,

        @NotNull(message = "Giá không được để trống.")
        @DecimalMin(value = "0.00", message = "Giá không được là số âm.")
        @Digits(
                integer = 16,
                fraction = 2,
                message = "Giá phải có tối đa 16 chữ số nguyên và 2 chữ số thập phân.")
        BigDecimal priceAmount,

        @NotBlank(message = "Mã tiền tệ không được để trống.")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Mã tiền tệ phải gồm đúng 3 chữ cái.")
        String currency,

        @NotNull(message = "Cấu hình giới hạn không được để trống.")
        @Size(max = 100, message = "Cấu hình giới hạn không hợp lệ.")
        Map<String, Object> limits,

        @NotNull(message = "Cấu hình tính năng không được để trống.")
        @Size(max = 100, message = "Cấu hình tính năng không hợp lệ.")
        Map<String, Object> features,

        @NotBlank(message = "Trạng thái gói không được để trống.")
        @Pattern(
                regexp = "^(ACTIVE|INACTIVE|ARCHIVED)$",
                message = "Trạng thái gói không hợp lệ.")
        String status
) {
    public SubscriptionPlanRequest {
        planCode = normalizeUpper(planCode);
        planName = normalize(planName);
        billingPeriod = normalizeUpper(billingPeriod);
        currency = normalizeUpper(currency);
        status = normalizeUpper(status);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }
}
