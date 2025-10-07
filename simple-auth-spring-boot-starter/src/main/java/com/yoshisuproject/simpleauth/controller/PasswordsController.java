package com.yoshisuproject.simpleauth.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.PasswordResetToken;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.PasswordResetTokenRepository;
import com.yoshisuproject.simpleauth.service.PasswordsMailer;
import com.yoshisuproject.simpleauth.service.UserService;

/**
 * Controller for handling password reset workflows.
 *
 * <p>
 * This controller implements a secure password reset flow:
 *
 * <ol>
 * <li>User requests reset by providing their email address
 * <li>System generates a unique, time-limited token and emails it to the user
 * <li>User clicks the link in the email to access the reset form
 * <li>User submits new password, which is validated and saved
 * <li>Token is immediately deleted after successful password reset
 * </ol>
 *
 * <p>
 * Security features:
 *
 * <ul>
 * <li>Tokens expire after a configurable duration (default: 15 minutes)
 * <li>Only one active token per user (old tokens are deleted when new ones are
 * created)
 * <li>Email existence is not revealed (same success message regardless)
 * <li>Password must be at least 8 characters
 * <li>Password confirmation is required
 * </ul>
 *
 * <p>
 * Configuration:
 *
 * <ul>
 * <li>{@code simple-auth.password-reset.token-expiry} - Token expiration in
 * seconds (default: 900)
 * </ul>
 *
 * <p>
 * Routes:
 *
 * <ul>
 * <li>GET /passwords/new - Show password reset request form
 * <li>POST /passwords - Send password reset email
 * <li>GET /passwords/{token}/edit - Show password reset form
 * <li>PATCH /passwords/{token} - Update password
 * <li>POST /passwords/{token} - Update password (fallback for browsers without
 * PATCH support)
 * </ul>
 */
@Controller
@ConditionalOnProperty(
        prefix = "simple-auth.password-reset",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequestMapping("/passwords")
public class PasswordsController {

    private final UserService userService;
    private final PasswordsMailer passwordsMailer;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SimpleAuthProperties properties;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final int tokenExpirySeconds;

    public PasswordsController(
            UserService userService,
            PasswordsMailer passwordsMailer,
            PasswordResetTokenRepository passwordResetTokenRepository,
            SimpleAuthProperties properties,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordsMailer = passwordsMailer;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.tokenExpirySeconds = properties.getPasswordReset().getTokenExpiry();
    }

    /**
     * GET /passwords/new - Show password reset request form.
     *
     * @param model
     *            the view model
     * @return the reset request view name
     */
    @GetMapping("/new")
    public String newPasswordReset(Model model) {
        return "passwords/new";
    }

    /**
     * POST /passwords - Send password reset email.
     *
     * @param emailAddress
     *            the user's email address
     * @param redirectAttributes
     *            flash attributes for feedback messages
     * @return redirect to login page with notice
     */
    @PostMapping
    public String create(@RequestParam("emailAddress") String emailAddress, RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userService.findByEmailAddress(emailAddress);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Generate selector and verifier
            String selector = UUID.randomUUID().toString();
            String verifier = UUID.randomUUID().toString();

            // Hash the verifier with BCrypt
            String verifierHash = passwordEncoder.encode(verifier);

            // Delete any existing tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getId());

            // Create and store new token with selector and verifier hash
            PasswordResetToken resetToken = new PasswordResetToken(
                    user.getId(), selector, verifierHash, LocalDateTime.now().plusSeconds(tokenExpirySeconds));
            passwordResetTokenRepository.save(resetToken);

            // Combine selector and verifier to create the full token
            String fullToken = selector + ":" + verifier;

            // Send password reset email with full token
            passwordsMailer.sendPasswordResetEmail(user, fullToken);
        }

        // Always show success message (security: don't reveal if email exists)
        redirectAttributes.addFlashAttribute(
                "notice", "If that email address is in our system, you will receive a password reset email shortly.");
        return "redirect:" + properties.getUrls().getLoginUrl();
    }

    /**
     * GET /passwords/{token}/edit - Show password reset form.
     *
     * @param token
     *            the password reset token
     * @param model
     *            the view model to populate
     * @param redirectAttributes
     *            flash attributes for errors
     * @return the reset form view or redirect when invalid/expired
     */
    @GetMapping("/{token}/edit")
    public String edit(@PathVariable String token, Model model, RedirectAttributes redirectAttributes) {
        // Parse token into selector and verifier
        String[] parts = token.split(":", 2);
        if (parts.length != 2) {
            redirectAttributes.addFlashAttribute("error", "Invalid password reset link");
            return "redirect:/passwords/new";
        }

        String selector = parts[0];
        String verifier = parts[1];

        // Find token by selector
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findBySelector(selector);

        if (resetTokenOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset link");
            return "redirect:/passwords/new";
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Check expiration
        if (resetToken.isExpired()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset link");
            return "redirect:/passwords/new";
        }

        // Verify the verifier against the stored hash
        if (!passwordEncoder.matches(verifier, resetToken.getVerifierHash())) {
            redirectAttributes.addFlashAttribute("error", "Invalid password reset link");
            return "redirect:/passwords/new";
        }

        model.addAttribute("token", token);
        return "passwords/edit";
    }

    /**
     * PATCH /passwords/{token} - Update password.
     *
     * @param token
     *            the password reset token
     * @param password
     *            the new password
     * @param passwordConfirmation
     *            the new password confirmation
     * @param redirectAttributes
     *            flash attributes for user feedback
     * @return redirect to login on success, or back to form on validation failure
     */
    @PatchMapping("/{token}")
    public String update(
            @PathVariable String token,
            @RequestParam String password,
            @RequestParam("passwordConfirmation") String passwordConfirmation,
            RedirectAttributes redirectAttributes) {

        // Parse token into selector and verifier
        String[] parts = token.split(":", 2);
        if (parts.length != 2) {
            redirectAttributes.addFlashAttribute("error", "Invalid password reset link");
            return "redirect:/passwords/new";
        }

        String selector = parts[0];
        String verifier = parts[1];

        // Find token by selector
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findBySelector(selector);

        if (resetTokenOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset link");
            return "redirect:/passwords/new";
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Check expiration
        if (resetToken.isExpired()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset link");
            return "redirect:/passwords/new";
        }

        // Verify the verifier against the stored hash
        if (!passwordEncoder.matches(verifier, resetToken.getVerifierHash())) {
            redirectAttributes.addFlashAttribute("error", "Invalid password reset link");
            return "redirect:/passwords/new";
        }

        if (!password.equals(passwordConfirmation)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/passwords/" + token + "/edit";
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/passwords/" + token + "/edit";
        }

        // Reset password using UserService (transactional)
        boolean success = userService.resetPassword(resetToken.getUserId(), password);

        if (success) {
            // Remove used token from database by selector
            passwordResetTokenRepository.deleteBySelector(selector);

            redirectAttributes.addFlashAttribute("notice", "Your password has been reset successfully");
            return "redirect:" + properties.getUrls().getLoginUrl();
        }

        redirectAttributes.addFlashAttribute("error", "Unable to reset password");
        return "redirect:/passwords/new";
    }

    /**
     * POST /passwords/{token} - Fallback for browsers without PATCH support.
     * Performs the same update as PATCH.
     *
     * @param token
     *            the password reset token
     * @param password
     *            the new password
     * @param passwordConfirmation
     *            the new password confirmation
     * @param redirectAttributes
     *            flash attributes for user feedback
     * @return redirect destination from
     *         {@link #update(String, String, String, RedirectAttributes)}
     */
    @PostMapping("/{token}")
    public String updatePost(
            @PathVariable String token,
            @RequestParam String password,
            @RequestParam("passwordConfirmation") String passwordConfirmation,
            RedirectAttributes redirectAttributes) {
        return update(token, password, passwordConfirmation, redirectAttributes);
    }
}
