package com.yoshisuproject.simpleauth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Require authentication for controller or method.
 *
 * <p>
 * Can be applied at:
 *
 * <ul>
 * <li><b>Class level</b>: All methods in the controller require authentication
 * <li><b>Method level</b>: Only this method requires authentication
 * </ul>
 *
 * <p>
 * Example:
 *
 * <pre>
 * &#64;Controller
 * @RequireAuthentication
 * public class PostsController {
 *     // All methods require authentication
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuthentication {}
