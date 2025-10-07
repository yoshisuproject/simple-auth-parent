package com.yoshisuproject.simpleauth.repository;

import java.util.Optional;

import com.yoshisuproject.simpleauth.model.User;

/**
 * Repository interface for User data access operations.
 *
 * <p>
 * Provides CRUD (Create, Read, Update, Delete) operations for {@link User}
 * entities. This is the data access layer
 * abstraction that decouples the business logic from the underlying persistence
 * mechanism.
 *
 * <p>
 * All implementations should ensure thread-safety and proper transaction
 * management.
 */
public interface UserRepository {

    /**
     * Find user by ID
     *
     * @param id
     *            the user ID
     * @return Optional containing the user if found
     */
    Optional<User> findById(Long id);

    /**
     * Find user by email address
     *
     * @param emailAddress
     *            the email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailAddress(String emailAddress);

    /**
     * Check if a user with the given email address exists
     *
     * @param emailAddress
     *            the email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmailAddress(String emailAddress);

    /**
     * Save a user (insert or update)
     *
     * @param user
     *            the user to save
     * @return the saved user with generated ID if applicable
     */
    User save(User user);

    /**
     * Delete a user
     *
     * @param user
     *            the user to delete
     */
    void delete(User user);

    /** Delete all users */
    void deleteAll();

    /**
     * Count all users
     *
     * @return the number of users
     */
    long count();
}
