-- Flyway migration: create sellers and complaints tables
CREATE TABLE IF NOT EXISTS sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    return_policy TEXT,
    shipping_policy TEXT
);

CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution TEXT,
    created_at DATETIME,
    resolved_at DATETIME
);
