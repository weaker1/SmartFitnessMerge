package com.smartfitness;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordSecurityTests {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void bcryptMatchesCorrectPasswordAndRejectsWrongPassword() {
        String hash = passwordEncoder.encode("CorrectPassword123");

        assertTrue(passwordEncoder.matches("CorrectPassword123", hash));
        assertFalse(passwordEncoder.matches("WrongPassword123", hash));
    }
}
