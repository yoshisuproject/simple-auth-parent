# Simple Auth Spring Boot Sample Application

This is a sample Spring Boot application demonstrating how to use the Simple Auth library for authentication.

## Overview

This demo application showcases:
- Session-based authentication with login/logout
- Password reset workflow
- Annotation-based access control (`@RequireAuthentication`, `@AllowUnauthenticatedAccess`)
- Mixed authentication patterns (public and protected endpoints)
- Integration with H2 in-memory database

## Quick Start

### 1. Run the Application

From the project root directory:

```bash
./mvnw spring-boot:run -pl simple-auth-spring-sample
```

Or from this module directory:

```bash
cd simple-auth-spring-sample
../mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 2. Test Account

A test user is automatically created on startup:

- **Email**: `test@example.com`
- **Password**: `password123`

See `src/main/resources/data.sql` for details.

## Available Endpoints

### Public Endpoints (No Authentication Required)

| URL | Description | Notes |
|-----|-------------|-------|
| `GET /session/new` | Login page | Provided by Simple Auth |
| `POST /session` | Login action | Submit email and password |
| `GET /passwords/new` | Password reset request | Provided by Simple Auth |
| `POST /passwords` | Send reset email | Provided by Simple Auth |
| `GET /passwords/{token}/edit` | Password reset form | Provided by Simple Auth |
| `GET /posts` | Public posts index | Shows different content for authenticated/unauthenticated users |
| `GET /h2-console` | H2 Database Console | For development only |

### Protected Endpoints (Authentication Required)

| URL | Description | Notes |
|-----|-------------|-------|
| `GET /` | Home page | Displays current user and session info |
| `GET /posts/{id}` | Post detail page | Example: `/posts/1` |
| `GET /posts/new` | New post form | Example protected form |
| `DELETE /session` | Logout | Simple Auth logout endpoint |
| `GET /session/destroy` | Logout (fallback) | For browsers without DELETE support |

## Testing Authentication

### Test Flow 1: Login and Access Protected Pages

1. Navigate to `http://localhost:8080/`
2. You'll be redirected to `/session/new` (login page)
3. Enter credentials:
   - Email: `test@example.com`
   - Password: `password123`
4. After login, you'll be redirected back to `/`
5. Try accessing:
   - `/posts/1` - Should work (authenticated)
   - `/posts/new` - Should work (authenticated)

### Test Flow 2: Mixed Authentication Pattern

1. Navigate to `http://localhost:8080/posts` (no login required)
2. Notice the page shows you're not logged in
3. Login using the credentials above
4. Navigate to `/posts` again
5. Now the page shows you're logged in with user details

### Test Flow 3: Password Reset

1. Navigate to `http://localhost:8080/passwords/new`
2. Enter email: `test@example.com`
3. Check the console logs for the password reset email (if mail server is not configured)
4. Use the token from the logs to access `/passwords/{token}/edit`
5. Enter a new password and confirm it
6. Login with the new password

## Configuration

### Database (H2)

The sample uses H2 in-memory database with file persistence:

```properties
spring.datasource.url=jdbc:h2:file:./data/demo-db
```

Database files are stored in `./data/demo-db.*`

#### Accessing H2 Console

1. Navigate to `http://localhost:8080/h2-console`
2. Use these settings:
   - **JDBC URL**: `jdbc:h2:file:./data/demo-db`
   - **Username**: `sa`
   - **Password**: (leave empty)
3. Click "Connect"

You can view the tables:
- `users` - User accounts
- `sessions` - Active sessions
- `password_reset_tokens` - Password reset tokens

### Mail Configuration

For password reset emails, the sample is configured to use a local SMTP server:

```properties
spring.mail.host=localhost
spring.mail.port=1025
```

#### Option 1: Use MailDev (Recommended for Development)

Install and run MailDev to capture emails:

```bash
# Install MailDev (requires Node.js)
npm install -g maildev

# Run MailDev
maildev
```

Then access the web interface at `http://localhost:1080` to view emails.

#### Option 2: Use Real SMTP Server

Update `application.properties` with your SMTP settings:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### Option 3: Disable Email

Set `simple-auth.mail.enabled=false` in `application.properties`.

## Application Structure

```
simple-auth-spring-sample/
├── src/main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java           # Main application
│   │   └── controller/
│   │       ├── HomeController.java        # Protected home page
│   │       └── PostsController.java       # Mixed auth pattern
│   └── resources/
│       ├── application.properties         # Configuration
│       └── data.sql                       # Test data
└── pom.xml
```

## Key Features Demonstrated

### 1. Class-Level Authentication

`HomeController.java` requires authentication for all methods:

```java
@Controller
@RequireAuthentication  // All methods require authentication
public class HomeController {
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("user", Current.getUser());
        return "home/index";
    }
}
```

### 2. Mixed Authentication Pattern

`PostsController.java` demonstrates mixed access:

```java
@Controller
@RequestMapping("/posts")
@RequireAuthentication  // Default: require authentication
public class PostsController {

    @GetMapping
    @AllowUnauthenticatedAccess  // Override: public access
    public String index(Model model) {
        // Show different content based on authentication status
        model.addAttribute("authenticated", Current.getUser() != null);
        return "posts/index";
    }

    @GetMapping("/{id}")  // Protected - requires authentication
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("postId", id);
        return "posts/show";
    }
}
```

### 3. Accessing Current User

```java
User currentUser = Current.getUser();
Session currentSession = Current.getSession();
boolean authenticated = Current.isAuthenticated();
```

## View Templates

View templates are provided by the Simple Auth starter and located in:

```
simple-auth-spring-boot-starter/src/main/resources/templates/
├── sessions/
│   └── new.html          # Login form
├── passwords/
│   ├── new.html          # Password reset request
│   └── edit.html         # Password reset form
├── home/
│   └── index.html        # Home page
└── posts/
    ├── index.html        # Posts index
    ├── show.html         # Post detail
    └── new.html          # New post form
```

## Customization Examples

### Adding User Registration

Simple Auth doesn't include user registration by design. Here's how to add it:

```java
@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public String register(@RequestParam String email,
                          @RequestParam String password) {
        User user = new User();
        user.setEmailAddress(email);
        user.setPasswordDigest(passwordEncoder.encode(password));
        userRepository.save(user);

        return "redirect:/session/new";
    }
}
```

### Custom User Service

Extend the default `UserService` to add custom logic:

```java
@Service
@Primary
public class CustomUserService extends UserService {

    public CustomUserService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
    }

    @Override
    public Optional<User> authenticateUser(String email, String password) {
        // Add custom logic (e.g., account lockout, logging)
        return super.authenticateUser(email, password);
    }
}
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `application.properties`:

```properties
server.port=8081
```

### Database Locked

If you see "Database may be already in use", stop any other instances of the application.

### Email Not Sent

Check that:
1. MailDev (or your SMTP server) is running
2. Port 1025 is not blocked
3. `simple-auth.mail.enabled=true` in configuration

### Cannot Access H2 Console

Make sure:
1. `spring.h2.console.enabled=true`
2. You're using the correct JDBC URL: `jdbc:h2:file:./data/demo-db`

## Running Tests

```bash
./mvnw test -pl simple-auth-spring-sample
```

## Next Steps

1. Review the source code to understand the authentication patterns
2. Read the [Annotation Guide](../ANNOTATIONS_GUIDE.md) for more details
3. Check the [Feature Comparison](../FEATURE_COMPARISON.md) with Rails
4. Explore the Simple Auth starter source code
5. Build your own application using this as a template

## Additional Resources

- [Main README](../README.md) - Project overview
- [Annotations Guide](../ANNOTATIONS_GUIDE.md) - Detailed annotation usage
- [Feature Comparison](../FEATURE_COMPARISON.md) - Comparison with Rails 8

## License

MIT License
