package com.yoshisuproject.simpleauth.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yoshisuproject.simpleauth.autoconfigure.SimpleAuthProperties;
import com.yoshisuproject.simpleauth.model.User;
import com.yoshisuproject.simpleauth.service.AuthenticationService;
import com.yoshisuproject.simpleauth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SessionsController handles user login and logout.
 *
 * <p>
 * Routes:
 *
 * <ul>
 * <li>GET /session/new - Show login form
 * <li>POST /session - Create session (login)
 * <li>DELETE /session - Destroy session (logout)
 * <li>GET /session/destroy - Logout fallback for browsers without DELETE
 * support
 * </ul>
 */
@Controller
@RequestMapping("/session")
public class SessionsController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final SimpleAuthProperties properties;

    public SessionsController(
            AuthenticationService authenticationService, UserService userService, SimpleAuthProperties properties) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.properties = properties;
    }

    /**
     * GET /session/new - Show login form.
     *
     * @param model
     *            the view model
     * @return the login view name
     */
    @GetMapping("/new")
    public String newSession(Model model) {
        return "sessions/new";
    }

    /**
     * POST /session - Create session (login).
     *
     * @param emailAddress
     *            user's email address
     * @param password
     *            plain text password
     * @param request
     *            the HTTP request (used to derive client IP and user agent)
     * @param response
     *            the HTTP response (used to set session cookie)
     * @param redirectAttributes
     *            flash attributes for feedback messages
     * @return redirect to home on success or back to login on failure
     */
    @PostMapping
    public String create(
            @RequestParam("emailAddress") String emailAddress,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userService.authenticateUser(emailAddress, password);

        if (userOpt.isPresent()) {
            // Authentication successful
            authenticationService.startNewSessionFor(userOpt.get(), request, response);
            return "redirect:" + properties.getUrls().getHomeUrl();
        }

        // Authentication failed
        redirectAttributes.addFlashAttribute("error", "Invalid email or password");
        return "redirect:" + properties.getUrls().getLoginUrl();
    }

    /**
     * DELETE /session - Destroy session (logout).
     *
     * @param response
     *            the HTTP response (used to clear session cookie)
     * @return redirect to login page
     */
    @DeleteMapping
    public String destroy(HttpServletResponse response) {
        authenticationService.terminateSession(response);
        return "redirect:" + properties.getUrls().getLoginUrl();
    }

    /**
     * GET /session/destroy - Logout fallback for browsers without DELETE support.
     *
     * @param response
     *            the HTTP response (used to clear session cookie)
     * @return redirect to login page
     */
    @GetMapping("/destroy")
    public String destroyGet(HttpServletResponse response) {
        return destroy(response);
    }
}
