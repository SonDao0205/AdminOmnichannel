package com.admin.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class TenantCodeGenerator {

    public String generate() {
        return "TEN_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(java.util.Locale.ROOT);
    }
}
