package com.admin.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTests {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void generatesStrongNonRepeatingPasswords() {
        String first = generator.generate();
        String second = generator.generate();

        assertEquals(20, first.length());
        policy.validate(first);
        policy.validate(second);
        assertNotEquals(first, second);
    }
}
