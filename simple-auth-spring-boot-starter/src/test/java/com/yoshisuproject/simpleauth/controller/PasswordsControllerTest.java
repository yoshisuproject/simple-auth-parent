package com.yoshisuproject.simpleauth.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.PasswordResetToken;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.PasswordResetTokenRepository;
import com.yoshisuproject.simpleauth.service.PasswordsMailer;
import com.yoshisuproject.simpleauth.service.UserService;

/**
 * Unit tests for PasswordsController Focuses on testing the selector/verifier
 * pattern and BCrypt security
 */
@ExtendWith(MockitoExtension.class)
class PasswordsControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordsMailer passwordsMailer;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    private PasswordsController controller;
    private SimpleAuthProperties properties;
    private PasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        properties = new SimpleAuthProperties();
        properties.getPasswordReset().setTokenExpiry(900); // 15 minutes

        controller = new PasswordsController(
                userService, passwordsMailer, passwordResetTokenRepository, properties, passwordEncoder);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmailAddress("test@example.com");
    }

    @Test
    void testNewPasswordReset() {
        String view = controller.newPasswordReset(model);

        assertEquals("passwords/new", view);
    }

    @Test
    void testCreateGeneratesCorrectTokenFormat() {
        when(userService.findByEmailAddress("test@example.com")).thenReturn(Optional.of(testUser));

        controller.create("test@example.com", redirectAttributes);

        // Verify token was saved
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getSelector());
        assertNotNull(savedToken.getVerifierHash());

        // Verify selector is a valid UUID format
        assertDoesNotThrow(() -> UUID.fromString(savedToken.getSelector()));

        // Verify verifierHash is BCrypt format (starts with $2a$ or $2b$)
        assertTrue(savedToken.getVerifierHash().startsWith("$2a$")
                || savedToken.getVerifierHash().startsWith("$2b$"));
    }

    @Test
    void testCreateHashesVerifierWithBCrypt() {
        when(userService.findByEmailAddress("test@example.com")).thenReturn(Optional.of(testUser));

        controller.create("test@example.com", redirectAttributes);

        // Capture the saved token
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();

        // Verify the hash is a valid BCrypt hash by checking its structure
        String hash = savedToken.getVerifierHash();
        assertNotNull(hash);
        assertTrue(hash.length() == 60); // BCrypt hashes are always 60 characters
        assertTrue(hash.matches("\\$2[aby]\\$\\d{2}\\$.{53}")); // BCrypt format regex
    }

    @Test
    void testCreateSendsEmailWithSelectorColonVerifierFormat() {
        when(userService.findByEmailAddress("test@example.com")).thenReturn(Optional.of(testUser));

        controller.create("test@example.com", redirectAttributes);

        // Capture the token sent in email
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordsMailer).sendPasswordResetEmail(eq(testUser), tokenCaptor.capture());

        String emailToken = tokenCaptor.getValue();

        // Verify format is "selector:verifier"
        assertTrue(emailToken.contains(":"));
        String[] parts = emailToken.split(":");
        assertEquals(2, parts.length);

        // Verify both parts are UUIDs
        assertDoesNotThrow(() -> UUID.fromString(parts[0])); // selector
        assertDoesNotThrow(() -> UUID.fromString(parts[1])); // verifier
    }

    @Test
    void testCreateDeletesOldTokensForUser() {
        when(userService.findByEmailAddress("test@example.com")).thenReturn(Optional.of(testUser));

        controller.create("test@example.com", redirectAttributes);

        verify(passwordResetTokenRepository).deleteByUserId(testUser.getId());
    }

    @Test
    void testCreateDoesNotRevealIfEmailDoesNotExist() {
        when(userService.findByEmailAddress("nonexistent@example.com")).thenReturn(Optional.empty());

        String result = controller.create("nonexistent@example.com", redirectAttributes);

        // Should still show success message
        verify(redirectAttributes).addFlashAttribute(eq("notice"), anyString());
        assertEquals("redirect:" + properties.getUrls().getLoginUrl(), result);

        // Should not send email or create token
        verify(passwordsMailer, never()).sendPasswordResetEmail(any(), any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void testEditParsesTokenCorrectly() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));

        String result = controller.edit(fullToken, model, redirectAttributes);

        assertEquals("passwords/edit", result);
        verify(model).addAttribute("token", fullToken);
    }

    @Test
    void testEditVerifiesBCryptHash() {
        String selector = UUID.randomUUID().toString();
        String correctVerifier = UUID.randomUUID().toString();
        String wrongVerifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(correctVerifier);

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));

        // Test with correct verifier
        String correctToken = selector + ":" + correctVerifier;
        String result = controller.edit(correctToken, model, redirectAttributes);
        assertEquals("passwords/edit", result);

        // Test with wrong verifier
        String wrongToken = selector + ":" + wrongVerifier;
        result = controller.edit(wrongToken, model, redirectAttributes);
        assertEquals("redirect:/passwords/new", result);
        verify(redirectAttributes).addFlashAttribute("error", "Invalid password reset link");
    }

    @Test
    void testEditRejectsInvalidTokenFormat() {
        String invalidToken = "no-colon-separator";

        String result = controller.edit(invalidToken, model, redirectAttributes);

        assertEquals("redirect:/passwords/new", result);
        verify(redirectAttributes).addFlashAttribute("error", "Invalid password reset link");
        verify(passwordResetTokenRepository, never()).findBySelector(any());
    }

    @Test
    void testEditRejectsTokenWithMultipleColons() {
        String invalidToken = "selector:verifier:extra";

        controller.edit(invalidToken, model, redirectAttributes);

        // Should only split into 2 parts max, so this should fail at verification
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void testEditRejectsExpiredToken() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);

        // Create expired token
        PasswordResetToken expiredToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().minusMinutes(15) // Expired 15 minutes ago
                );

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(expiredToken));

        String fullToken = selector + ":" + verifier;
        String result = controller.edit(fullToken, model, redirectAttributes);

        assertEquals("redirect:/passwords/new", result);
        verify(redirectAttributes).addFlashAttribute("error", "Invalid or expired password reset link");
    }

    @Test
    void testEditRejectsNonExistentSelector() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.empty());

        String fullToken = selector + ":" + verifier;
        String result = controller.edit(fullToken, model, redirectAttributes);

        assertEquals("redirect:/passwords/new", result);
        verify(redirectAttributes).addFlashAttribute("error", "Invalid or expired password reset link");
    }

    @Test
    void testUpdateSuccessfully() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));
        when(userService.resetPassword(testUser.getId(), "newpassword123")).thenReturn(true);

        String result = controller.update(fullToken, "newpassword123", "newpassword123", redirectAttributes);

        assertEquals("redirect:" + properties.getUrls().getLoginUrl(), result);
        verify(userService).resetPassword(testUser.getId(), "newpassword123");
        verify(passwordResetTokenRepository).deleteBySelector(selector);
        verify(redirectAttributes).addFlashAttribute("notice", "Your password has been reset successfully");
    }

    @Test
    void testUpdateRejectsPasswordMismatch() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));

        String result = controller.update(fullToken, "password1", "password2", redirectAttributes);

        assertEquals("redirect:/passwords/" + fullToken + "/edit", result);
        verify(redirectAttributes).addFlashAttribute("error", "Passwords do not match");
        verify(userService, never()).resetPassword(any(), any());
        verify(passwordResetTokenRepository, never()).deleteBySelector(any());
    }

    @Test
    void testUpdateRejectsShortPassword() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));

        String result = controller.update(fullToken, "short", "short", redirectAttributes);

        assertEquals("redirect:/passwords/" + fullToken + "/edit", result);
        verify(redirectAttributes).addFlashAttribute("error", "Password must be at least 8 characters");
        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    void testUpdateVerifiesTokenBeforePasswordReset() {
        String selector = UUID.randomUUID().toString();
        String correctVerifier = UUID.randomUUID().toString();
        String wrongVerifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(correctVerifier);
        String wrongToken = selector + ":" + wrongVerifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));

        String result = controller.update(wrongToken, "newpassword123", "newpassword123", redirectAttributes);

        assertEquals("redirect:/passwords/new", result);
        verify(userService, never()).resetPassword(any(), any());
        verify(passwordResetTokenRepository, never()).deleteBySelector(any());
    }

    @Test
    void testUpdateDoesNotDeleteTokenOnFailure() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));
        when(userService.resetPassword(testUser.getId(), "newpassword123")).thenReturn(false);

        String result = controller.update(fullToken, "newpassword123", "newpassword123", redirectAttributes);

        assertEquals("redirect:/passwords/new", result);
        verify(redirectAttributes).addFlashAttribute("error", "Unable to reset password");
        verify(passwordResetTokenRepository, never()).deleteBySelector(any());
    }

    @Test
    void testUpdatePostDelegatesToUpdate() {
        String selector = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString();
        String verifierHash = passwordEncoder.encode(verifier);
        String fullToken = selector + ":" + verifier;

        PasswordResetToken resetToken = new PasswordResetToken(
                testUser.getId(), selector, verifierHash, LocalDateTime.now().plusMinutes(15));

        when(passwordResetTokenRepository.findBySelector(selector)).thenReturn(Optional.of(resetToken));
        when(userService.resetPassword(testUser.getId(), "newpassword123")).thenReturn(true);

        // Call POST endpoint
        String result = controller.updatePost(fullToken, "newpassword123", "newpassword123", redirectAttributes);

        // Should behave exactly like PATCH
        assertEquals("redirect:" + properties.getUrls().getLoginUrl(), result);
        verify(userService).resetPassword(testUser.getId(), "newpassword123");
        verify(passwordResetTokenRepository).deleteBySelector(selector);
    }

    @Test
    void testCreateSetsCorrectExpirationTime() {
        when(userService.findByEmailAddress("test@example.com")).thenReturn(Optional.of(testUser));

        LocalDateTime beforeCreation = LocalDateTime.now();
        controller.create("test@example.com", redirectAttributes);
        LocalDateTime afterCreation = LocalDateTime.now();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        LocalDateTime expiresAt = savedToken.getExpiresAt();

        // Verify expiration is approximately 900 seconds (15 minutes) from now
        LocalDateTime expectedExpiration = beforeCreation.plusSeconds(900);
        assertTrue(expiresAt.isAfter(expectedExpiration.minusSeconds(5)));
        assertTrue(expiresAt.isBefore(afterCreation.plusSeconds(905)));
    }
}
