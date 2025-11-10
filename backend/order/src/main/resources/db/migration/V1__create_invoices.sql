-- Flyway migration: create invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    issued_at DATETIME
);
