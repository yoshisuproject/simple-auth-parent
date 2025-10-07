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

import com.yoshisuproject.simpleauth.model.User;

/**
 * JDBC-based implementation of {@link UserRepository}.
 *
 * <p>
 * This implementation uses Spring's {@link JdbcTemplate} to execute SQL queries
 * against the users table. It provides
 * direct database access without an ORM layer, offering predictable performance
 * and fine-grained control over SQL
 * execution.
 *
 * <p>
 * All timestamp fields are stored in the database using the database's native
 * timestamp type and converted to
 * {@link LocalDateTime} when read.
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_ID =
            "SELECT id, email_address, password_digest, created_at, updated_at " + "FROM users WHERE id = ?";

    private static final String FIND_BY_EMAIL =
            "SELECT id, email_address, password_digest, created_at, updated_at " + "FROM users WHERE email_address = ?";

    private static final String EXISTS_BY_EMAIL = "SELECT COUNT(*) FROM users WHERE email_address = ?";

    private static final String INSERT_USER =
            "INSERT INTO users (email_address, password_digest, created_at, updated_at) " + "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_USER =
            "UPDATE users SET email_address = ?, password_digest = ?, updated_at = ? " + "WHERE id = ?";

    private static final String DELETE_USER = "DELETE FROM users WHERE id = ?";

    private static final String DELETE_ALL_USERS = "DELETE FROM users";

    private static final String COUNT_USERS = "SELECT COUNT(*) FROM users";

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(FIND_BY_ID, new UserRowMapper(), id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmailAddress(String emailAddress) {
        try {
            User user = jdbcTemplate.queryForObject(FIND_BY_EMAIL, new UserRowMapper(), emailAddress);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmailAddress(String emailAddress) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BY_EMAIL, Integer.class, emailAddress);
        return count != null && count > 0;
    }

    @Override
    public User save(User user) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getId() == null) {
            // Insert new user
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps = connection.prepareStatement(INSERT_USER, new String[] {"id"});
                        ps.setString(1, user.getEmailAddress());
                        ps.setString(2, user.getPasswordDigest());
                        ps.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
                        ps.setTimestamp(4, Timestamp.valueOf(user.getUpdatedAt()));
                        return ps;
                    },
                    keyHolder);

            user.setId(keyHolder.getKey().longValue());
        } else {
            // Update existing user
            user.setUpdatedAt(now);
            jdbcTemplate.update(
                    UPDATE_USER,
                    user.getEmailAddress(),
                    user.getPasswordDigest(),
                    Timestamp.valueOf(now),
                    user.getId());
        }

        return user;
    }

    @Override
    public void delete(User user) {
        jdbcTemplate.update(DELETE_USER, user.getId());
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update(DELETE_ALL_USERS);
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject(COUNT_USERS, Long.class);
        return count != null ? count : 0L;
    }

    /** RowMapper for User entity */
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmailAddress(rs.getString("email_address"));
            user.setPasswordDigest(rs.getString("password_digest"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                user.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            return user;
        }
    }
}
