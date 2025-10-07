package com.yoshisuproject.simpleauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.SessionRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Unit tests for AuthenticationService */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthenticationService authenticationService;
    private User user;
    private Session session;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(sessionRepository);

        user = new User();
        user.setId(1L);
        user.setEmailAddress("test@example.com");

        session = new Session(user, "192.168.1.1", "Mozilla/5.0");
        session.setId(1L);
    }

    @AfterEach
    void tearDown() {
        Current.clear();
    }

    @Test
    void testResumeSessionWithValidCookie() throws Exception {
        Cookie[] cookies = new Cookie[] {new Cookie("session_token", session.getToken())};

        when(request.getCookies()).thenReturn(cookies);
        when(sessionRepository.findByToken(session.getToken())).thenReturn(Optional.of(session));

        authenticationService.resumeSession(request);

        assertEquals(session, Current.getSession());
        assertEquals(user, Current.getUser());
    }

    @Test
    void testResumeSessionWithInvalidToken() throws Exception {
        Cookie[] cookies = new Cookie[] {new Cookie("session_token", "invalid-token")};

        when(request.getCookies()).thenReturn(cookies);
        when(sessionRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        authenticationService.resumeSession(request);

        assertNull(Current.getSession());
        assertNull(Current.getUser());
    }

    @Test
    void testResumeSessionWithNoCookies() throws Exception {
        when(request.getCookies()).thenReturn(null);

        authenticationService.resumeSession(request);

        assertNull(Current.getSession());
        assertNull(Current.getUser());
    }

    @Test
    void testResumeSessionWithOtherCookies() throws Exception {
        Cookie[] cookies = new Cookie[] {new Cookie("other_cookie", "value")};

        when(request.getCookies()).thenReturn(cookies);

        authenticationService.resumeSession(request);

        assertNull(Current.getSession());
        assertNull(Current.getUser());
        verify(sessionRepository, never()).findByToken(any());
    }

    @Test
    void testStartNewSessionFor() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        Session newSession = authenticationService.startNewSessionFor(user, request, response);

        assertNotNull(newSession);
        assertEquals(user, newSession.getUser());
        assertEquals("192.168.1.1", newSession.getIpAddress());
        assertEquals("Mozilla/5.0", newSession.getUserAgent());

        // Verify cookie was set
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals("session_token", cookie.getName());
        assertEquals(newSession.getToken(), cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(30 * 24 * 60 * 60, cookie.getMaxAge());

        // Verify Current was set
        assertEquals(newSession, Current.getSession());
        assertEquals(user, Current.getUser());
    }

    @Test
    void testStartNewSessionWithXForwardedFor() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Chrome");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session newSession = authenticationService.startNewSessionFor(user, request, response);

        assertEquals("10.0.0.1", newSession.getIpAddress()); // First IP in the list
    }

    @Test
    void testTerminateSession() throws Exception {
        Current.setSession(session);
        Current.setUser(user);

        authenticationService.terminateSession(response);

        verify(sessionRepository).delete(session);

        // Verify cookie was cleared
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals("session_token", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());

        // Verify Current was cleared
        assertNull(Current.getSession());
        assertNull(Current.getUser());
    }

    @Test
    void testTerminateSessionWhenNoSession() throws Exception {
        authenticationService.terminateSession(response);

        verify(sessionRepository, never()).delete(any());
        verify(response, never()).addCookie(any());
    }

    @Test
    void testAuthenticatedReturnsTrueWhenSessionExists() {
        Current.setSession(session);

        assertTrue(authenticationService.authenticated());
    }

    @Test
    void testAuthenticatedReturnsFalseWhenNoSession() {
        assertFalse(authenticationService.authenticated());
    }

    @Test
    void testRequireAuthenticationRedirectsWhenNotAuthenticated() throws Exception {
        authenticationService.requireAuthentication(response);

        verify(response).sendRedirect("/session/new");
    }

    @Test
    void testRequireAuthenticationDoesNothingWhenAuthenticated() throws Exception {
        Current.setSession(session);

        authenticationService.requireAuthentication(response);

        verify(response, never()).sendRedirect(any());
    }

    @Test
    void testGetClientIpWithXRealIP() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Chrome");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session newSession = authenticationService.startNewSessionFor(user, request, response);

        assertEquals("10.0.0.1", newSession.getIpAddress());
    }

    @Test
    void testGetClientIpFallbackToRemoteAddr() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Chrome");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session newSession = authenticationService.startNewSessionFor(user, request, response);

        assertEquals("192.168.1.1", newSession.getIpAddress());
    }
}
