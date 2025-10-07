package com.yoshisuproject.simpleauth.model;

import java.time.LocalDateTime;

/**
 * Represents an authenticated user in the system.
 *
 * <p>
 * This is a POJO (Plain Old Java Object) designed for JDBC operations. It
 * stores user authentication credentials and
 * metadata including email address, password digest (hashed password), and
 * timestamps for creation and last update.
 *
 * <p>
 * The password is never stored in plain text. Only the BCrypt-hashed digest is
 * persisted.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * User user = new User("user@example.com", hashedPassword);
 * userRepository.save(user);
 * </pre>
 */
public class User {

    private Long id;
    private String emailAddress;
    private String passwordDigest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor for JDBC mapping. */
    public User() {}

    /**
     * Creates a new user with email and password digest.
     *
     * @param emailAddress
     *            the user's email address
     * @param passwordDigest
     *            the BCrypt-hashed password
     */
    public User(String emailAddress, String passwordDigest) {
        this.emailAddress = emailAddress;
        this.passwordDigest = passwordDigest;
    }

    /**
     * Gets the user's unique identifier.
     *
     * @return the user ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the user's unique identifier.
     *
     * @param id
     *            the user ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user's email address.
     *
     * @return the email address
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the user's email address.
     *
     * @param emailAddress
     *            the email address
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Gets the user's password digest (BCrypt hash).
     *
     * @return the hashed password
     */
    public String getPasswordDigest() {
        return passwordDigest;
    }

    /**
     * Sets the user's password digest.
     *
     * @param passwordDigest
     *            the BCrypt-hashed password
     */
    public void setPasswordDigest(String passwordDigest) {
        this.passwordDigest = passwordDigest;
    }

    /**
     * Gets the timestamp when this user was created.
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
     * Gets the timestamp when this user was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt
     *            the last update timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
