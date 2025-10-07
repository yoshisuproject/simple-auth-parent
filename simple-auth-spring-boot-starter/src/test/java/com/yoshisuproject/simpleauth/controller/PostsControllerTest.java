package com.yoshisuproject.simpleauth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import com.yoshisuproject.simpleauth.annotation.AllowUnauthenticatedAccess;
import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.model.Session;
import com.yoshisuproject.simpleauth.model.User;

/** Unit tests for PostsController */
@ExtendWith(MockitoExtension.class)
class PostsControllerTest {

    @Mock
    private Model model;

    @InjectMocks
    private PostsController controller;

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
    void testIndexAllowsUnauthenticatedAccess() throws NoSuchMethodException {
        String view = controller.index(model);

        assertEquals("posts/index", view);
        verify(model).addAttribute(eq("authenticated"), anyBoolean());
        verify(model).addAttribute(eq("message"), any());

        // Verify annotation
        Method indexMethod = PostsController.class.getMethod("index", Model.class);
        assertTrue(indexMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));
    }

    @Test
    void testIndexWhenAuthenticated() {
        Current.setUser(user);
        Current.setSession(session);

        String view = controller.index(model);

        assertEquals("posts/index", view);
        verify(model).addAttribute("authenticated", true);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("message", "Posts Index - Public Access");
    }

    @Test
    void testIndexWhenNotAuthenticated() {
        String view = controller.index(model);

        assertEquals("posts/index", view);
        verify(model).addAttribute("authenticated", false);
        verify(model, never()).addAttribute(eq("user"), any());
        verify(model).addAttribute("message", "Posts Index - Public Access");
    }

    @Test
    void testShowRequiresAuthentication() throws NoSuchMethodException {
        Current.setUser(user);
        Current.setSession(session);

        String view = controller.show(1L, model);

        assertEquals("posts/show", view);
        verify(model).addAttribute("postId", 1L);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("message", "Post Detail - Requires Authentication");

        // Verify no @AllowUnauthenticatedAccess annotation
        Method showMethod = PostsController.class.getMethod("show", Long.class, Model.class);
        assertFalse(showMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));
    }

    @Test
    void testNewPostRequiresAuthentication() throws NoSuchMethodException {
        Current.setUser(user);
        Current.setSession(session);

        String view = controller.newPost(model);

        assertEquals("posts/new", view);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("message", "Create Post - Requires Authentication");

        // Verify no @AllowUnauthenticatedAccess annotation
        Method newMethod = PostsController.class.getMethod("newPost", Model.class);
        assertFalse(newMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));
    }

    @Test
    void testControllerHasRequireAuthenticationAnnotation() {
        assertTrue(controller.getClass().isAnnotationPresent(RequireAuthentication.class));
    }

    @Test
    void testAnnotationPrecedence() throws NoSuchMethodException {
        // Class has @RequireAuthentication
        assertTrue(PostsController.class.isAnnotationPresent(RequireAuthentication.class));

        // index() has @AllowUnauthenticatedAccess which overrides class-level
        Method indexMethod = PostsController.class.getMethod("index", Model.class);
        assertTrue(indexMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));

        // show() and newPost() inherit class-level @RequireAuthentication
        Method showMethod = PostsController.class.getMethod("show", Long.class, Model.class);
        assertFalse(showMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));

        Method newMethod = PostsController.class.getMethod("newPost", Model.class);
        assertFalse(newMethod.isAnnotationPresent(AllowUnauthenticatedAccess.class));
    }
}
