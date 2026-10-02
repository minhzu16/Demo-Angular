-- ============================================================
-- NEXMART - HIGH CONCURRENCY & OUTBOX SCHEMA DEFINITIONS (PHASE 4)
-- ============================================================

USE order_db;

CREATE TABLE IF NOT EXISTS order_idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    order_id INT,
    response_body LONGTEXT,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    INDEX idx_idemp_key (idempotency_key),
    INDEX idx_idemp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id INT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    exchange VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE payment_db;

CREATE TABLE IF NOT EXISTS payment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_txn_id VARCHAR(100) NOT NULL,
    order_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    raw_payload LONGTEXT,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_provider_txn (provider, provider_txn_id),
    INDEX idx_payment_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
