-- Run once with a migration account before starting production with ddl-auto=validate.
-- Existing app_users / nfc_cards tables and their ownership migration must already exist.
CREATE TABLE IF NOT EXISTS auth_sessions (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_auth_session_expiry (expires_at)
);
