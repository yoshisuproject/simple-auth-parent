package com.yoshisuproject.simpleauth.repository;

import java.util.List;
import java.util.Optional;

import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;

/**
 * Repository interface for Session data access operations.
 *
 * <p>
 * Manages the persistence of active user sessions. Each session represents an
 * authenticated user's active connection
 * to the system, identified by a unique token stored in a browser cookie.
 *
 * <p>
 * Sessions can be queried by token (for authentication checks) or by user (for
 * managing multiple sessions or
 * implementing "logout all devices" functionality).
 */
public interface SessionRepository {

    /**
     * Find session by token
     *
     * @param token
     *            the session token
     * @return Optional containing the session if found
     */
    Optional<Session> findByToken(String token);

    /**
     * Find all sessions for a specific user
     *
     * @param user
     *            the user
     * @return list of sessions for the user
     */
    List<Session> findByUser(User user);

    /**
     * Delete all sessions for a specific user
     *
     * @param user
     *            the user
     */
    void deleteByUser(User user);

    /**
     * Save a session (insert or update)
     *
     * @param session
     *            the session to save
     * @return the saved session with generated ID if applicable
     */
    Session save(Session session);

    /**
     * Delete a session
     *
     * @param session
     *            the session to delete
     */
    void delete(Session session);

    /** Delete all sessions */
    void deleteAll();

    /**
     * Count all sessions
     *
     * @return the number of sessions
     */
    long count();
}
