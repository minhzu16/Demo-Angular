-- Sprint 10 - Wishlist Feature
-- Create wishlists table

CREATE TABLE wishlists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'User ID',
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    variant_id BIGINT NULL COMMENT 'Product variant ID (optional)',
    notes VARCHAR(500) NULL COMMENT 'User notes',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Added timestamp',
    
    UNIQUE KEY uk_user_product_variant (user_id, product_id, variant_id),
    INDEX idx_user (user_id),
    INDEX idx_product (product_id),
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
