-- Test user data
-- This file is automatically loaded by Spring Boot on startup

-- Insert a test user
-- Email: test@example.com
-- Password: password123 (BCrypt hashed with strength 10)
INSERT INTO users (email_address, password_digest, created_at, updated_at)
VALUES ('test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
