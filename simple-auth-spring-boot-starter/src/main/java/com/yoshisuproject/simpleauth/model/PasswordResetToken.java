package com.yoshisuproject.simpleauth.model;

import java.time.LocalDateTime;

/**
 * Represents a temporary password reset token for a user.
 *
 * <p>
 * Uses a secure selector + verifier pattern for token storage:
 *
 * <ul>
 * <li><b>Selector</b>: A random UUID stored in plaintext, used to quickly
 * locate the token record
 * <li><b>Verifier Hash</b>: A BCrypt hash of the verifier portion, used to
 * validate the token
 * </ul>
 *
 * <p>
 * The actual token sent to users has the format: {@code {selector}:{verifier}}
 *
 * <p>
 * This approach ensures that even if the database is compromised, attackers
 * cannot use the stored information to
 * forge valid reset tokens, as they only have access to the selector
 * (meaningless random ID) and the hashed verifier
 * (cannot be reversed).
 *
 * <p>
 * Tokens have a limited lifespan (default 15 minutes) to minimize security
 * risks. They are single-use and deleted
 * immediately after successful password reset.
 *
 * <p>
 * A scheduled cleanup service periodically removes expired tokens to maintain
 * database hygiene.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * String selector = UUID.randomUUID().toString();
 * String verifier = UUID.randomUUID().toString();
 * String verifierHash = passwordEncoder.encode(verifier);
 *
 * LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
 * PasswordResetToken token = new PasswordResetToken(userId, selector, verifierHash, expiresAt);
 * tokenRepository.save(token);
 *
 * // Send to user: selector + ":" + verifier
 * String tokenToSend = selector + ":" + verifier;
 * </pre>
 */
public class PasswordResetToken {

    private Long id;
    private Long userId;
    private String selector;
    private String verifierHash;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    /** Default constructor for JDBC mapping. */
    public PasswordResetToken() {}

    /**
     * Creates a new password reset token with selector and verifier hash.
     * Automatically sets the creation timestamp to
     * now.
     *
     * @param userId
     *            the user ID for whom this token is created
     * @param selector
     *            the selector portion (random UUID, stored in plaintext)
     * @param verifierHash
     *            the BCrypt hash of the verifier portion
     * @param expiresAt
     *            the expiration timestamp
     */
    public PasswordResetToken(Long userId, String selector, String verifierHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.selector = selector;
        this.verifierHash = verifierHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Gets the token's unique identifier.
     *
     * @return the token ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the token's unique identifier.
     *
     * @param id
     *            the token ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the ID of the user who requested the password reset.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId
     *            the user ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the selector portion of the token.
     *
     * @return the selector (random UUID)
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Sets the selector portion of the token.
     *
     * @param selector
     *            the selector (random UUID)
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }

    /**
     * Gets the BCrypt hash of the verifier portion.
     *
     * @return the verifier hash
     */
    public String getVerifierHash() {
        return verifierHash;
    }

    /**
     * Sets the BCrypt hash of the verifier portion.
     *
     * @param verifierHash
     *            the verifier hash
     */
    public void setVerifierHash(String verifierHash) {
        this.verifierHash = verifierHash;
    }

    /**
     * Gets the expiration timestamp for this token.
     *
     * @return the expiration timestamp
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Sets the expiration timestamp.
     *
     * @param expiresAt
     *            the expiration timestamp
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Gets the timestamp when this token was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt
     *            the creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Check if this token has expired
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "PasswordResetToken{"
                + "id="
                + id
                + ", userId="
                + userId
                + ", selector='"
                + selector
                + '\''
                + ", verifierHash='[PROTECTED]'"
                + ", expiresAt="
                + expiresAt
                + ", createdAt="
                + createdAt
                + '}';
    }
}
