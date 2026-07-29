package com.admin.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PasswordPolicyTests {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> policy.validate("Strong-owner-2026!"));
    }

    @Test
    void rejectsWeakPasswords() {
        assertThrows(IllegalArgumentException.class, () -> policy.validate("short"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("all-lowercase-123!"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("NO-LOWERCASE-123!"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("MissingNumber!"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("MissingSpecial123"));
    }
}
