package com.yoshisuproject.simpleauth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for Current ThreadLocal utility */
class CurrentTest {

    private User user;
    private Session session;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress("test@example.com");

        session = new Session(user, "192.168.1.1", "Mozilla/5.0");
        session.setId(1L);
    }

    @AfterEach
    void tearDown() {
        // Always clear ThreadLocal to prevent memory leaks
        Current.clear();
    }

    @Test
    void testSetAndGetUser() {
        Current.setUser(user);

        User currentUser = Current.getUser();
        assertNotNull(currentUser);
        assertEquals(user.getId(), currentUser.getId());
        assertEquals(user.getEmailAddress(), currentUser.getEmailAddress());
    }

    @Test
    void testSetAndGetSession() {
        Current.setSession(session);

        Session currentSession = Current.getSession();
        assertNotNull(currentSession);
        assertEquals(session.getId(), currentSession.getId());
        assertEquals(session.getToken(), currentSession.getToken());
    }

    @Test
    void testIsAuthenticatedWhenSessionExists() {
        Current.setSession(session);

        assertTrue(Current.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWhenNoSession() {
        assertFalse(Current.isAuthenticated());
    }

    @Test
    void testClearRemovesUserAndSession() {
        Current.setUser(user);
        Current.setSession(session);

        assertTrue(Current.isAuthenticated());

        Current.clear();

        assertNull(Current.getUser());
        assertNull(Current.getSession());
        assertFalse(Current.isAuthenticated());
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        // Set user in main thread
        Current.setUser(user);

        // Create another user for the other thread
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmailAddress("other@example.com");

        Thread otherThread = new Thread(() -> {
            // This should be isolated from main thread
            assertNull(Current.getUser());

            // Set different user in this thread
            Current.setUser(otherUser);
            assertEquals(otherUser, Current.getUser());

            // Clean up
            Current.clear();
        });

        otherThread.start();
        otherThread.join();

        // Main thread should still have original user
        assertEquals(user, Current.getUser());

        // Clean up main thread
        Current.clear();
    }

    @Test
    void testGetUserReturnsNullWhenNotSet() {
        assertNull(Current.getUser());
    }

    @Test
    void testGetSessionReturnsNullWhenNotSet() {
        assertNull(Current.getSession());
    }

    @Test
    void testSetUserAndSessionTogether() {
        Current.setUser(user);
        Current.setSession(session);

        assertEquals(user, Current.getUser());
        assertEquals(session, Current.getSession());
        assertTrue(Current.isAuthenticated());
    }

    @Test
    void testClearIsIdempotent() {
        Current.setUser(user);
        Current.setSession(session);

        Current.clear();
        Current.clear(); // Should not throw exception

        assertNull(Current.getUser());
        assertNull(Current.getSession());
    }
}
