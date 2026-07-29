package com.admin.dto;

public record CsrfResponse(
        String headerName,
        String parameterName,
        String token
) {
}
