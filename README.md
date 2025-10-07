# Spring Simple Auth

A lightweight, Rails-inspired authentication library for Spring Boot applications. Provides session-based authentication, password reset functionality, and annotation-based access control out of the box.

## Features

-   **Session-based Authentication** - Secure cookie-based sessions with IP and User-Agent tracking
-   **Password Reset** - Complete password reset workflow with email notifications
-   **Secure Token Storage** - Password reset tokens use selector + BCrypt hashed verifier pattern for database security
-   **Annotation-based Security** - Simple `@RequireAuthentication` and `@AllowUnauthenticatedAccess` annotations
-   **Multi-database Support** - H2, MySQL, PostgreSQL, SQL Server, and more
-   **Spring Boot Starter** - Auto-configuration with sensible defaults
-   **Production Ready** - BCrypt password hashing, SQL injection protection, comprehensive test coverage
-   **Extensible** - Service layer architecture for easy customization

## Philosophy

This project follows Rails 8's philosophy: provide a solid authentication foundation that you own and can customize, rather than a complex framework that hides implementation details.

## Quick Start

### 1. Add Dependency

Maven:

```xml
<dependency>
    <groupId>com.yoshisuproject</groupId>
    <artifactId>simple-auth-spring-boot-starter</artifactId>
    <version>3.5.7-SNAPSHOT</version>
</dependency>
```

Gradle:

```gradle
implementation 'com.yoshisuproject:simple-auth-spring-boot-starter:3.5.7-SNAPSHOT'
```

### 2. Configure Database

Add to `application.properties`:

```properties
# Database configuration (H2 example)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Simple Auth configuration
simple-auth.enabled=true
```

### 3. Add Views

Create Thymeleaf templates:

-   `src/main/resources/templates/sessions/new.html` - Login form
-   `src/main/resources/templates/passwords/new.html` - Password reset request
-   `src/main/resources/templates/passwords/edit.html` - Password reset form

### 4. Protect Your Endpoints

```java
@Controller
@RequireAuthentication  // Require authentication for all endpoints
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard() {
        User currentUser = Current.getUser();
        return "dashboard";
    }

    @GetMapping("/public")
    @AllowUnauthenticatedAccess  // Override for specific endpoints
    public String publicPage() {
        return "public";
    }
}
```

### 5. Access Current User

```java
// Get current authenticated user
User user = Current.getUser();

// Get current session
Session session = Current.getSession();

// Check if authenticated
boolean authenticated = Current.isAuthenticated();
```

## Configuration

### Application Properties

```properties
# Enable/disable Simple Auth (default: true)
simple-auth.enabled=true

# Password reset token expiry in seconds (default: 900 = 15 minutes)
simple-auth.password-reset.token-expiry=900

# URL patterns to exclude from authentication (default: ["/error"])
simple-auth.urls.excluded-patterns=/error,/static/**,/public/**

# Enable email functionality (default: true)
simple-auth.mail.enabled=true
```

### Mail Configuration

For password reset emails:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Architecture

Spring Simple Auth follows a clean layered architecture:

```
┌─────────────────────────────────────────┐
│        Controllers                      │
│  (SessionsController,                  │
│   PasswordsController)                 │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│        Services                         │
│  (AuthenticationService,                │
│   UserService, PasswordsMailer)         │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│        Repositories                     │
│  (UserRepository, SessionRepository,    │
│   PasswordResetTokenRepository)         │
└─────────────────────────────────────────┘
```

### Key Components

-   **Controllers**: Handle HTTP requests (login, logout, password reset)
-   **Services**: Business logic and transaction management
-   **Repositories**: Data access layer with JDBC implementation
-   **Interceptor**: `AuthenticationInterceptor` handles request authentication
-   **Models**: `User`, `Session`, `PasswordResetToken`, `Current` (thread-local)
-   **Scheduled Tasks**: Automatic cleanup of expired password reset tokens (hourly)

## API Reference

### Annotations

#### `@RequireAuthentication`

Applied to controllers or methods to require authentication.

```java
@Controller
@RequireAuthentication
public class SecureController {
    // All methods require authentication
}
```

#### `@AllowUnauthenticatedAccess`

Override `@RequireAuthentication` for specific methods.

```java
@GetMapping("/public")
@AllowUnauthenticatedAccess
public String publicEndpoint() {
    return "public";
}
```

### Services

#### AuthenticationService

```java
// Check if user is authenticated
boolean authenticated = authenticationService.authenticated();

// Start new session
Session session = authenticationService.startNewSessionFor(user, request, response);

// Terminate session
authenticationService.terminateSession(response);
```

#### UserService

```java
// Authenticate user
Optional<User> user = userService.authenticateUser(email, password);

// Reset password (transactional)
boolean success = userService.resetPassword(userId, newPassword);

// Find user
Optional<User> user = userService.findByEmailAddress(email);
```

## User Registration

Spring Simple Auth does not include user registration by design (following Rails' approach). Implement registration in your application:

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

## Customization

### Custom UserService

Extend or replace the default `UserService`:

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
        // Add custom authentication logic (e.g., account lockout)
        return super.authenticateUser(email, password);
    }
}
```

### Custom Password Reset Token Expiration

Configure via application properties (default is 15 minutes):

```properties
# Change to 30 minutes (1800 seconds)
simple-auth.password-reset.token-expiry=1800

# Or 1 hour (3600 seconds)
simple-auth.password-reset.token-expiry=3600
```

### Add Session Revocation

```java
@Service
public class SessionManagementService {

    private final SessionRepository sessionRepository;

    public void revokeAllUserSessions(User user) {
        sessionRepository.deleteByUser(user);
    }
}
```

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    password_digest VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Sessions Table

```sql
CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Password Reset Tokens Table

```sql
CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    selector VARCHAR(36) NOT NULL UNIQUE,
    verifier_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Security Design**: Uses selector + verifier pattern where:

-   `selector`: Random UUID for fast lookup (stored in plaintext)
-   `verifier_hash`: BCrypt hash of the verifier portion (cannot be reversed)
-   Combined token format sent to user: `{selector}:{verifier}`

Expired tokens are automatically cleaned up every hour by `PasswordResetTokenCleanupService`.

## Security Features

-   **BCrypt Password Hashing** - Industry-standard password encryption
-   **Secure Session Tokens** - UUID-based tokens with HttpOnly cookies
-   **SQL Injection Protection** - Parameterized queries throughout
-   **Timing Attack Protection** - BCrypt inherent protection
-   **Email Enumeration Prevention** - Generic success messages
-   **IP and User-Agent Tracking** - Session monitoring capabilities
-   **Automatic Token Cleanup** - Scheduled removal of expired tokens (hourly)
-   **Hashed Password Reset Tokens** - Selector + verifier pattern with BCrypt hashing
-   **Transaction Management** - Critical operations wrapped in transactions

### Security Considerations

**Password Reset Token Security**: Uses a secure selector + verifier pattern to protect tokens even in the event of database compromise:

-   **Selector**: Random UUID stored in plaintext, used only for fast database lookup (meaningless if exposed)
-   **Verifier**: Random UUID hashed with BCrypt before storage, verified on token use
-   **Token Format**: `{selector}:{verifier}` sent to user via email
-   **Protection**: Even if database is breached, attackers only see selector + verifier hash (cannot forge valid tokens)
-   **Additional Safeguards**:
    -   Tokens expire after 15 minutes by default (configurable)
    -   Only one token per user (old tokens deleted when new ones created)
    -   Tokens are single-use and deleted immediately after password reset
    -   Automatic hourly cleanup of expired tokens

## Supported Databases

-   H2
-   MySQL / MariaDB
-   PostgreSQL
-   SQL Server
-   Oracle (schema-all.sql fallback)

Database platform is auto-detected from DataSource metadata.

## Testing

Run tests:

```bash
./mvnw test
```

Test coverage:

-   112 tests total (all passing)
-   Unit tests: Controllers, Services, Models, Repositories
-   Integration tests: Full application context with authentication flows
-   Comprehensive test coverage across all components

## Comparison with Rails

Spring Simple Auth is inspired by Rails 8's authentication generator:

| Feature             | Rails 8        | Spring Simple Auth                 |
| ------------------- | -------------- | ---------------------------------- |
| Session Management  | ✅             | ✅                                 |
| Password Reset      | ✅             | ✅                                 |
| BCrypt Hashing      | ✅             | ✅                                 |
| Token Storage       | Signed (no DB) | ✅ Selector + Hashed Verifier (DB) |
| Token Expiration    | 15 min         | 15 min (configurable)              |
| Auto-configuration  | ✅             | ✅                                 |
| Annotation Security | ❌             | ✅                                 |
| Multi-database      | Limited        | ✅                                 |
| Token Cleanup       | N/A (signed)   | ✅ Automatic (hourly)              |
| User Registration   | ❌             | ❌                                 |

## What's NOT Included

This is a foundation, not a complete auth system. You'll need to add:

-   **User registration** - Implement your own signup flow
-   **Email verification** - Add email confirmation if needed
-   **Remember me** - Extend session expiration
-   **OAuth/Social login** - Integrate third-party providers
-   **Two-factor authentication** - Add 2FA support
-   **Account lockout** - Prevent brute force attacks

## Examples

See the `simple-auth-spring-sample` module for a complete working example.

## Requirements

-   Java 21+
-   Spring Boot 3.5+
-   Spring Framework 6.x

## License

MIT License

## Contributing

Contributions are welcome! Please submit pull requests or open issues on GitHub.

## Credits

Inspired by Rails 8's authentication generator and Ruby on Rails conventions.
