package com.yoshisuproject.simpleauth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for User model */
class UserTest {

    private PasswordEncoder passwordEncoder;
    private User user;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        user = new User();
    }

    @Test
    void testUserCreation() {
        user.setEmailAddress("test@example.com");
        user.setPasswordDigest("hashed_password");

        assertEquals("test@example.com", user.getEmailAddress());
        assertEquals("hashed_password", user.getPasswordDigest());
    }

    @Test
    void testPasswordEncryption() {
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        user.setPasswordDigest(hashedPassword);

        // Verify password matches
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordDigest()));

        // Verify wrong password doesn't match
        assertFalse(passwordEncoder.matches("wrongpassword", user.getPasswordDigest()));
    }

    @Test
    void testUserWithId() {
        user.setId(1L);
        user.setEmailAddress("user@example.com");

        assertEquals(1L, user.getId());
        assertEquals("user@example.com", user.getEmailAddress());
    }

    @Test
    void testUserTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void testUserEquality() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmailAddress("test@example.com");

        User user2 = new User();
        user2.setId(1L);
        user2.setEmailAddress("test@example.com");

        // Note: User class doesn't override equals(), so this tests object identity
        assertNotEquals(user1, user2);
        assertEquals(user1.getId(), user2.getId());
        assertEquals(user1.getEmailAddress(), user2.getEmailAddress());
    }

    @Test
    void testPasswordDigestNotNull() {
        user.setEmailAddress("test@example.com");
        user.setPasswordDigest("hashed_password");

        assertNotNull(user.getPasswordDigest());
        assertFalse(user.getPasswordDigest().isEmpty());
    }
}
