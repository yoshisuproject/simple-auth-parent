package com.yoshisuproject.simpleauth.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.UserRepository;

/**
 * Business logic service for user management operations.
 *
 * <p>
 * This service provides core user-related functionality including:
 *
 * <ul>
 * <li>User authentication (credential verification)
 * <li>Password reset operations
 * <li>User lookup by email or ID
 * </ul>
 *
 * <p>
 * All password operations use BCrypt hashing via the configured
 * {@link PasswordEncoder}. Operations that modify user
 * data are marked as {@code @Transactional} to ensure data consistency.
 *
 * <p>
 * This service acts as a facade over the {@link UserRepository}, adding
 * business logic such as password encoding and
 * authentication verification.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate a user with email and password
     *
     * @param emailAddress
     *            the user's email address
     * @param password
     *            the plain text password
     * @return Optional containing the user if authentication succeeds, empty
     *         otherwise
     */
    public Optional<User> authenticateUser(String emailAddress, String password) {
        Optional<User> userOpt = userRepository.findByEmailAddress(emailAddress);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPasswordDigest())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Reset user password This operation is transactional to ensure data
     * consistency
     *
     * @param userId
     *            the user ID
     * @param newPassword
     *            the new plain text password
     * @return true if password was reset successfully, false if user not found
     */
    @Transactional
    public boolean resetPassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordDigest(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    /**
     * Find user by email address
     *
     * @param emailAddress
     *            the email address
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmailAddress(String emailAddress) {
        return userRepository.findByEmailAddress(emailAddress);
    }

    /**
     * Find user by ID
     *
     * @param id
     *            the user ID
     * @return Optional containing the user if found
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
