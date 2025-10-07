package com.yoshisuproject.simpleauth.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;

/**
 * JDBC-based implementation of {@link SessionRepository}.
 *
 * <p>
 * This implementation uses Spring's {@link JdbcTemplate} to manage active user
 * sessions in the database. Sessions
 * are stored with their associated user data, and queries automatically join
 * the users table to populate the full
 * {@link User} object within each {@link Session}.
 *
 * <p>
 * This approach ensures that session lookups always return complete session
 * information without requiring additional
 * database queries.
 */
@Repository
public class JdbcSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_TOKEN =
            "SELECT s.id, s.user_id, s.token, s.ip_address, s.user_agent, s.created_at, "
                    + "u.email_address, u.password_digest, u.created_at as u_created_at, u.updated_at as u_updated_at "
                    + "FROM sessions s JOIN users u ON s.user_id = u.id WHERE s.token = ?";

    private static final String FIND_BY_USER_ID =
            "SELECT s.id, s.user_id, s.token, s.ip_address, s.user_agent, s.created_at, "
                    + "u.email_address, u.password_digest, u.created_at as u_created_at, u.updated_at as u_updated_at "
                    + "FROM sessions s JOIN users u ON s.user_id = u.id WHERE s.user_id = ?";

    private static final String DELETE_BY_USER_ID = "DELETE FROM sessions WHERE user_id = ?";

    private static final String INSERT_SESSION =
            "INSERT INTO sessions (user_id, token, ip_address, user_agent, created_at) " + "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_SESSION =
            "UPDATE sessions SET user_id = ?, token = ?, ip_address = ?, user_agent = ? " + "WHERE id = ?";

    private static final String DELETE_SESSION = "DELETE FROM sessions WHERE id = ?";

    private static final String DELETE_ALL_SESSIONS = "DELETE FROM sessions";

    private static final String COUNT_SESSIONS = "SELECT COUNT(*) FROM sessions";

    public JdbcSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Session> findByToken(String token) {
        try {
            Session session = jdbcTemplate.queryForObject(FIND_BY_TOKEN, new SessionRowMapper(), token);
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Session> findByUser(User user) {
        return jdbcTemplate.query(FIND_BY_USER_ID, new SessionRowMapper(), user.getId());
    }

    @Override
    public void deleteByUser(User user) {
        jdbcTemplate.update(DELETE_BY_USER_ID, user.getId());
    }

    @Override
    public Session save(Session session) {
        if (session.getId() == null) {
            // Insert new session
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(LocalDateTime.now());
            }

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps = connection.prepareStatement(INSERT_SESSION, new String[] {"id"});
                        ps.setLong(1, session.getUser().getId());
                        ps.setString(2, session.getToken());
                        ps.setString(3, session.getIpAddress());
                        ps.setString(4, session.getUserAgent());
                        ps.setTimestamp(5, Timestamp.valueOf(session.getCreatedAt()));
                        return ps;
                    },
                    keyHolder);

            session.setId(keyHolder.getKey().longValue());
        } else {
            // Update existing session
            jdbcTemplate.update(
                    UPDATE_SESSION,
                    session.getUser().getId(),
                    session.getToken(),
                    session.getIpAddress(),
                    session.getUserAgent(),
                    session.getId());
        }

        return session;
    }

    @Override
    public void delete(Session session) {
        jdbcTemplate.update(DELETE_SESSION, session.getId());
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update(DELETE_ALL_SESSIONS);
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject(COUNT_SESSIONS, Long.class);
        return count != null ? count : 0L;
    }

    /** RowMapper for Session entity with joined User data */
    private static class SessionRowMapper implements RowMapper<Session> {
        @Override
        public Session mapRow(ResultSet rs, int rowNum) throws SQLException {
            // Map User
            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setEmailAddress(rs.getString("email_address"));
            user.setPasswordDigest(rs.getString("password_digest"));

            Timestamp userCreatedAt = rs.getTimestamp("u_created_at");
            if (userCreatedAt != null) {
                user.setCreatedAt(userCreatedAt.toLocalDateTime());
            }

            Timestamp userUpdatedAt = rs.getTimestamp("u_updated_at");
            if (userUpdatedAt != null) {
                user.setUpdatedAt(userUpdatedAt.toLocalDateTime());
            }

            // Map Session
            Session session = new Session();
            session.setId(rs.getLong("id"));
            session.setUser(user);
            session.setToken(rs.getString("token"));
            session.setIpAddress(rs.getString("ip_address"));
            session.setUserAgent(rs.getString("user_agent"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                session.setCreatedAt(createdAt.toLocalDateTime());
            }

            return session;
        }
    }
}
