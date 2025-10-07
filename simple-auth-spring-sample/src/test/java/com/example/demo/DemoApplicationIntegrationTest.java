package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

/**
 * Integration tests for the demo application authentication flows. Uses
 * application-test.properties for configuration
 * with Flyway auto-detection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class DemoApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data first
        jdbcTemplate.update("DELETE FROM sessions");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM users");

        // Create test user
        String hashedPassword = passwordEncoder.encode("password123");
        jdbcTemplate.update(
                "INSERT INTO users (email_address, password_digest, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "test@example.com",
                hashedPassword);
    }

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("Unauthenticated access to home page redirects to login")
    void unauthenticatedAccessToHomeRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/session/new"));
    }

    @Test
    @DisplayName("Unauthenticated access to post detail redirects to login")
    void unauthenticatedAccessToPostDetailRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/posts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }

    @Test
    @DisplayName("Unauthenticated access to new post redirects to login")
    void unauthenticatedAccessToNewPostRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }

    @Test
    @DisplayName("Unauthenticated access to posts index is allowed")
    void unauthenticatedAccessToPostsIndexIsAllowed() throws Exception {
        mockMvc.perform(get("/posts")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login page is accessible without authentication")
    void loginPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/session/new")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Full authentication flow: login and access protected pages")
    void fullAuthenticationFlow() throws Exception {
        // Step 1: Attempt to access home page without authentication
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/session/new"));

        // Step 2: Login with valid credentials
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // Extract session cookie
        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getValue()).isNotEmpty();

        // Step 3: Access home page with valid session
        mockMvc.perform(get("/").cookie(sessionCookie)).andExpect(status().isOk());

        // Step 4: Access protected post detail with valid session
        mockMvc.perform(get("/posts/1").cookie(sessionCookie)).andExpect(status().isOk());

        // Step 5: Access new post form with valid session
        mockMvc.perform(get("/posts/new").cookie(sessionCookie)).andExpect(status().isOk());

        // Step 6: Access public posts index with valid session
        mockMvc.perform(get("/posts").cookie(sessionCookie)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login with invalid credentials fails")
    void loginWithInvalidCredentialsFails() throws Exception {
        MvcResult result = mockMvc.perform(post("/session")
                        .param("emailAddress", "test@example.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andReturn();

        // Session cookie should not be set
        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertThat(sessionCookie).isNull();
    }

    @Test
    @DisplayName("Logout removes session and redirects to login")
    void logoutRemovesSessionAndRedirectsToLogin() throws Exception {
        // Step 1: Login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");
        assertThat(sessionCookie).isNotNull();

        // Step 2: Access home page successfully
        mockMvc.perform(get("/").cookie(sessionCookie)).andExpect(status().isOk());

        // Step 3: Logout (using GET fallback)
        MvcResult logoutResult = mockMvc.perform(get("/session/destroy").cookie(sessionCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andReturn();

        // Session cookie should be deleted (max age = 0)
        Cookie deletedCookie = logoutResult.getResponse().getCookie("session_token");
        assertThat(deletedCookie).isNotNull();
        assertThat(deletedCookie.getMaxAge()).isZero();

        // Step 4: Attempt to access home page with deleted session
        mockMvc.perform(get("/").cookie(sessionCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }

    @Test
    @DisplayName("Mixed authentication pattern on posts controller works correctly")
    void mixedAuthenticationPatternWorksCorrectly() throws Exception {
        // Public endpoint - accessible without authentication
        mockMvc.perform(get("/posts")).andExpect(status().isOk());

        // Protected endpoints - require authentication
        mockMvc.perform(get("/posts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));

        mockMvc.perform(get("/posts/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));

        // Login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");

        // All endpoints accessible after login
        mockMvc.perform(get("/posts").cookie(sessionCookie)).andExpect(status().isOk());
        mockMvc.perform(get("/posts/1").cookie(sessionCookie)).andExpect(status().isOk());
        mockMvc.perform(get("/posts/new").cookie(sessionCookie)).andExpect(status().isOk());
    }
}
