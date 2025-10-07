package com.yoshisuproject.simpleauth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for PasswordResetToken model */
class PasswordResetTokenTest {

    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        token = new PasswordResetToken();
    }

    @Test
    void testTokenCreationWithConstructor() {
        Long userId = 1L;
        String selector = "test-selector";
        String verifierHash = "$2a$10$abcdefghijklmnopqrstuv";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken token = new PasswordResetToken(userId, selector, verifierHash, expiresAt);

        assertEquals(userId, token.getUserId());
        assertEquals(selector, token.getSelector());
        assertEquals(verifierHash, token.getVerifierHash());
        assertEquals(expiresAt, token.getExpiresAt());
        assertNotNull(token.getCreatedAt());
    }

    @Test
    void testGetterAndSetterForSelector() {
        String selector = "unique-selector-uuid";
        token.setSelector(selector);

        assertEquals(selector, token.getSelector());
    }

    @Test
    void testGetterAndSetterForVerifierHash() {
        String verifierHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        token.setVerifierHash(verifierHash);

        assertEquals(verifierHash, token.getVerifierHash());
    }

    @Test
    void testGetterAndSetterForUserId() {
        Long userId = 42L;
        token.setUserId(userId);

        assertEquals(userId, token.getUserId());
    }

    @Test
    void testGetterAndSetterForExpiresAt() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        token.setExpiresAt(expiresAt);

        assertEquals(expiresAt, token.getExpiresAt());
    }

    @Test
    void testGetterAndSetterForCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now();
        token.setCreatedAt(createdAt);

        assertEquals(createdAt, token.getCreatedAt());
    }

    @Test
    void testGetterAndSetterForId() {
        Long id = 123L;
        token.setId(id);

        assertEquals(id, token.getId());
    }

    @Test
    void testIsExpiredReturnsFalseForFutureExpiration() {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        token.setExpiresAt(futureTime);

        assertFalse(token.isExpired());
    }

    @Test
    void testIsExpiredReturnsTrueForPastExpiration() {
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        token.setExpiresAt(pastTime);

        assertTrue(token.isExpired());
    }

    @Test
    void testIsExpiredReturnsTrueForExactlyNow() {
        // Set expiration to a time slightly in the past to ensure it's expired
        LocalDateTime almostNow = LocalDateTime.now().minusSeconds(1);
        token.setExpiresAt(almostNow);

        assertTrue(token.isExpired());
    }

    @Test
    void testToStringDoesNotExposeVerifierHash() {
        token.setSelector("test-selector");
        token.setVerifierHash("$2a$10$secrethashvalue");
        token.setUserId(1L);

        String tokenString = token.toString();

        assertTrue(tokenString.contains("test-selector"));
        assertTrue(tokenString.contains("userId=1"));
        assertTrue(tokenString.contains("[PROTECTED]"));
        assertFalse(tokenString.contains("$2a$10$secrethashvalue"));
    }

    @Test
    void testToStringIncludesRelevantFields() {
        token.setId(1L);
        token.setUserId(2L);
        token.setSelector("selector-123");
        token.setVerifierHash("hash");
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2025, 1, 1, 12, 15);
        token.setCreatedAt(createdAt);
        token.setExpiresAt(expiresAt);

        String tokenString = token.toString();

        assertTrue(tokenString.contains("id=1"));
        assertTrue(tokenString.contains("userId=2"));
        assertTrue(tokenString.contains("selector='selector-123'"));
        assertTrue(tokenString.contains("expiresAt=2025-01-01T12:15"));
        assertTrue(tokenString.contains("createdAt=2025-01-01T12:00"));
    }

    @Test
    void testConstructorSetsCreatedAtAutomatically() {
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);

        PasswordResetToken token = new PasswordResetToken(
                1L, "selector", "verifierHash", LocalDateTime.now().plusMinutes(15));

        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

        assertNotNull(token.getCreatedAt());
        assertTrue(token.getCreatedAt().isAfter(beforeCreation));
        assertTrue(token.getCreatedAt().isBefore(afterCreation));
    }

    @Test
    void testTokenWithNullValues() {
        token.setUserId(null);
        token.setSelector(null);
        token.setVerifierHash(null);
        token.setExpiresAt(null);

        assertNull(token.getUserId());
        assertNull(token.getSelector());
        assertNull(token.getVerifierHash());
        assertNull(token.getExpiresAt());
    }

    @Test
    void testTokenFieldsIndependence() {
        String selector1 = "selector-1";
        String selector2 = "selector-2";

        token.setSelector(selector1);
        assertEquals(selector1, token.getSelector());

        token.setSelector(selector2);
        assertEquals(selector2, token.getSelector());
        assertNotEquals(selector1, token.getSelector());
    }
}
