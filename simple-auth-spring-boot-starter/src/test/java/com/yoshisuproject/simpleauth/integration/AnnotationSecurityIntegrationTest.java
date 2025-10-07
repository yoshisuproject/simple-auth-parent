package com.yoshisuproject.simpleauth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.repository.SessionRepository;
import com.yoshisuproject.simpleauth.repository.UserRepository;

import jakarta.servlet.http.Cookie;

/**
 * Integration tests for annotation-based security Tests @RequireAuthentication
 * and @AllowUnauthenticatedAccess
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class AnnotationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        passwordEncoder = new BCryptPasswordEncoder();

        // Clean up
        Current.clear();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmailAddress("annotation@example.com");
        testUser.setPasswordDigest(passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
    }

    private Cookie getSessionCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/session")
                        .param("emailAddress", "annotation@example.com")
                        .param("password", "password123"))
                .andReturn();
        return loginResult.getResponse().getCookie("session_token");
    }

    @AfterEach
    void tearDown() {
        Current.clear();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testClassLevelRequireAuthenticationOnHomeController() throws Exception {
        // HomeController has @RequireAuthentication at class level
        // Without login, should redirect
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/session/new"));

        // With login, should work
        Cookie sessionCookie = getSessionCookie();
        mockMvc.perform(get("/").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"));
    }

    @Test
    void testClassLevelRequireAuthenticationOnPostsController() throws Exception {
        // PostsController has @RequireAuthentication at class level
        // But /posts has @AllowUnauthenticatedAccess override
        mockMvc.perform(get("/posts")).andExpect(status().isOk()).andExpect(view().name("posts/index"));

        // /posts/{id} should require authentication (no override)
        mockMvc.perform(get("/posts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));

        Cookie sessionCookie = getSessionCookie();
        mockMvc.perform(get("/posts/1").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/show"));
    }

    @Test
    void testAllowUnauthenticatedAccessOverridesClassLevel() throws Exception {
        // /posts index has @AllowUnauthenticatedAccess
        // Should be accessible without login
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/index"))
                .andExpect(model().attribute("authenticated", false));

        // Should also be accessible with login
        Cookie sessionCookie = getSessionCookie();
        mockMvc.perform(get("/posts").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/index"))
                .andExpect(model().attribute("authenticated", true));
    }

    @Test
    void testMethodLevelProtectionInheritsFromClass() throws Exception {
        // /posts/new should inherit @RequireAuthentication from class
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));

        Cookie sessionCookie = getSessionCookie();
        mockMvc.perform(get("/posts/new").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/new"));
    }

    @Test
    void testMultipleProtectedEndpointsWithSameSession() throws Exception {
        Cookie sessionCookie = getSessionCookie();

        // Test that session works across multiple protected endpoints
        mockMvc.perform(get("/").cookie(sessionCookie)).andExpect(status().isOk());

        mockMvc.perform(get("/posts/1").cookie(sessionCookie)).andExpect(status().isOk());

        mockMvc.perform(get("/posts/new").cookie(sessionCookie)).andExpect(status().isOk());
    }

    @Test
    void testPublicEndpointDoesNotRequireSession() throws Exception {
        // /posts is public, should work without any cookie
        mockMvc.perform(get("/posts")).andExpect(status().isOk()).andExpect(model().attribute("authenticated", false));
    }

    @Test
    void testInvalidSessionRedirectsToLogin() throws Exception {
        Cookie invalidCookie = new Cookie("session_token", "invalid-token");

        mockMvc.perform(get("/").cookie(invalidCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/session/new"));
    }
}
