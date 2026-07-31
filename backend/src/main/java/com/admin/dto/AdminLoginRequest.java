package com.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "Email quản trị không được để trống.")
        @Email(message = "Email quản trị không đúng định dạng.")
        @Size(max = 255, message = "Email quản trị không được vượt quá 255 ký tự.")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(max = 128, message = "Mật khẩu không được vượt quá 128 ký tự.")
        String password
) {
    public AdminLoginRequest {
        email = email == null
                ? null
                : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
