package com.yoshisuproject.simpleauth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.service.AuthenticationService;
import com.yoshisuproject.simpleauth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Unit tests for SessionsController */
@ExtendWith(MockitoExtension.class)
class SessionsControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    private SessionsController controller;
    private SimpleAuthProperties properties;
    private User user;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        properties = new SimpleAuthProperties();
        controller = new SessionsController(authenticationService, userService, properties);

        user = new User();
        user.setId(1L);
        user.setEmailAddress("test@example.com");
        user.setPasswordDigest(passwordEncoder.encode("password123"));
    }

    @Test
    void testNewSession() {
        String view = controller.newSession(model);

        assertEquals("sessions/new", view);
    }

    @Test
    void testCreateWithValidCredentials() {
        Session session = new Session(user, "192.168.1.1", "Mozilla/5.0");
        session.setId(1L);

        when(userService.authenticateUser("test@example.com", "password123")).thenReturn(Optional.of(user));
        when(authenticationService.startNewSessionFor(eq(user), eq(request), eq(response)))
                .thenReturn(session);

        String view = controller.create("test@example.com", "password123", request, response, redirectAttributes);

        assertEquals("redirect:/", view);
        verify(authenticationService).startNewSessionFor(eq(user), eq(request), eq(response));
        verify(redirectAttributes, never()).addFlashAttribute(eq("error"), any());
    }

    @Test
    void testCreateWithInvalidEmail() {
        when(userService.authenticateUser("wrong@example.com", "password123")).thenReturn(Optional.empty());

        String view = controller.create("wrong@example.com", "password123", request, response, redirectAttributes);

        assertEquals("redirect:/session/new", view);
        verify(authenticationService, never()).startNewSessionFor(any(), any(), any());
        verify(redirectAttributes).addFlashAttribute("error", "Invalid email or password");
    }

    @Test
    void testCreateWithInvalidPassword() {
        when(userService.authenticateUser("test@example.com", "wrongpassword")).thenReturn(Optional.empty());

        String view = controller.create("test@example.com", "wrongpassword", request, response, redirectAttributes);

        assertEquals("redirect:/session/new", view);
        verify(authenticationService, never()).startNewSessionFor(any(), any(), any());
        verify(redirectAttributes).addFlashAttribute("error", "Invalid email or password");
    }

    @Test
    void testCreateWithEmptyPassword() {
        when(userService.authenticateUser("test@example.com", "")).thenReturn(Optional.empty());

        String view = controller.create("test@example.com", "", request, response, redirectAttributes);

        assertEquals("redirect:/session/new", view);
        verify(redirectAttributes).addFlashAttribute("error", "Invalid email or password");
    }

    @Test
    void testCreateWithValidCredentialsUsesConfiguredHomeUrl() {
        properties.getUrls().setHomeUrl("/dashboard");

        Session session = new Session(user, "192.168.1.1", "Mozilla/5.0");
        session.setId(2L);

        when(userService.authenticateUser("test@example.com", "password123")).thenReturn(Optional.of(user));
        when(authenticationService.startNewSessionFor(eq(user), eq(request), eq(response)))
                .thenReturn(session);

        String view = controller.create("test@example.com", "password123", request, response, redirectAttributes);

        assertEquals("redirect:/dashboard", view);
    }

    @Test
    void testDestroy() {
        String view = controller.destroy(response);

        assertEquals("redirect:/session/new", view);
        verify(authenticationService).terminateSession(response);
    }

    @Test
    void testDestroyGet() {
        String view = controller.destroyGet(response);

        assertEquals("redirect:/session/new", view);
        verify(authenticationService).terminateSession(response);
    }

    @Test
    void testDestroyRespectsConfiguredLoginUrl() {
        properties.getUrls().setLoginUrl("/sign-in");

        String view = controller.destroy(response);

        assertEquals("redirect:/sign-in", view);
    }

    @Test
    void testPasswordEncoderMatches() {
        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("wrongpassword", encodedPassword));
    }
}
