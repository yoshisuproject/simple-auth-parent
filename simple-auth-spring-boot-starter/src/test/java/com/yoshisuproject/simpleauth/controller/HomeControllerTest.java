package com.yoshisuproject.simpleauth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;

/** Unit tests for HomeController */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private Model model;

    @InjectMocks
    private HomeController controller;

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
        Current.clear();
    }

    @Test
    void testIndexWhenAuthenticated() {
        // Set up authentication
        Current.setUser(user);
        Current.setSession(session);

        String view = controller.index(model);

        assertEquals("home/index", view);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("session", session);
    }

    @Test
    void testIndexUsesRequireAuthenticationAnnotation() {
        // Verify that the controller uses @RequireAuthentication annotation
        assertTrue(controller.getClass().isAnnotationPresent(RequireAuthentication.class));
    }
}
