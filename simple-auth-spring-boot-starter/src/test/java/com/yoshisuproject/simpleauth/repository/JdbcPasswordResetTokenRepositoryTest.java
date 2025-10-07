package com.yoshisuproject.simpleauth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.yoshisuproject.simpleauth.model.PasswordResetToken;
import com.yoshisuproject.simpleauth.model.User;

/**
 * Integration tests for JdbcPasswordResetTokenRepository Tests database
 * operations for password reset tokens with
 * selector/verifier pattern
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcPasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmailAddress("test@example.com");
        testUser.setPasswordDigest(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSaveAndFindBySelector() {
        String selector = "test-selector-uuid";
        String verifierHash = passwordEncoder.encode("test-verifier");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken token = new PasswordResetToken(testUser.getId(), selector, verifierHash, expiresAt);

        PasswordResetToken saved = tokenRepository.save(token);

        assertNotNull(saved.getId());
        assertEquals(testUser.getId(), saved.getUserId());
        assertEquals(selector, saved.getSelector());
        assertEquals(verifierHash, saved.getVerifierHash());
        assertEquals(expiresAt, saved.getExpiresAt());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testFindBySelectorReturnsEmptyForNonExistent() {
        Optional<PasswordResetToken> result = tokenRepository.findBySelector("non-existent-selector");

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindBySelectorReturnsExistingToken() {
        String selector = "unique-selector";
        PasswordResetToken token = createAndSaveToken(selector);

        Optional<PasswordResetToken> result = tokenRepository.findBySelector(selector);

        assertTrue(result.isPresent());
        assertEquals(token.getId(), result.get().getId());
        assertEquals(selector, result.get().getSelector());
    }

    @Test
    void testSaveCorrectlyStoresSelectorAndVerifierHash() {
        String selector = "selector-123";
        String verifierHash = "$2a$10$abcdefghijklmnopqrstuv";

        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(token);

        // Retrieve and verify
        Optional<PasswordResetToken> retrieved = tokenRepository.findBySelector(selector);
        assertTrue(retrieved.isPresent());
        assertEquals(selector, retrieved.get().getSelector());
        assertEquals(verifierHash, retrieved.get().getVerifierHash());
    }

    @Test
    void testDeleteBySelector() {
        String selector = "selector-to-delete";
        createAndSaveToken(selector);

        // Verify token exists
        assertTrue(tokenRepository.findBySelector(selector).isPresent());

        // Delete by selector
        tokenRepository.deleteBySelector(selector);

        // Verify token was deleted
        assertTrue(tokenRepository.findBySelector(selector).isEmpty());
    }

    @Test
    void testDeleteBySelectorDoesNotAffectOtherTokens() {
        String selector1 = "selector-1";
        String selector2 = "selector-2";

        createAndSaveToken(selector1);
        createAndSaveToken(selector2);

        tokenRepository.deleteBySelector(selector1);

        assertTrue(tokenRepository.findBySelector(selector1).isEmpty());
        assertTrue(tokenRepository.findBySelector(selector2).isPresent());
    }

    @Test
    void testFindByUserId() {
        String selector = "user-token-selector";
        PasswordResetToken token = createAndSaveToken(selector);

        Optional<PasswordResetToken> result = tokenRepository.findByUserId(testUser.getId());

        assertTrue(result.isPresent());
        assertEquals(token.getId(), result.get().getId());
        assertEquals(testUser.getId(), result.get().getUserId());
    }

    @Test
    void testFindByUserIdReturnsEmptyForNonExistent() {
        Optional<PasswordResetToken> result = tokenRepository.findByUserId(99999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteByUserId() {
        createAndSaveToken("selector-1");
        createAndSaveToken("selector-2");

        // Verify tokens exist
        assertEquals(2, tokenRepository.count());

        // Delete all tokens for user
        tokenRepository.deleteByUserId(testUser.getId());

        // Verify tokens were deleted
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testDeleteByUserIdOnlyDeletesUserTokens() {
        // Create another user
        User anotherUser = new User();
        anotherUser.setEmailAddress("another@example.com");
        anotherUser.setPasswordDigest(passwordEncoder.encode("password"));
        anotherUser = userRepository.save(anotherUser);

        // Create tokens for both users
        createAndSaveToken("user1-selector");

        PasswordResetToken anotherUserToken = new PasswordResetToken(
                anotherUser.getId(),
                "user2-selector",
                passwordEncoder.encode("verifier"),
                LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(anotherUserToken);

        // Delete only testUser's tokens
        tokenRepository.deleteByUserId(testUser.getId());

        // Verify only testUser's tokens were deleted
        assertEquals(1, tokenRepository.count());
        assertTrue(tokenRepository.findByUserId(anotherUser.getId()).isPresent());
    }

    @Test
    void testDeleteExpiredTokens() {
        // Create expired token
        PasswordResetToken expiredToken = new PasswordResetToken(
                testUser.getId(),
                "expired-selector",
                passwordEncoder.encode("verifier"),
                LocalDateTime.now().minusMinutes(15));
        tokenRepository.save(expiredToken);

        // Create valid token
        PasswordResetToken validToken = new PasswordResetToken(
                testUser.getId(),
                "valid-selector",
                passwordEncoder.encode("verifier"),
                LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(validToken);

        // Delete expired tokens
        int deleted = tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Verify only expired token was deleted
        assertEquals(1, deleted);
        assertEquals(1, tokenRepository.count());
        assertTrue(tokenRepository.findBySelector("valid-selector").isPresent());
        assertTrue(tokenRepository.findBySelector("expired-selector").isEmpty());
    }

    @Test
    void testDeleteExpiredTokensWithNoExpiredTokens() {
        createAndSaveToken("valid-selector");

        int deleted = tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        assertEquals(0, deleted);
        assertEquals(1, tokenRepository.count());
    }

    @Test
    void testDeleteToken() {
        PasswordResetToken token = createAndSaveToken("selector-to-delete");

        tokenRepository.delete(token);

        assertTrue(tokenRepository.findBySelector("selector-to-delete").isEmpty());
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testDeleteAll() {
        createAndSaveToken("selector-1");
        createAndSaveToken("selector-2");
        createAndSaveToken("selector-3");

        assertEquals(3, tokenRepository.count());

        tokenRepository.deleteAll();

        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testCount() {
        assertEquals(0, tokenRepository.count());

        createAndSaveToken("selector-1");
        assertEquals(1, tokenRepository.count());

        createAndSaveToken("selector-2");
        assertEquals(2, tokenRepository.count());

        tokenRepository.deleteAll();
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testUpdateExistingToken() {
        PasswordResetToken token = createAndSaveToken("original-selector");

        // Update the token
        token.setSelector("updated-selector");
        token.setVerifierHash(passwordEncoder.encode("new-verifier"));
        PasswordResetToken updated = tokenRepository.save(token);

        // Verify update
        assertEquals(token.getId(), updated.getId());
        assertEquals("updated-selector", updated.getSelector());

        // Verify old selector no longer works
        assertTrue(tokenRepository.findBySelector("original-selector").isEmpty());

        // Verify new selector works
        assertTrue(tokenRepository.findBySelector("updated-selector").isPresent());
    }

    @Test
    void testTokenTimestampsArePreserved() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), "selector-with-timestamps", passwordEncoder.encode("verifier"), expiresAt);

        PasswordResetToken saved = tokenRepository.save(token);
        assertNotNull(saved.getCreatedAt());

        // Retrieve and verify timestamps are preserved
        Optional<PasswordResetToken> retrieved = tokenRepository.findBySelector("selector-with-timestamps");
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getCreatedAt());
        assertNotNull(retrieved.get().getExpiresAt());

        // Verify expiration time is approximately correct (within 1 second tolerance)
        LocalDateTime retrievedExpiration = retrieved.get().getExpiresAt();
        assertTrue(Math.abs(retrievedExpiration.toLocalTime().toSecondOfDay()
                        - expiresAt.toLocalTime().toSecondOfDay())
                < 2);
    }

    @Test
    void testSelectorUniquenessConstraint() {
        String selector = "duplicate-selector";

        createAndSaveToken(selector);

        // Attempt to create another token with same selector should fail or replace
        PasswordResetToken duplicate = new PasswordResetToken(
                testUser.getId(),
                selector,
                passwordEncoder.encode("different-verifier"),
                LocalDateTime.now().plusMinutes(15));

        assertThrows(Exception.class, () -> {
            tokenRepository.save(duplicate);
        });
    }

    /** Helper method to create and save a token with given selector */
    private PasswordResetToken createAndSaveToken(String selector) {
        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(),
                selector,
                passwordEncoder.encode("verifier"),
                LocalDateTime.now().plusMinutes(15));
        return tokenRepository.save(token);
    }
}
