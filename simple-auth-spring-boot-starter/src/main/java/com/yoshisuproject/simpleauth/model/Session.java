package com.yoshisuproject.simpleauth.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an active user session in the authentication system.
 *
 * <p>
 * This POJO tracks authenticated sessions with a unique token, associated user,
 * and metadata about the client (IP
 * address and user agent). Each session is created when a user successfully
 * logs in and is destroyed when they log out
 * or the session expires.
 *
 * <p>
 * The session token is a UUID that is stored in a cookie on the client side and
 * used to identify and restore the
 * session on subsequent requests.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * Session session = new Session(user, "192.168.1.1", "Mozilla/5.0...");
 * sessionRepository.save(session);
 * // session.getToken() returns a unique UUID string
 * </pre>
 */
public class Session {

    private Long id;
    private User user;
    private String token;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    /** Default constructor for JDBC mapping. */
    public Session() {}

    /**
     * Creates a new session for the given user with client metadata. Automatically
     * generates a unique UUID token.
     *
     * @param user
     *            the authenticated user
     * @param ipAddress
     *            the client's IP address
     * @param userAgent
     *            the client's user agent string
     */
    public Session(User user, String ipAddress, String userAgent) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    /**
     * Gets the session's unique identifier.
     *
     * @return the session ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the session's unique identifier.
     *
     * @param id
     *            the session ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user associated with this session.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with this session.
     *
     * @param user
     *            the user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the unique session token (UUID string). This token is stored in the
     * client's cookie.
     *
     * @return the session token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the session token.
     *
     * @param token
     *            the session token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the client's IP address.
     *
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the client's IP address.
     *
     * @param ipAddress
     *            the IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the client's user agent string.
     *
     * @return the user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the client's user agent string.
     *
     * @param userAgent
     *            the user agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the timestamp when this session was created.
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
}
