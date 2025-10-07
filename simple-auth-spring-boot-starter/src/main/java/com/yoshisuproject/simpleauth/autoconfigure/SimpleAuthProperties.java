package com.yoshisuproject.simpleauth.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Simple Auth.
 *
 * <p>
 * This class binds all configuration properties with the {@code simple-auth}
 * prefix from application.properties or
 * application.yml. It provides centralized configuration for all aspects of the
 * authentication system.
 *
 * <p>
 * Example configuration in application.yml:
 *
 * <pre>
 * simple-auth:
 *   enabled: true
 *   session:
 *     cookie-name: session_token
 *     max-age: 2592000  # 30 days in seconds
 *   urls:
 *     login-url: /session/new
 *     excluded-patterns:
 *       - /session/**
 *       - /passwords/**
 *   password-reset:
 *     token-expiry: 900  # 15 minutes in seconds
 * </pre>
 *
 * <p>
 * All nested configuration classes are accessible via getters and support full
 * IDE auto-completion when
 * spring-boot-configuration-processor is present.
 *
 * @see SimpleAuthAutoConfiguration
 */
@ConfigurationProperties(prefix = "simple-auth")
public class SimpleAuthProperties {

    /** Enable or disable simple auth */
    private boolean enabled = true;

    /** Session configuration */
    private Session session = new Session();

    /** URL configuration */
    private Urls urls = new Urls();

    /** Mail configuration */
    private Mail mail = new Mail();

    /** Password reset configuration */
    private PasswordReset passwordReset = new PasswordReset();

    /**
     * Checks if Simple Auth is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether Simple Auth is enabled.
     *
     * @param enabled
     *            true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the session configuration.
     *
     * @return the session configuration
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the session configuration.
     *
     * @param session
     *            the session configuration
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Gets the URL configuration.
     *
     * @return the URL configuration
     */
    public Urls getUrls() {
        return urls;
    }

    /**
     * Sets the URL configuration.
     *
     * @param urls
     *            the URL configuration
     */
    public void setUrls(Urls urls) {
        this.urls = urls;
    }

    /**
     * Gets the email configuration.
     *
     * @return the email configuration
     */
    public Mail getMail() {
        return mail;
    }

    /**
     * Sets the email configuration.
     *
     * @param mail
     *            the email configuration
     */
    public void setMail(Mail mail) {
        this.mail = mail;
    }

    /**
     * Gets the password reset configuration.
     *
     * @return the password reset configuration
     */
    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    /**
     * Sets the password reset configuration.
     *
     * @param passwordReset
     *            the password reset configuration
     */
    public void setPasswordReset(PasswordReset passwordReset) {
        this.passwordReset = passwordReset;
    }

    /** Session-related configuration properties. */
    public static class Session {
        /** Session cookie name */
        private String cookieName = "session_token";

        /** Session max age in seconds (default 30 days) */
        private int maxAge = 30 * 24 * 60 * 60;

        /** Session cookie HTTP only flag */
        private boolean httpOnly = true;

        /** Session cookie path */
        private String path = "/";

        /**
         * Gets the session cookie name.
         *
         * @return the cookie name
         */
        public String getCookieName() {
            return cookieName;
        }

        /**
         * Sets the session cookie name.
         *
         * @param cookieName
         *            the cookie name
         */
        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        /**
         * Gets the session max age in seconds.
         *
         * @return the max age in seconds
         */
        public int getMaxAge() {
            return maxAge;
        }

        /**
         * Sets the session max age in seconds.
         *
         * @param maxAge
         *            the max age in seconds
         */
        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }

        /**
         * Checks if the session cookie should be HTTP only.
         *
         * @return true if HTTP only, false otherwise
         */
        public boolean isHttpOnly() {
            return httpOnly;
        }

        /**
         * Sets whether the session cookie should be HTTP only.
         *
         * @param httpOnly
         *            true for HTTP only, false otherwise
         */
        public void setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
        }

        /**
         * Gets the session cookie path.
         *
         * @return the cookie path
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets the session cookie path.
         *
         * @param path
         *            the cookie path
         */
        public void setPath(String path) {
            this.path = path;
        }
    }

    /** URL configuration properties for authentication flows. */
    public static class Urls {
        /** Login page URL */
        private String loginUrl = "/session/new";

        /** Logout URL */
        private String logoutUrl = "/session/destroy";

        /** Home URL (redirect after login) */
        private String homeUrl = "/";

        /** Excluded URL patterns (comma separated) */
        private String[] excludedPatterns = {"/session/new", "/session", "/passwords/**", "/h2-console/**", "/error"};

        /**
         * Gets the login page URL.
         *
         * @return the login URL
         */
        public String getLoginUrl() {
            return loginUrl;
        }

        /**
         * Sets the login page URL.
         *
         * @param loginUrl
         *            the login URL
         */
        public void setLoginUrl(String loginUrl) {
            this.loginUrl = loginUrl;
        }

        /**
         * Gets the logout URL.
         *
         * @return the logout URL
         */
        public String getLogoutUrl() {
            return logoutUrl;
        }

        /**
         * Sets the logout URL.
         *
         * @param logoutUrl
         *            the logout URL
         */
        public void setLogoutUrl(String logoutUrl) {
            this.logoutUrl = logoutUrl;
        }

        /**
         * Gets the home URL (redirect destination after successful login).
         *
         * @return the home URL
         */
        public String getHomeUrl() {
            return homeUrl;
        }

        /**
         * Sets the home URL (redirect destination after successful login).
         *
         * @param homeUrl
         *            the home URL
         */
        public void setHomeUrl(String homeUrl) {
            this.homeUrl = homeUrl;
        }

        /**
         * Gets the URL patterns excluded from authentication checks.
         *
         * @return array of URL patterns
         */
        public String[] getExcludedPatterns() {
            return excludedPatterns;
        }

        /**
         * Sets the URL patterns excluded from authentication checks. Supports Ant-style
         * path patterns (e.g., /api/**,
         * *.css).
         *
         * @param excludedPatterns
         *            array of URL patterns
         */
        public void setExcludedPatterns(String[] excludedPatterns) {
            this.excludedPatterns = excludedPatterns;
        }
    }

    /** Email configuration properties. */
    public static class Mail {
        /** Enable or disable email functionality */
        private boolean enabled = true;

        /** From email address */
        private String from = "noreply@example.com";

        /**
         * Checks if email functionality is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether email functionality is enabled.
         *
         * @param enabled
         *            true to enable, false to disable
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the from email address.
         *
         * @return the from email address
         */
        public String getFrom() {
            return from;
        }

        /**
         * Sets the from email address.
         *
         * @param from
         *            the from email address
         */
        public void setFrom(String from) {
            this.from = from;
        }
    }

    /** Password reset configuration properties. */
    public static class PasswordReset {
        /** Enable or disable password reset functionality */
        private boolean enabled = true;

        /** Password reset token expiry in seconds (default 15 minutes) */
        private int tokenExpiry = 900;

        /**
         * Checks if password reset functionality is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether password reset functionality is enabled.
         *
         * @param enabled
         *            true to enable, false to disable
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the password reset token expiry time in seconds.
         *
         * @return the token expiry in seconds
         */
        public int getTokenExpiry() {
            return tokenExpiry;
        }

        /**
         * Sets the password reset token expiry time in seconds.
         *
         * @param tokenExpiry
         *            the token expiry in seconds
         */
        public void setTokenExpiry(int tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
        }
    }
}
