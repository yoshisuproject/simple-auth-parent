package com.yoshisuproject.simpleauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.model.Current;

/**
 * Home page controller demonstrating authentication requirement.
 *
 * <p>
 * This controller serves as an example of protecting resources using the
 * {@link RequireAuthentication} annotation.
 * When applied at the class level, all methods within this controller require
 * an authenticated user session.
 *
 * <p>
 * If an unauthenticated user attempts to access any route in this controller,
 * they will be redirected to the login
 * page by the
 * {@link com.yoshisuproject.simpleauth.interceptor.AuthenticationInterceptor}.
 *
 * <p>
 * Routes:
 *
 * <ul>
 * <li>GET / - Home page (authentication required)
 * </ul>
 */
@Controller
@RequireAuthentication
public class HomeController {

    /**
     * GET / - Home page (requires authentication via {@code @RequireAuthentication}
     * on class).
     *
     * @param model
     *            the view model to populate
     * @return the home view name
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("user", Current.getUser());
        model.addAttribute("session", Current.getSession());
        return "home/index";
    }
}
