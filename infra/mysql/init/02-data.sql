-- ============================================================
-- TIKI E-COMMERCE - PROFESSIONAL SEED DATA (FINAL V4 - FK SAFE)
-- ============================================================

USE auth_db;
SET FOREIGN_KEY_CHECKS = 0;
-- Passwords: 'Test123456'
SET @HASH = '$2a$10$PWRJl4vxcwxRcbxZDVaKseTzY2s.h0povfkNSKDtcwAx56sFx0D9W';

INSERT INTO users (id, username, email, password_hash, role, created_at, updated_at, email_verified) VALUES
(1,  'admin',       'admin@tiki.vn',        @HASH, 'ADMIN',  NOW() - INTERVAL 365 DAY, NOW(), 1),
(2,  'tech_store',  'tech_store@email.com', @HASH, 'SELLER', NOW() - INTERVAL 200 DAY, NOW(), 1),
(5,  'an_nguyen',   'an@gmail.com',         @HASH, 'BUYER',  NOW() - INTERVAL 120 DAY, NOW(), 1)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
SET FOREIGN_KEY_CHECKS = 1;

USE product_db;
SET FOREIGN_KEY_CHECKS = 0;
-- Categories
INSERT INTO categories (id, name, parent_id) VALUES
(1,  'Dien Thoai - May Tinh Bang', NULL),
(2,  'Laptop - Thiet Bi IT',       NULL),
(11, 'Smartphone', 1),
(23, 'MacBook',    2)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Brands
INSERT INTO brands (id, name, description, logo_url) VALUES
(1,  'Apple',   'MacBook, iPhone, iPad...', 'https://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg'),
(2,  'Samsung', 'Galaxy phones...',         'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Products
INSERT INTO products (id, name, sku, price, list_price, brand, category_id, thumbnail_url, description, stock, status, shop_id, seller_id, created_at, attributes) VALUES
(1,  'Apple iPhone 15 Pro Max', 'IP15PM-256-NV', 33990000, 34990000, 'Apple', 11,
     'https://salt.tikicdn.com/cache/750x750/ts/product/5e/18/24/2a6154ba08df6ce6161d13f4303fa19e.jpg',
     'iPhone 15 Pro Max chip A17 Pro.',
     150, 'ACTIVE', 2, 2, NOW(), '{"Color":"Titan","Storage":"256GB"}'),
(2,  'Samsung Galaxy S24 Ultra', 'S24U-256-GY', 28990000, 33990000, 'Samsung', 11,
     'https://salt.tikicdn.com/cache/750x750/ts/product/b9/8d/62/1652417770851578335.jpg',
     'S24 Ultra voi AI.',
     80, 'ACTIVE', 2, 2, NOW(), '{"Color":"Gray","Storage":"256GB"}'),
(3,  'MacBook Air M2 2022', 'MBA-M2-8-256-SLV', 24890000, 28990000, 'Apple', 23,
     'https://salt.tikicdn.com/cache/750x750/ts/product/87/44/e3/3762696076594264667104646733ec60.png',
     'MacBook Air M2 sieu mong.',
     40, 'ACTIVE', 2, 2, NOW(), '{"Color":"Silver","RAM":"8GB"}')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Product Variants
INSERT INTO product_variants (id, product_id, sku, color, size, price, stock, is_active, created_at) VALUES
(1, 1, 'IP15PM-V1', 'Titan', '256GB', 33990000, 50, 1, NOW()),
(2, 2, 'S24U-V1',   'Gray',  '256GB', 28990000, 40, 1, NOW()),
(3, 3, 'MBA-V1',    'Silver', '8GB',   24890000, 30, 1, NOW())
ON DUPLICATE KEY UPDATE sku = VALUES(sku);
SET FOREIGN_KEY_CHECKS = 1;

USE order_db;
SET FOREIGN_KEY_CHECKS = 0;
-- Vouchers
INSERT INTO vouchers (id, code, shop_id, type, value, min_order_value, start_date, end_date, max_usage, used_count, is_active, created_at) VALUES
(1,  'WELCOME50K',  NULL, 'FIXED',      50000,  200000,   NOW(), NOW() + INTERVAL 90 DAY,  1000, 0,  1, NOW())
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- Orders
INSERT INTO orders (id, user_id, order_number, status, subtotal, voucher_code, voucher_discount, shipping_fee, total_amount, customer_name, customer_phone, shipping_province, shipping_district, shipping_address, payment_method, payment_status, created_at) VALUES
(1,  5,  'ORD-101', 'DELIVERED',  33990000, 'WELCOME50K',  50000,  20000, 33960000, 'Nguyen An', '0945678901', 'HCM', 'Q.1', '12 Nguyen Hue', 'COD', 'PAID', NOW())
ON DUPLICATE KEY UPDATE order_number = VALUES(order_number);
SET FOREIGN_KEY_CHECKS = 1;

USE cart_db;
SET FOREIGN_KEY_CHECKS = 0;
-- Carts
INSERT INTO carts (id, user_id, session_id, is_active, created_at, updated_at) VALUES
(1,  5,  NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- Cart Items
INSERT INTO cart_items (id, cart_id, product_id, variant_id, qty, price_snapshot) VALUES
(1, 1, 1, 1, 1, 33990000)
ON DUPLICATE KEY UPDATE qty = VALUES(qty);
SET FOREIGN_KEY_CHECKS = 1;
