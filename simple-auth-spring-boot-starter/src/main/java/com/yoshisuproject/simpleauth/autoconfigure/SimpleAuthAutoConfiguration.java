package com.yoshisuproject.simpleauth.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.yoshisuproject.simpleauth.controller.SessionsController;
import com.yoshisuproject.simpleauth.interceptor.AuthenticationInterceptor;
import com.yoshisuproject.simpleauth.repository.UserRepository;
import com.yoshisuproject.simpleauth.service.AuthenticationService;

/**
 * Spring Boot auto-configuration for Simple Auth authentication system.
 *
 * <p>
 * This is the main entry point for the Simple Auth starter. It automatically
 * configures all necessary components for
 * the authentication system when the starter is included in a Spring Boot
 * application.
 *
 * <p>
 * Auto-configuration activation conditions:
 *
 * <ul>
 * <li>{@link AuthenticationInterceptor} is present on the classpath
 * <li>Property {@code simple-auth.enabled} is not explicitly set to
 * {@code false}
 * </ul>
 *
 * <p>
 * Components automatically configured:
 *
 * <ul>
 * <li>Controllers: Login, logout, password reset, and example protected
 * resources
 * <li>Repositories: JDBC-based data access for users, sessions, and password
 * reset tokens
 * <li>Services: Authentication, user management, email sending, and token
 * cleanup
 * <li>Interceptor: Request interception for authentication enforcement
 * <li>Password encoder: BCrypt-based password hashing (if not already provided)
 * <li>Scheduled tasks: Automatic cleanup of expired password reset tokens
 * </ul>
 *
 * <p>
 * The configuration also:
 *
 * <ul>
 * <li>Registers the authentication interceptor for all URLs except those in
 * excluded patterns
 * <li>Enables Spring's scheduling support for background tasks
 * <li>Binds configuration properties from the {@code simple-auth} prefix
 * </ul>
 *
 * <p>
 * To disable auto-configuration, set {@code simple-auth.enabled=false} in
 * application properties.
 *
 * @see SimpleAuthProperties
 * @see AuthenticationInterceptor
 */
@AutoConfiguration
@ConditionalOnClass(AuthenticationInterceptor.class)
@ConditionalOnProperty(prefix = "simple-auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SimpleAuthProperties.class)
@EnableScheduling
@ComponentScan(
        basePackageClasses = {
            SessionsController.class,
            UserRepository.class,
            AuthenticationService.class,
            AuthenticationInterceptor.class
        })
public class SimpleAuthAutoConfiguration implements WebMvcConfigurer {

    private final SimpleAuthProperties properties;

    private final AuthenticationInterceptor authenticationInterceptor;

    /**
     * Creates the auto-configuration with required dependencies.
     *
     * @param properties
     *            the Simple Auth configuration properties
     * @param authenticationInterceptor
     *            the authentication interceptor
     */
    public SimpleAuthAutoConfiguration(
            SimpleAuthProperties properties, AuthenticationInterceptor authenticationInterceptor) {
        this.properties = properties;
        this.authenticationInterceptor = authenticationInterceptor;
    }

    /**
     * Provides a BCrypt password encoder bean if none is already defined. This
     * allows applications to override with
     * their own encoder if needed.
     *
     * @return a BCrypt password encoder
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Registers the authentication interceptor for all URLs except excluded
     * patterns. The excluded patterns are
     * configured via {@code simple-auth.urls.excluded-patterns}.
     *
     * @param registry
     *            the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(properties.getUrls().getExcludedPatterns());
    }
}
