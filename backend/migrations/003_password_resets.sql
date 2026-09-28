-- Run once with a migration account; one pending reset per user.
CREATE TABLE IF NOT EXISTS password_resets (
    user_id BIGINT NOT NULL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL,
    expires_at DATETIME(6) NOT NULL
);
