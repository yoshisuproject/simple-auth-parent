package com.yoshisuproject.simpleauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yoshisuproject.simpleauth.annotation.AllowUnauthenticatedAccess;
import com.yoshisuproject.simpleauth.annotation.RequireAuthentication;
import com.yoshisuproject.simpleauth.model.Current;

/**
 * Posts controller demonstrating flexible annotation-based authentication.
 *
 * <p>
 * This controller illustrates how to mix authenticated and unauthenticated
 * access within the same controller using
 * annotation precedence:
 *
 * <ul>
 * <li>Class-level {@link RequireAuthentication} sets authentication as the
 * default for all methods
 * <li>Method-level {@link AllowUnauthenticatedAccess} selectively allows public
 * access to specific endpoints
 * </ul>
 *
 * <p>
 * This pattern is useful for resources that are mostly protected but have some
 * public-facing views, such as a blog
 * where reading is public but creating/editing requires authentication.
 *
 * <p>
 * Routes:
 *
 * <ul>
 * <li>GET /posts - List posts (public access via method override)
 * <li>GET /posts/{id} - View single post (authentication required)
 * <li>GET /posts/new - New post form (authentication required)
 * </ul>
 */
@Controller
@RequestMapping("/posts")
@RequireAuthentication // All methods require authentication by default
public class PostsController {

    /**
     * GET /posts - List all posts (allows unauthenticated access). Overrides the
     * class-level
     * {@code @RequireAuthentication}.
     *
     * @param model
     *            the view model to populate
     * @return the posts index view name
     */
    @GetMapping
    @AllowUnauthenticatedAccess
    public String index(Model model) {
        boolean authenticated = Current.isAuthenticated();
        model.addAttribute("authenticated", authenticated);
        if (authenticated) {
            model.addAttribute("user", Current.getUser());
        }
        model.addAttribute("message", "Posts Index - Public Access");
        return "posts/index";
    }

    /**
     * GET /posts/{id} - View a single post (requires authentication). Inherits
     * {@code @RequireAuthentication} from
     * class level.
     *
     * @param id
     *            the post ID
     * @param model
     *            the view model to populate
     * @return the post detail view name
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("postId", id);
        model.addAttribute("user", Current.getUser());
        model.addAttribute("message", "Post Detail - Requires Authentication");
        return "posts/show";
    }

    /**
     * GET /posts/new - Create new post form (requires authentication). Inherits
     * {@code @RequireAuthentication} from
     * class level.
     *
     * @param model
     *            the view model to populate
     * @return the new post view name
     */
    @GetMapping("/new")
    public String newPost(Model model) {
        model.addAttribute("user", Current.getUser());
        model.addAttribute("message", "Create Post - Requires Authentication");
        return "posts/new";
    }
}
