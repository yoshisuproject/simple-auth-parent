package com.yoshisuproject.simpleauth.interceptor;

import java.lang.reflect.Method;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.yoshisuproject.simpleauth.annotation.AllowUnauthenticatedAccess;
import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.model.Current;
import com.yoshisuproject.simpleauth.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring MVC interceptor that enforces authentication requirements for
 * controller methods.
 *
 * <p>
 * This interceptor is automatically registered for all URLs (except those in
 * excluded patterns) and performs the
 * following operations on each request:
 *
 * <p>
 * <b>Before request handling (preHandle):</b>
 *
 * <ol>
 * <li>Attempts to resume an existing session from the session cookie
 * <li>Checks if the target controller method requires authentication via
 * annotations
 * <li>If authentication is required but missing, redirects to login page
 * <li>Otherwise, allows the request to proceed
 * </ol>
 *
 * <p>
 * <b>After request completion (afterCompletion):</b>
 *
 * <ul>
 * <li>Clears thread-local storage ({@link Current}) to prevent memory leaks
 * </ul>
 *
 * <p>
 * <b>Annotation precedence (highest to lowest):</b>
 *
 * <ol>
 * <li>Method-level {@link AllowUnauthenticatedAccess} - Always allows access
 * <li>Method-level {@link RequireAuthentication} - Requires authentication
 * <li>Class-level {@link RequireAuthentication} - Requires authentication for
 * all methods
 * </ol>
 *
 * <p>
 * If no authentication annotations are present, the request is allowed to
 * proceed by default.
 *
 * @see RequireAuthentication
 * @see AllowUnauthenticatedAccess
 * @see AuthenticationService
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AuthenticationService authenticationService;

    public AuthenticationInterceptor(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Called before each request Automatically resumes session if exists and checks
     * authentication annotations
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Always resume session first
        authenticationService.resumeSession(request);

        // Only check annotations for controller methods
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        // Check if method has @AllowUnauthenticatedAccess - highest priority
        if (method.isAnnotationPresent(AllowUnauthenticatedAccess.class)) {
            return true;
        }

        // Check if method or class has @RequireAuthentication
        boolean requiresAuth = method.isAnnotationPresent(RequireAuthentication.class)
                || controllerClass.isAnnotationPresent(RequireAuthentication.class);

        if (requiresAuth && !authenticationService.authenticated()) {
            authenticationService.requireAuthentication(response);
            return false;
        }

        return true;
    }

    /**
     * Called after request completion Clears thread-local storage to prevent memory
     * leaks
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        Current.clear();
    }
}
