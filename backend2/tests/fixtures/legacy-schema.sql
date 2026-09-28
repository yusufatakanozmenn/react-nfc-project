-- Schema only; no customer records or credentials. Signed BIGINT and BIT columns match the Java export.
CREATE TABLE IF NOT EXISTS `app_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `email` varchar(150) NOT NULL,
  `name` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `auth_rate_limits` (
  `id` char(64) NOT NULL,
  `attempts` int unsigned NOT NULL DEFAULT '0',
  `window_started_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_rate_limits_window` (`window_started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `auth_sessions` (
  `id` varchar(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_session_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `nfc_cards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `code` varchar(255) NOT NULL,
  `destination_url` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `scans` int DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4wxkkvb5gxg9188l0ep2aor5i` (`code`),
  KEY `idx_nfc_cards_owner` (`owner_id`),
  CONSTRAINT `fk_nfc_cards_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `password_resets` (
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `email` varchar(150) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `token_hash` (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
