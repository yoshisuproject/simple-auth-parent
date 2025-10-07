package com.yoshisuproject.simpleauth.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.SessionRepository;
import com.yoshisuproject.simpleauth.repository.UserRepository;

import jakarta.servlet.http.Cookie;

/**
 * Integration tests for Authentication using @SpringBootTest Tests the complete
 * authentication flow with Spring Boot
 * context
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Clean up database
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmailAddress("integration@example.com");
        testUser.setPasswordDigest(passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        Current.clear();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/session/new")).andExpect(status().isOk()).andExpect(view().name("sessions/new"));
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // Verify session cookie was set
        Cookie sessionCookie = result.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        assertEquals("session_token", sessionCookie.getName());
        assertTrue(sessionCookie.isHttpOnly());
        assertEquals("/", sessionCookie.getPath());

        // Verify session was created in database
        String token = sessionCookie.getValue();
        assertTrue(sessionRepository.findByToken(token).isPresent());
    }

    @Test
    void testFailedLoginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andExpect(flash().attribute("error", "Invalid email or password"));

        // Verify no session was created
        assertEquals(0, sessionRepository.count());
    }

    @Test
    void testFailedLoginWithWrongEmail() throws Exception {
        mockMvc.perform(post("/session")
                        .param("emailAddress", "wrong@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andExpect(flash().attribute("error", "Invalid email or password"));

        // Verify no session was created
        assertEquals(0, sessionRepository.count());
    }

    @Test
    void testLogout() throws Exception {
        // First, login to create a session
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");
        assertNotNull(sessionCookie);
        String token = sessionCookie.getValue();

        // Verify session exists
        assertTrue(sessionRepository.findByToken(token).isPresent());

        // Now logout
        MvcResult logoutResult = mockMvc.perform(get("/session/destroy").cookie(sessionCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"))
                .andReturn();

        // Verify session cookie was cleared
        Cookie clearedCookie = logoutResult.getResponse().getCookie("session_token");
        assertNotNull(clearedCookie);
        assertEquals(0, clearedCookie.getMaxAge());

        // Verify session was deleted from database
        assertFalse(sessionRepository.findByToken(token).isPresent());
    }

    @Test
    void testAccessProtectedPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/session/new"));
    }

    @Test
    void testAccessProtectedPageWithLogin() throws Exception {
        // First, login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");

        // Now access protected page
        mockMvc.perform(get("/").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("session"));
    }

    @Test
    void testPublicPostsIndexAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/index"))
                .andExpect(model().attribute("authenticated", false));
    }

    @Test
    void testPublicPostsIndexAccessibleWithLogin() throws Exception {
        // First, login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");

        // Access posts index
        mockMvc.perform(get("/posts").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/index"))
                .andExpect(model().attribute("authenticated", true))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void testProtectedPostDetailRequiresLogin() throws Exception {
        mockMvc.perform(get("/posts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }

    @Test
    void testProtectedPostDetailAccessibleWithLogin() throws Exception {
        // First, login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");

        // Access protected post detail
        mockMvc.perform(get("/posts/1").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("postId", 1L));
    }

    @Test
    void testSessionPersistsAcrossRequests() throws Exception {
        // Login
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "integration@example.com")
                        .param("password", "password123"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("session_token");

        // First request
        mockMvc.perform(get("/").cookie(sessionCookie)).andExpect(status().isOk());

        // Second request with same cookie
        mockMvc.perform(get("/posts/1").cookie(sessionCookie)).andExpect(status().isOk());

        // Verify only one session in database
        assertEquals(1, sessionRepository.count());
    }

    @Test
    void testInvalidSessionTokenRedirects() throws Exception {
        Cookie invalidCookie = new Cookie("session_token", "invalid-token-12345");

        mockMvc.perform(get("/").cookie(invalidCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }
}
