package com.yoshisuproject.simpleauth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.SessionRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication service - Provides core authentication business logic.
 *
 * <p>
 * This service provides fundamental authentication methods that can be used
 * across the application. It handles
 * session management, authentication checks, and cookie operations.
 *
 * <p>
 * All session cookie settings (name, path, max age, HTTP-only flag) and URL
 * redirects (login page) are configurable
 * via {@link SimpleAuthProperties}. This allows customization through
 * application configuration files without code
 * changes.
 *
 * @see SimpleAuthProperties
 */
@Service
public class AuthenticationService {

    private final SessionRepository sessionRepository;
    private final SimpleAuthProperties properties;

    /**
     * Creates AuthenticationService with explicit properties.
     *
     * @param sessionRepository
     *            repository for sessions
     * @param properties
     *            configuration properties (cookie name, max age, urls)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AuthenticationService(SessionRepository sessionRepository, SimpleAuthProperties properties) {
        this.sessionRepository = sessionRepository;
        this.properties = properties != null ? properties : new SimpleAuthProperties();
    }

    /**
     * Backward-compatible constructor for tests that don't wire properties. Uses
     * default values from
     * {@link SimpleAuthProperties}.
     *
     * @param sessionRepository
     *            repository for sessions
     */
    public AuthenticationService(SessionRepository sessionRepository) {
        this(sessionRepository, new SimpleAuthProperties());
    }

    /**
     * Check if the current request is authenticated
     *
     * @return true if authenticated, false otherwise
     */
    public boolean authenticated() {
        return Current.isAuthenticated();
    }

    /**
     * Require authentication for the current request. If not authenticated,
     * redirects to the configured login page.
     *
     * @param response
     *            the HTTP response
     * @throws Exception
     *             if redirect fails
     */
    public void requireAuthentication(HttpServletResponse response) throws Exception {
        if (!authenticated()) {
            String loginUrl = properties.getUrls().getLoginUrl();
            response.sendRedirect(loginUrl);
        }
    }

    /**
     * Resume session from cookie. This method restores the session from the cookie
     * if it exists. The cookie name is
     * determined by {@code simple-auth.session.cookie-name} configuration.
     *
     * @param request
     *            the HTTP request
     */
    public void resumeSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.getSession().getCookieName().equals(cookie.getName())) {
                    String token = cookie.getValue();
                    Optional<Session> sessionOpt = sessionRepository.findByToken(token);
                    if (sessionOpt.isPresent()) {
                        Session session = sessionOpt.get();
                        Current.setSession(session);
                        Current.setUser(session.getUser());
                    }
                    break;
                }
            }
        }
    }

    /**
     * Start a new session for the given user. Creates session record and sets
     * cookie with configured properties (name,
     * path, max age, HTTP-only flag).
     *
     * @param user
     *            the user to create session for
     * @param request
     *            the HTTP request
     * @param response
     *            the HTTP response
     * @return the created session
     */
    public Session startNewSessionFor(User user, HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        Session session = new Session(user, ipAddress, userAgent);
        session = sessionRepository.save(session);

        Cookie cookie = new Cookie(properties.getSession().getCookieName(), session.getToken());
        cookie.setHttpOnly(properties.getSession().isHttpOnly());
        cookie.setPath(properties.getSession().getPath());
        cookie.setMaxAge(properties.getSession().getMaxAge());
        response.addCookie(cookie);

        Current.setSession(session);
        Current.setUser(user);

        return session;
    }

    /**
     * Terminate the current session. Deletes session record from database and
     * removes cookie using the configured
     * cookie settings.
     *
     * @param response
     *            the HTTP response
     */
    public void terminateSession(HttpServletResponse response) {
        Session session = Current.getSession();
        if (session != null) {
            sessionRepository.delete(session);

            Cookie cookie = new Cookie(properties.getSession().getCookieName(), "");
            cookie.setHttpOnly(properties.getSession().isHttpOnly());
            cookie.setPath(properties.getSession().getPath());
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            Current.clear();
        }
    }

    /**
     * Get the real client IP address, considering proxies
     *
     * @param request
     *            the HTTP request
     * @return the client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
