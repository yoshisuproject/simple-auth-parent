package com.yoshisuproject.simpleauth.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.yoshisuproject.simpleauth.model.PasswordResetToken;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.PasswordResetTokenRepository;
import com.yoshisuproject.simpleauth.repository.SessionRepository;
import com.yoshisuproject.simpleauth.repository.UserRepository;

/**
 * Integration tests for complete password reset workflow Tests the end-to-end
 * flow with database and security
 * verification
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        sessionRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmailAddress("integration@example.com");
        testUser.setPasswordDigest(passwordEncoder.encode("oldpassword"));
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testPasswordResetFormAccessible() throws Exception {
        mockMvc.perform(get("/passwords/new")).andExpect(status().isOk()).andExpect(view().name("passwords/new"));
    }

    @Test
    void testCompletePasswordResetWorkflow() throws Exception {
        // Step 1: Request password reset
        mockMvc.perform(post("/passwords").param("emailAddress", "integration@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andExpect(
                        flash().attribute(
                                        "notice",
                                        "If that email address is in our system, you will receive a password reset email shortly."));

        // Step 2: Verify token was created in database
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserId(testUser.getId());
        assertTrue(tokenOpt.isPresent());

        PasswordResetToken savedToken = tokenOpt.get();
        assertNotNull(savedToken.getSelector());
        assertNotNull(savedToken.getVerifierHash());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));

        // Step 3: Construct the full token (selector:verifier)
        // In real scenario, verifier would be from email, but we need to reconstruct it
        // For testing, we'll create a new verifier and update the database
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        savedToken.setVerifierHash(verifierHash);
        tokenRepository.save(savedToken);

        String fullToken = savedToken.getSelector() + ":" + verifier;

        // Step 4: Access password reset form
        mockMvc.perform(get("/passwords/" + fullToken + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("passwords/edit"))
                .andExpect(model().attribute("token", fullToken));

        // Step 5: Submit new password
        mockMvc.perform(patch("/passwords/" + fullToken)
                        .param("password", "newpassword123")
                        .param("passwordConfirmation", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andExpect(flash().attribute("notice", "Your password has been reset successfully"));

        // Step 6: Verify password was updated
        User updatedUser =
                userRepository.findByEmailAddress("integration@example.com").get();
        assertTrue(passwordEncoder.matches("newpassword123", updatedUser.getPasswordDigest()));
        assertFalse(passwordEncoder.matches("oldpassword", updatedUser.getPasswordDigest()));

        // Step 7: Verify token was deleted after use
        assertTrue(tokenRepository.findBySelector(savedToken.getSelector()).isEmpty());
    }

    @Test
    void testTokenStoredCorrectlyInDatabase() throws Exception {
        mockMvc.perform(post("/passwords").param("emailAddress", "integration@example.com"));

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserId(testUser.getId());
        assertTrue(tokenOpt.isPresent());

        PasswordResetToken token = tokenOpt.get();

        // Verify selector is plaintext UUID
        assertDoesNotThrow(() -> UUID.fromString(token.getSelector()));

        // Verify verifierHash is BCrypt format
        assertTrue(token.getVerifierHash().startsWith("$2a$")
                || token.getVerifierHash().startsWith("$2b$"));
        assertEquals(60, token.getVerifierHash().length());
    }

    @Test
    void testInvalidVerifierRejected() throws Exception {
        // Create a token
        String selector = UUID.randomUUID().toString();
        String correctVerifier = UUID.randomUUID().toString();
        String wrongVerifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(correctVerifier);

        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        // Try to use wrong verifier
        String wrongToken = selector + ":" + wrongVerifier;

        mockMvc.perform(get("/passwords/" + wrongToken + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/new"))
                .andExpect(flash().attribute("error", "Invalid password reset link"));
    }

    @Test
    void testExpiredTokenRejected() throws Exception {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);

        // Create expired token
        PasswordResetToken expiredToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().minusMinutes(15));
        tokenRepository.save(expiredToken);

        String fullToken = selector + ":" + verifier;

        mockMvc.perform(get("/passwords/" + fullToken + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/new"))
                .andExpect(flash().attribute("error", "Invalid or expired password reset link"));
    }

    @Test
    void testOldTokensDeletedWhenNewTokenRequested() throws Exception {
        // Create old token
        PasswordResetToken oldToken = new PasswordResetToken(
                testUser.getId(),
                "old-selector",
                passwordEncoder.encode("old-verifier"),
                LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(oldToken);

        assertEquals(1, tokenRepository.count());

        // Request new token
        mockMvc.perform(post("/passwords").param("emailAddress", "integration@example.com"));

        // Verify only one token exists (old one deleted, new one created)
        assertEquals(1, tokenRepository.count());

        // Verify old selector no longer exists
        assertTrue(tokenRepository.findBySelector("old-selector").isEmpty());
    }

    @Test
    void testMultipleUsersTokensDoNotInterfere() throws Exception {
        // Create second user
        User user2 = new User();
        user2.setEmailAddress("user2@example.com");
        user2.setPasswordDigest(passwordEncoder.encode("password"));
        user2 = userRepository.save(user2);

        // Request tokens for both users
        mockMvc.perform(post("/passwords").param("emailAddress", "integration@example.com"));

        mockMvc.perform(post("/passwords").param("emailAddress", "user2@example.com"));

        // Verify both tokens exist
        assertTrue(tokenRepository.findByUserId(testUser.getId()).isPresent());
        assertTrue(tokenRepository.findByUserId(user2.getId()).isPresent());

        // Verify they have different selectors
        PasswordResetToken token1 =
                tokenRepository.findByUserId(testUser.getId()).get();
        PasswordResetToken token2 = tokenRepository.findByUserId(user2.getId()).get();
        assertNotEquals(token1.getSelector(), token2.getSelector());
    }

    @Test
    void testTokenCannotBeReused() throws Exception {
        // Create and save a token with known verifier
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);

        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        String fullToken = selector + ":" + verifier;

        // Use token once successfully
        mockMvc.perform(patch("/passwords/" + fullToken)
                        .param("password", "newpassword123")
                        .param("passwordConfirmation", "newpassword123"))
                .andExpect(status().is3xxRedirection());

        // Try to use same token again
        mockMvc.perform(get("/passwords/" + fullToken + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/new"))
                .andExpect(flash().attribute("error", "Invalid or expired password reset link"));
    }

    @Test
    void testDatabaseCompromiseCannotForgeTokens() {
        // Simulate database compromise: attacker has selector and verifierHash
        String selector = UUID.randomUUID().toString();
        String verifierHash = "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        PasswordResetToken compromisedToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(compromisedToken);

        // Attacker tries to use the selector with a guessed/forged verifier
        // Even if attacker knows the selector and hash, they cannot forge a valid token
        String forgedVerifier = UUID.randomUUID().toString();
        String forgedToken = selector + ":" + forgedVerifier;

        // This should fail because forgedVerifier won't match the verifierHash
        assertThrows(AssertionError.class, () -> {
            mockMvc.perform(get("/passwords/" + forgedToken + "/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("passwords/edit"));
        });
    }

    @Test
    void testInvalidTokenFormatRejected() throws Exception {
        // No colon separator
        mockMvc.perform(get("/passwords/invalidtoken/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/new"))
                .andExpect(flash().attribute("error", "Invalid password reset link"));

        // Empty token
        mockMvc.perform(get("/passwords/:/edit")).andExpect(status().is3xxRedirection());

        // Only selector, no verifier
        mockMvc.perform(get("/passwords/selector-only:/edit")).andExpect(status().is3xxRedirection());
    }

    @Test
    void testPasswordValidationRules() throws Exception {
        // Create valid token
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);

        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        String fullToken = selector + ":" + verifier;

        // Test password too short
        mockMvc.perform(patch("/passwords/" + fullToken)
                        .param("password", "short")
                        .param("passwordConfirmation", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/" + fullToken + "/edit"))
                .andExpect(flash().attribute("error", "Password must be at least 8 characters"));

        // Test password mismatch
        mockMvc.perform(patch("/passwords/" + fullToken)
                        .param("password", "password123")
                        .param("passwordConfirmation", "password456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passwords/" + fullToken + "/edit"))
                .andExpect(flash().attribute("error", "Passwords do not match"));
    }

    @Test
    void testNonExistentEmailDoesNotCreateToken() throws Exception {
        mockMvc.perform(post("/passwords").param("emailAddress", "nonexistent@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        flash().attribute(
                                        "notice",
                                        "If that email address is in our system, you will receive a password reset email shortly."));

        // Verify no token was created
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testPostMethodAlsoWorksForPasswordUpdate() throws Exception {
        // Create valid token
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);

        PasswordResetToken token = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        String fullToken = selector + ":" + verifier;

        // Use POST instead of PATCH
        mockMvc.perform(post("/passwords/" + fullToken)
                        .param("password", "newpassword123")
                        .param("passwordConfirmation", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andExpect(flash().attribute("notice", "Your password has been reset successfully"));

        // Verify password was updated
        User updatedUser =
                userRepository.findByEmailAddress("integration@example.com").get();
        assertTrue(passwordEncoder.matches("newpassword123", updatedUser.getPasswordDigest()));
    }
}
