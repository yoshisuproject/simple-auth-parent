package com.yoshisuproject.simpleauth.model;

/**
 * Thread-local storage for current request's user and session.
 *
 * <pre>
 * Usage:
 * <code>
 * Current.setUser(user);
 * Current.setSession(session);
 * User currentUser = Current.getUser();
 * Session currentSession = Current.getSession();
 * Current.clear(); // Clear after request
 * </code>
 */
public class Current {

    private static final ThreadLocal<User> user = new ThreadLocal<>();

    private static final ThreadLocal<Session> session = new ThreadLocal<>();

    private Current() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the current request's authenticated user from thread-local storage.
     *
     * @return the current user, or null if unauthenticated
     */
    public static User getUser() {
        return user.get();
    }

    /**
     * Sets the current request's authenticated user in thread-local storage. Should
     * be cleared via {@link #clear()}
     * after request completion.
     *
     * @param currentUser
     *            the authenticated user to set
     */
    public static void setUser(User currentUser) {
        user.set(currentUser);
    }

    /**
     * Gets the current request's session from thread-local storage.
     *
     * @return the current session, or null if none
     */
    public static Session getSession() {
        return session.get();
    }

    /**
     * Sets the current request's session in thread-local storage. Should be cleared
     * via {@link #clear()} after request
     * completion.
     *
     * @param currentSession
     *            the session to set
     */
    public static void setSession(Session currentSession) {
        session.set(currentSession);
    }

    /**
     * Clear all thread-local variables. Should be called after each request to
     * prevent memory leaks.
     */
    public static void clear() {
        user.remove();
        session.remove();
    }

    /**
     * Check if there is an authenticated user for the current request.
     *
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return session.get() != null;
    }
}
