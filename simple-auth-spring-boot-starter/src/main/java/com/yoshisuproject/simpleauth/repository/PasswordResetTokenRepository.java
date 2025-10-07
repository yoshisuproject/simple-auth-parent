package com.yoshisuproject.simpleauth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.yoshisuproject.simpleauth.model.PasswordResetToken;

/**
 * Repository interface for PasswordResetToken data access operations.
 *
 * <p>
 * Manages the persistence of temporary password reset tokens. These tokens are
 * generated when users request a
 * password reset and are sent to their registered email address. The tokens
 * have a limited lifespan and should be
 * deleted after use or expiration.
 *
 * <p>
 * This repository includes a special method for bulk deletion of expired
 * tokens, which should be called periodically
 * by a scheduled cleanup service to maintain database hygiene.
 */
public interface PasswordResetTokenRepository {

    /**
     * Find a password reset token by selector
     *
     * @param selector
     *            the selector portion of the token
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findBySelector(String selector);

    /**
     * Find password reset token by user ID
     *
     * @param userId
     *            the user ID
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findByUserId(Long userId);

    /**
     * Save a password reset token
     *
     * @param token
     *            the token to save
     * @return the saved token with generated ID
     */
    PasswordResetToken save(PasswordResetToken token);

    /**
     * Delete a password reset token
     *
     * @param token
     *            the token to delete
     */
    void delete(PasswordResetToken token);

    /**
     * Delete password reset token by selector
     *
     * @param selector
     *            the selector portion of the token
     */
    void deleteBySelector(String selector);

    /**
     * Delete all password reset tokens for a user
     *
     * @param userId
     *            the user ID
     */
    void deleteByUserId(Long userId);

    /**
     * Delete all expired password reset tokens
     *
     * @param now
     *            the current timestamp
     * @return the number of deleted tokens
     */
    int deleteExpiredTokens(LocalDateTime now);

    /** Delete all password reset tokens */
    void deleteAll();

    /**
     * Count all password reset tokens
     *
     * @return the count
     */
    long count();
}
