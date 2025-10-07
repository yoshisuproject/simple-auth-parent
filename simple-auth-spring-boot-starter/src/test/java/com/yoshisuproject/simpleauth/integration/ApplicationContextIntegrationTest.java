package com.yoshisuproject.simpleauth.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.controller.HomeController;
import com.yoshisuproject.simpleauth.controller.PasswordsController;
import com.yoshisuproject.simpleauth.controller.PostsController;
import com.yoshisuproject.simpleauth.controller.SessionsController;
import com.yoshisuproject.simpleauth.repository.SessionRepository;
import com.yoshisuproject.simpleauth.repository.UserRepository;
import com.yoshisuproject.simpleauth.service.PasswordsMailer;

/**
 * Integration test for Spring Boot application context Verifies that all beans
 * are correctly configured
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class ApplicationContextIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void allControllersAreLoaded() {
        assertNotNull(context.getBean(HomeController.class));
        assertNotNull(context.getBean(SessionsController.class));
        assertNotNull(context.getBean(PasswordsController.class));
        assertNotNull(context.getBean(PostsController.class));
    }

    @Test
    void allRepositoriesAreLoaded() {
        assertNotNull(context.getBean(UserRepository.class));
        assertNotNull(context.getBean(SessionRepository.class));
    }

    @Test
    void allServicesAreLoaded() {
        assertNotNull(context.getBean(PasswordsMailer.class));
    }

    @Test
    void mailComponentsAreLoaded() {
        assertNotNull(context.getBean(JavaMailSender.class));
        assertNotNull(context.getBean(SpringTemplateEngine.class));
    }

    @Test
    void repositoriesAreProperlyInjected() {
        UserRepository userRepository = context.getBean(UserRepository.class);
        SessionRepository sessionRepository = context.getBean(SessionRepository.class);

        assertNotNull(userRepository);
        assertNotNull(sessionRepository);

        // Verify they can be used
        assertEquals(0, userRepository.count());
        assertEquals(0, sessionRepository.count());
    }

    @Test
    void controllersHaveProperAnnotations() {
        HomeController homeController = context.getBean(HomeController.class);
        PostsController postsController = context.getBean(PostsController.class);

        // Verify annotation presence
        assertTrue(homeController.getClass().isAnnotationPresent(RequireAuthentication.class));

        assertTrue(postsController.getClass().isAnnotationPresent(RequireAuthentication.class));
    }

    @Test
    void passwordsMailerIsConfigured() {
        PasswordsMailer mailer = context.getBean(PasswordsMailer.class);
        assertNotNull(mailer);
    }

    @Test
    void applicationHasWebMvcConfiguration() {
        // WebConfig should be loaded
        String[] beanNames =
                context.getBeanNamesForType(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class);

        assertTrue(beanNames.length > 0, "WebMvcConfigurer beans should be present");
    }
}
