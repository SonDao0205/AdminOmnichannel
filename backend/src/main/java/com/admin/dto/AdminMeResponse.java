package com.admin.dto;

public record AdminMeResponse(
        String id,
        String email,
        String displayName,
        String actorType
) {
}
