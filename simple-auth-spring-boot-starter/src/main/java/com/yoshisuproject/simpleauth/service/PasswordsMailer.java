package com.yoshisuproject.simpleauth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending password reset emails to users.
 *
 * <p>
 * This service uses Spring's {@link JavaMailSender} to send HTML-formatted
 * password reset emails. Email templates
 * are rendered using Thymeleaf, allowing for customizable and localized email
 * content.
 *
 * <p>
 * Configuration properties:
 *
 * <ul>
 * <li>{@code simple-auth.mail.from} - Sender email address (default:
 * noreply@example.com)
 * <li>{@code simple-auth.mail.enabled} - Master switch for email delivery
 * (default: true)
 * <li>{@code app.url} - Base application URL for reset links (default:
 * http://localhost:8080)
 * </ul>
 *
 * <p>
 * The email template is located at {@code templates/mailer/reset.html} and
 * receives the user object, reset URL, and
 * token as template variables.
 *
 * <p>
 * If email sending fails, the error is logged and processing continues to avoid
 * impacting the user experience.
 */
@Service
public class PasswordsMailer {

    private static final Logger logger = LoggerFactory.getLogger(PasswordsMailer.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final SimpleAuthProperties properties;
    private final String appUrl;

    public PasswordsMailer(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            SimpleAuthProperties properties,
            @Value("${app.url:http://localhost:8080}") String appUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.appUrl = appUrl;
    }

    /**
     * Send password reset email to user
     *
     * @param user
     *            the user requesting password reset
     * @param token
     *            the password reset token
     */
    public void sendPasswordResetEmail(User user, String token) {
        if (!properties.getMail().isEnabled()) {
            logger.debug("Password reset email disabled. Skipping email for {}", user.getEmailAddress());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(user.getEmailAddress());
            helper.setSubject("Reset your password");

            // Prepare template context
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("resetUrl", appUrl + "/passwords/" + token + "/edit");
            context.setVariable("token", token);

            String htmlContent = templateEngine.process("mailer/reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send password reset email to {}: {}", user.getEmailAddress(), e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Failed to send password reset email to {}: {}", user.getEmailAddress(), e.getMessage(), e);
        }
    }
}
