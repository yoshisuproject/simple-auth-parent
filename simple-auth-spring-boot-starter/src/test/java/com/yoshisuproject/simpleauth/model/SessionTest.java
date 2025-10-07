package com.yoshisuproject.simpleauth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for Session model */
class SessionTest {

    private User user;
    private Session session;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress("test@example.com");
        user.setPasswordDigest("hashed_password");
    }

    @Test
    void testSessionCreationWithConstructor() {
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        session = new Session(user, ipAddress, userAgent);

        assertEquals(user, session.getUser());
        assertEquals(ipAddress, session.getIpAddress());
        assertEquals(userAgent, session.getUserAgent());
        assertNotNull(session.getToken());
        // Note: @CreationTimestamp only works in JPA context, not in pure unit tests
        // So we don't assert createdAt here
    }

    @Test
    void testSessionTokenIsUUID() {
        session = new Session(user, "192.168.1.1", "Mozilla/5.0");

        String token = session.getToken();
        assertNotNull(token);
        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (36 characters)
        assertEquals(36, token.length());
        assertTrue(token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testSessionTokenIsUnique() {
        Session session1 = new Session(user, "192.168.1.1", "Mozilla/5.0");
        Session session2 = new Session(user, "192.168.1.1", "Mozilla/5.0");

        assertNotEquals(session1.getToken(), session2.getToken());
    }

    @Test
    void testSessionWithNullIpAndUserAgent() {
        session = new Session(user, null, null);

        assertEquals(user, session.getUser());
        assertNull(session.getIpAddress());
        assertNull(session.getUserAgent());
        assertNotNull(session.getToken());
    }

    @Test
    void testSessionSetters() {
        session = new Session();
        session.setId(1L);
        session.setUser(user);
        session.setToken("custom-token");
        session.setIpAddress("10.0.0.1");
        session.setUserAgent("Chrome");

        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);

        assertEquals(1L, session.getId());
        assertEquals(user, session.getUser());
        assertEquals("custom-token", session.getToken());
        assertEquals("10.0.0.1", session.getIpAddress());
        assertEquals("Chrome", session.getUserAgent());
        assertEquals(now, session.getCreatedAt());
    }

    @Test
    void testSessionCreatedAtCanBeSet() {
        session = new Session(user, "192.168.1.1", "Mozilla/5.0");
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);

        assertNotNull(session.getCreatedAt());
        assertEquals(now, session.getCreatedAt());
        // Note: @CreationTimestamp auto-setting only works in JPA context
    }

    @Test
    void testMultipleSessionsForSameUser() {
        Session session1 = new Session(user, "192.168.1.1", "Mozilla/5.0");
        Session session2 = new Session(user, "192.168.1.2", "Chrome");

        assertEquals(user, session1.getUser());
        assertEquals(user, session2.getUser());
        assertNotEquals(session1.getToken(), session2.getToken());
        assertNotEquals(session1.getIpAddress(), session2.getIpAddress());
    }
}
