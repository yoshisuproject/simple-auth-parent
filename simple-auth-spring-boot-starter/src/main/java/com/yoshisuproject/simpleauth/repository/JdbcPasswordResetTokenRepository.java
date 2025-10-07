package com.yoshisuproject.simpleauth.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.yoshisuproject.simpleauth.model.PasswordResetToken;

/**
 * JDBC-based implementation of {@link PasswordResetTokenRepository}.
 *
 * <p>
 * This implementation manages password reset tokens using Spring's
 * {@link JdbcTemplate}. It provides efficient token
 * storage and retrieval, including a bulk deletion method for removing expired
 * tokens.
 *
 * <p>
 * The {@link #deleteExpiredTokens(LocalDateTime)} method is designed to be
 * called by a scheduled cleanup service to
 * maintain database hygiene and prevent accumulation of expired tokens.
 */
@Repository
public class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_SELECTOR =
            "SELECT id, user_id, selector, verifier_hash, expires_at, created_at FROM password_reset_tokens WHERE selector = ?";

    private static final String FIND_BY_USER_ID =
            "SELECT id, user_id, selector, verifier_hash, expires_at, created_at FROM password_reset_tokens WHERE user_id = ?";

    private static final String INSERT_TOKEN =
            "INSERT INTO password_reset_tokens (user_id, selector, verifier_hash, expires_at, created_at) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_TOKEN =
            "UPDATE password_reset_tokens SET user_id = ?, selector = ?, verifier_hash = ?, expires_at = ? WHERE id = ?";

    private static final String DELETE_TOKEN = "DELETE FROM password_reset_tokens WHERE id = ?";

    private static final String DELETE_BY_SELECTOR = "DELETE FROM password_reset_tokens WHERE selector = ?";

    private static final String DELETE_BY_USER_ID = "DELETE FROM password_reset_tokens WHERE user_id = ?";

    private static final String DELETE_EXPIRED = "DELETE FROM password_reset_tokens WHERE expires_at < ?";

    private static final String DELETE_ALL = "DELETE FROM password_reset_tokens";

    private static final String COUNT_TOKENS = "SELECT COUNT(*) FROM password_reset_tokens";

    public JdbcPasswordResetTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PasswordResetToken> findBySelector(String selector) {
        try {
            PasswordResetToken resetToken =
                    jdbcTemplate.queryForObject(FIND_BY_SELECTOR, new PasswordResetTokenRowMapper(), selector);
            return Optional.ofNullable(resetToken);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PasswordResetToken> findByUserId(Long userId) {
        try {
            PasswordResetToken resetToken =
                    jdbcTemplate.queryForObject(FIND_BY_USER_ID, new PasswordResetTokenRowMapper(), userId);
            return Optional.ofNullable(resetToken);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        if (token.getId() == null) {
            // Insert new token
            if (token.getCreatedAt() == null) {
                token.setCreatedAt(LocalDateTime.now());
            }

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps = connection.prepareStatement(INSERT_TOKEN, new String[] {"id"});
                        ps.setLong(1, token.getUserId());
                        ps.setString(2, token.getSelector());
                        ps.setString(3, token.getVerifierHash());
                        ps.setTimestamp(4, Timestamp.valueOf(token.getExpiresAt()));
                        ps.setTimestamp(5, Timestamp.valueOf(token.getCreatedAt()));
                        return ps;
                    },
                    keyHolder);

            token.setId(keyHolder.getKey().longValue());
        } else {
            // Update existing token
            jdbcTemplate.update(
                    UPDATE_TOKEN,
                    token.getUserId(),
                    token.getSelector(),
                    token.getVerifierHash(),
                    Timestamp.valueOf(token.getExpiresAt()),
                    token.getId());
        }

        return token;
    }

    @Override
    public void delete(PasswordResetToken token) {
        jdbcTemplate.update(DELETE_TOKEN, token.getId());
    }

    @Override
    public void deleteBySelector(String selector) {
        jdbcTemplate.update(DELETE_BY_SELECTOR, selector);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jdbcTemplate.update(DELETE_BY_USER_ID, userId);
    }

    @Override
    public int deleteExpiredTokens(LocalDateTime now) {
        return jdbcTemplate.update(DELETE_EXPIRED, Timestamp.valueOf(now));
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update(DELETE_ALL);
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject(COUNT_TOKENS, Long.class);
        return count != null ? count : 0L;
    }

    /** RowMapper for PasswordResetToken entity */
    private static class PasswordResetTokenRowMapper implements RowMapper<PasswordResetToken> {
        @Override
        public PasswordResetToken mapRow(ResultSet rs, int rowNum) throws SQLException {
            PasswordResetToken token = new PasswordResetToken();
            token.setId(rs.getLong("id"));
            token.setUserId(rs.getLong("user_id"));
            token.setSelector(rs.getString("selector"));
            token.setVerifierHash(rs.getString("verifier_hash"));

            Timestamp expiresAt = rs.getTimestamp("expires_at");
            if (expiresAt != null) {
                token.setExpiresAt(expiresAt.toLocalDateTime());
            }

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                token.setCreatedAt(createdAt.toLocalDateTime());
            }

            return token;
        }
    }
}
