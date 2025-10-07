package com.yoshisuproject.simpleauth.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.yoshisuproject.simpleauth.repository.PasswordResetTokenRepository;

/**
 * Scheduled service for cleaning up expired password reset tokens.
 *
 * <p>
 * This service runs automatically on a fixed schedule (every hour) to remove
 * expired password reset tokens from the
 * database. This prevents table bloat and maintains optimal database
 * performance.
 *
 * <p>
 * The cleanup is non-blocking and logs the number of tokens removed. If no
 * expired tokens are found, only a debug
 * message is logged to avoid unnecessary log noise.
 *
 * <p>
 * Scheduling is enabled via Spring's {@code @EnableScheduling} annotation in
 * the auto-configuration.
 */
@Service
@ConditionalOnProperty(
        prefix = "simple-auth.password-reset",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PasswordResetTokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenCleanupService.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenCleanupService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Cleanup expired password reset tokens Runs every hour (3600000 milliseconds)
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired password reset tokens");

        int deletedCount = passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired password reset token(s)", deletedCount);
        } else {
            logger.debug("No expired password reset tokens to clean up");
        }
    }
}
