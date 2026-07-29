package com.admin.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SecureTokenServiceTests {

    private final SecureTokenService tokenService = new SecureTokenService();

    @Test
    void createsOpaqueTokensAndStableSha256Hashes() {
        String first = tokenService.newOpaqueToken();
        String second = tokenService.newOpaqueToken();

        assertEquals(43, first.length());
        assertNotEquals(first, second);
        assertEquals(64, tokenService.sha256(first).length());
        assertEquals(tokenService.sha256(first), tokenService.sha256(first));
        assertNotEquals(first, tokenService.sha256(first));
    }
}
