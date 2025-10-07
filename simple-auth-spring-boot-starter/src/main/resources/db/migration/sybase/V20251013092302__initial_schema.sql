-- Sybase Database Schema for Spring Simple Auth

-- Users table
CREATE TABLE users (
    id NUMERIC(19,0) IDENTITY PRIMARY KEY,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    password_digest VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- Sessions table
CREATE TABLE sessions (
    id NUMERIC(19,0) IDENTITY PRIMARY KEY,
    user_id NUMERIC(19,0) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for sessions
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_token ON sessions(token);

-- Password reset tokens table
CREATE TABLE password_reset_tokens (
    id NUMERIC(19,0) IDENTITY PRIMARY KEY,
    user_id NUMERIC(19,0) NOT NULL,
    selector VARCHAR(36) NOT NULL UNIQUE,
    verifier_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for password reset tokens
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_selector ON password_reset_tokens(selector);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
