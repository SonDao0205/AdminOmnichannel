package com.admin.dto;

import jakarta.validation.constraints.NotNull;

public record TenantAccessRequest(
        @NotNull(message = "Trạng thái khóa tài khoản không được để trống.")
        Boolean locked
) {
}
