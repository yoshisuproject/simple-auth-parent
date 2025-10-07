package com.yoshisuproject.simpleauth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly allow unauthenticated access to a controller method.
 *
 * <p>
 * This annotation provides a way to selectively exempt specific methods from
 * authentication requirements within a
 * controller that otherwise requires authentication via
 * {@link RequireAuthentication}.
 *
 * <p>
 * This is particularly useful when you have a controller where most endpoints
 * require authentication, but a few need
 * to be publicly accessible (e.g., viewing public posts vs. creating posts).
 *
 * <p>
 * <b>Important notes:</b>
 *
 * <ul>
 * <li>Can only be applied at the method level, not at the class level
 * <li>Has highest priority - always allows access regardless of class-level
 * annotations
 * <li>Overrides {@link RequireAuthentication} at both method and class levels
 * <li>The {@link com.yoshisuproject.simpleauth.model.Current} context may be
 * populated if a valid session cookie is
 * present, allowing methods to behave differently for authenticated users
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;Controller
 * &#64;RequireAuthentication // All methods require authentication by default
 * public class PostsController {
 *
 *     &#64;GetMapping("/posts")
 *     @AllowUnauthenticatedAccess // Override: this method is public
 *     public String index(Model model) {
 *         // Anyone can access this
 *         if (Current.isAuthenticated()) {
 *             // Show personalized view for logged-in users
 *         }
 *         return "posts/index";
 *     }
 *
 *     &#64;GetMapping("/posts/{id}")
 *     public String show(@PathVariable Long id) {
 *         // This method requires authentication (inherited from class)
 *         return "posts/show";
 *     }
 * }
 * </pre>
 *
 * @see RequireAuthentication
 * @see com.yoshisuproject.simpleauth.interceptor.AuthenticationInterceptor
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowUnauthenticatedAccess {}
