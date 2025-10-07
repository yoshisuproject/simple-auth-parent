package com.yoshisuproject.simpleauth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/** Unit tests for PasswordsMailer */
@ExtendWith(MockitoExtension.class)
class PasswordsMailerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private PasswordsMailer passwordsMailer;
    private SimpleAuthProperties properties;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        properties = new SimpleAuthProperties();
        properties.getMail().setFrom("noreply@example.com");
        properties.getMail().setEnabled(true);

        passwordsMailer = new PasswordsMailer(mailSender, templateEngine, properties, "http://localhost:8080");

        user = new User();
        user.setId(1L);
        user.setEmailAddress("test@example.com");

        token = "reset-token-123";
    }

    @Test
    void testSendPasswordResetEmailSuccess() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mailer/reset"), any(Context.class)))
                .thenReturn("<html><body>Reset your password</body></html>");

        passwordsMailer.sendPasswordResetEmail(user, token);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);

        // Verify template engine was called with correct context
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mailer/reset"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertEquals(user, context.getVariable("user"));
        assertEquals("http://localhost:8080/passwords/reset-token-123/edit", context.getVariable("resetUrl"));
        assertEquals(token, context.getVariable("token"));
    }

    @Test
    void testSendPasswordResetEmailWithCustomAppUrl() throws MessagingException {
        PasswordsMailer customMailer = new PasswordsMailer(mailSender, templateEngine, properties, "https://myapp.com");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mailer/reset"), any(Context.class)))
                .thenReturn("<html><body>Reset</body></html>");

        customMailer.sendPasswordResetEmail(user, token);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mailer/reset"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertEquals("https://myapp.com/passwords/reset-token-123/edit", context.getVariable("resetUrl"));
    }

    @Test
    void testSendPasswordResetEmailHandlesMailExceptionsGracefully() {
        when(mailSender.createMimeMessage()).thenThrow(new MailSendException("Mail server unavailable"));

        assertDoesNotThrow(() -> passwordsMailer.sendPasswordResetEmail(user, token));

        verify(mailSender).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmailSkippedWhenMailDisabled() {
        properties.getMail().setEnabled(false);

        passwordsMailer.sendPasswordResetEmail(user, token);

        verifyNoInteractions(mailSender);
        verifyNoInteractions(templateEngine);
    }

    @Test
    void testEmailContentGeneration() throws MessagingException {
        String expectedHtmlContent = "<html><body><h1>Password Reset</h1><p>Click the link</p></body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mailer/reset"), any(Context.class))).thenReturn(expectedHtmlContent);

        passwordsMailer.sendPasswordResetEmail(user, token);

        verify(templateEngine).process(eq("mailer/reset"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testTemplateContextVariables() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mailer/reset"), any(Context.class))).thenReturn("<html>Test</html>");

        passwordsMailer.sendPasswordResetEmail(user, token);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mailer/reset"), contextCaptor.capture());

        Context context = contextCaptor.getValue();

        // Verify all required variables are present
        assertNotNull(context.getVariable("user"));
        assertNotNull(context.getVariable("resetUrl"));
        assertNotNull(context.getVariable("token"));

        // Verify values
        assertEquals(user, context.getVariable("user"));
        assertTrue(((String) context.getVariable("resetUrl")).contains(token));
        assertEquals(token, context.getVariable("token"));
    }

    @Test
    void testResetUrlFormat() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mailer/reset"), any(Context.class))).thenReturn("<html>Test</html>");

        passwordsMailer.sendPasswordResetEmail(user, token);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mailer/reset"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        String resetUrl = (String) context.getVariable("resetUrl");

        // Verify URL format
        assertTrue(resetUrl.startsWith("http://localhost:8080/passwords/"));
        assertTrue(resetUrl.endsWith("/edit"));
        assertTrue(resetUrl.contains(token));
    }
}
