-- ============================================================
-- TIKI E-COMMERCE - LARGE SEED DATA (20+ RECORDS)
-- ============================================================

-- 1. AUTH & USERS
USE auth_db;
SET FOREIGN_KEY_CHECKS = 0;
SET @HASH = '$2a$10$PWRJl4vxcwxRcbxZDVaKseTzY2s.h0povfkNSKDtcwAx56sFx0D9W';

INSERT INTO users (id, username, email, password_hash, role, created_at, updated_at, email_verified) VALUES
(1, 'admin', 'admin@tiki.vn', @HASH, 'ADMIN', NOW(), NOW(), 1),
(2, 'tech_store', 'tech_store@email.com', @HASH, 'SELLER', NOW(), NOW(), 1),
(3, 'fashion_hub', 'fashion@email.com', @HASH, 'SELLER', NOW(), NOW(), 1),
(4, 'book_world', 'books@email.com', @HASH, 'SELLER', NOW(), NOW(), 1),
(5, 'an_nguyen', 'an@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(6, 'binh_le', 'binh@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(7, 'chi_pham', 'chi@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(8, 'duy_tran', 'duy@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(9, 'em_hoang', 'em@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(10, 'giang_vo', 'giang@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(11, 'hung_do', 'hung@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(12, 'lan_bui', 'lan@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(13, 'minh_vu', 'minh@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(14, 'nam_dang', 'nam@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(15, 'oanh_le', 'oanh@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(16, 'phuc_nguyen', 'phuc@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(17, 'quyen_tran', 'quyen@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(18, 'son_pham', 'son@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(19, 'tu_do', 'tu@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(20, 'uyet_bui', 'uyet@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1),
(21, 'vinh_ngo', 'vinh@gmail.com', @HASH, 'BUYER', NOW(), NOW(), 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);
SET FOREIGN_KEY_CHECKS = 1;

-- 2. PRODUCTS, CATEGORIES, BRANDS
USE product_db;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO categories (id, name, parent_id) VALUES
(1, 'Digital Devices', NULL),
(2, 'Computers & IT', NULL),
(3, 'Home Appliances', NULL),
(4, 'Fashion', NULL),
(5, 'Books', NULL),
(11, 'Smartphones', 1),
(12, 'Tablets', 1),
(21, 'Laptops', 2),
(22, 'Monitors', 2),
(23, 'MacBooks', 2),
(31, 'Kitchen', 3),
(32, 'Living Room', 3),
(41, 'Men Clothing', 4),
(42, 'Women Clothing', 4),
(51, 'Literature', 5),
(52, 'Technology Books', 5)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO brands (id, name) VALUES
(1, 'Apple'), (2, 'Samsung'), (3, 'Dell'), (4, 'HP'), (5, 'Sony'),
(6, 'LG'), (7, 'Xiaomi'), (8, 'Asus'), (9, 'Nike'), (10, 'Adidas')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (id, name, sku, price, list_price, brand, category_id, thumbnail_url, description, stock, status, shop_id, seller_id, created_at) VALUES
(1, 'iPhone 15 Pro Max', 'IP15PM', 33990000, 34990000, 'Apple', 11, 'https://salt.tikicdn.com/cache/750x750/ts/product/5e/18/24/2a6154ba08df6ce6161d13f4303fa19e.jpg', 'Latest iPhone', 100, 'ACTIVE', 2, 2, NOW()),
(2, 'Samsung Galaxy S24 Ultra', 'S24U', 28990000, 33990000, 'Samsung', 11, 'https://salt.tikicdn.com/cache/750x750/ts/product/b9/8d/62/1652417770851578335.jpg', 'Flagship Samsung', 80, 'ACTIVE', 2, 2, NOW()),
(3, 'iPad Pro M2', 'IPADM2', 21990000, 23990000, 'Apple', 12, 'https://salt.tikicdn.com/cache/750x750/ts/product/d0/25/61/88863f631169a13535914489a24ca609.jpg', 'Powerful Tablet', 50, 'ACTIVE', 2, 2, NOW()),
(4, 'Samsung Tab S9', 'TABS9', 15990000, 17990000, 'Samsung', 12, 'https://salt.tikicdn.com/cache/750x750/ts/product/9e/7b/0a/601e3860bb4d3807204480e64303f295.jpg', 'OLED Tablet', 60, 'ACTIVE', 2, 2, NOW()),
(5, 'Dell XPS 13', 'XPS13', 35990000, 38990000, 'Dell', 21, 'https://salt.tikicdn.com/cache/750x750/ts/product/16/be/da/b98cf98980b67ff9b987b469950c4ca5.jpg', 'Premium Laptop', 30, 'ACTIVE', 2, 2, NOW()),
(6, 'MacBook Air M2', 'MBA-M2', 24890000, 28990000, 'Apple', 23, 'https://salt.tikicdn.com/cache/750x750/ts/product/87/44/e3/3762696076594264667104646733ec60.png', 'Thin & Light', 40, 'ACTIVE', 2, 2, NOW()),
(7, 'Sony WH-1000XM5', 'XM5', 8490000, 9490000, 'Sony', 1, 'https://salt.tikicdn.com/cache/750x750/ts/product/dc/24/76/0a5f8fc31b9944a69ad669a0bd13f436.jpg', 'ANC Headphones', 120, 'ACTIVE', 2, 2, NOW()),
(8, 'Xiaomi 14 Ultra', 'X14U', 24990000, 26990000, 'Xiaomi', 11, 'https://salt.tikicdn.com/cache/750x750/ts/product/58/5c/4b/f2d5e7f0985558455776d5e1657c9cc7.jpg', 'Leica Camera', 70, 'ACTIVE', 2, 2, NOW()),
(9, 'Asus ROG Zephyrus G14', 'G14', 42990000, 45990000, 'Asus', 21, 'https://salt.tikicdn.com/cache/750x750/ts/product/49/0d/1b/adcc9c788939c895996020586f37da3f.jpg', 'Gaming laptop', 25, 'ACTIVE', 3, 3, NOW()),
(10, 'Nike Air Max 270', 'AM270', 3500000, 4000000, 'Nike', 41, 'https://salt.tikicdn.com/cache/750x750/ts/product/8e/31/6f/a6652494511675713437157a5c16a609.jpg', 'Running shoes', 200, 'ACTIVE', 3, 3, NOW()),
(11, 'Adidas Ultraboost', 'UB22', 3200000, 3800000, 'Adidas', 41, 'https://salt.tikicdn.com/cache/750x750/ts/product/6e/8f/94/a6652494511675713437157a5c16a609.jpg', 'Comfortable sneakers', 150, 'ACTIVE', 3, 3, NOW()),
(12, 'LG C3 OLED TV', 'LGC3', 29900000, 35000000, 'LG', 1, 'https://salt.tikicdn.com/cache/750x750/ts/product/8c/6f/1b/adcc9c788939c895996020586f37da3f.jpg', 'Perfect Black', 15, 'ACTIVE', 4, 4, NOW()),
(13, 'Clean Code Book', 'BOOK01', 550000, 600000, 'Books', 52, 'https://salt.tikicdn.com/cache/750x750/ts/product/9e/7b/0a/601e3860bb4d3807204480e64303f295.jpg', 'Coding bible', 500, 'ACTIVE', 4, 4, NOW()),
(14, 'iPhone 13', 'IP13', 14990000, 16990000, 'Apple', 11, 'https://salt.tikicdn.com/cache/750x750/ts/product/6e/8f/94/a6652494511675713437157a5c16a609.jpg', 'Value iPhone', 300, 'ACTIVE', 2, 2, NOW()),
(15, 'Samsung A54', 'A54', 8990000, 10490000, 'Samsung', 11, 'https://salt.tikicdn.com/cache/750x750/ts/product/d0/25/61/88863f631169a13535914489a24ca609.jpg', 'Mid-range king', 400, 'ACTIVE', 2, 2, NOW()),
(16, 'AirPods Pro 2', 'APP2', 5990000, 6990000, 'Apple', 1, 'https://salt.tikicdn.com/cache/750x750/ts/product/16/be/da/b98cf98980b67ff9b987b469950c4ca5.jpg', 'MagSafe USB-C', 500, 'ACTIVE', 2, 2, NOW()),
(17, 'Dell UltraSharp', 'U2723QE', 12500000, 14000000, 'Dell', 22, 'https://salt.tikicdn.com/cache/750x750/ts/product/58/5c/4b/f2d5e7f0985558455776d5e1657c9cc7.jpg', '4K Monitor', 40, 'ACTIVE', 2, 2, NOW()),
(18, 'MacBook Pro 14', 'MBP14', 45990000, 49990000, 'Apple', 23, 'https://salt.tikicdn.com/cache/750x750/ts/product/dc/24/76/0a5f8fc31b9944a69ad669a0bd13f436.jpg', 'M3 Pro chip', 20, 'ACTIVE', 2, 2, NOW()),
(19, 'Monitor Samsung Odyssey', 'G7', 11000000, 13000000, 'Samsung', 22, 'https://salt.tikicdn.com/cache/750x750/ts/product/44/55/66/77.jpg', 'Curved Gaming', 30, 'ACTIVE', 2, 2, NOW()),
(20, 'T-Shirt Cotton', 'TSH01', 250000, 350000, 'Xiaomi', 41, 'https://salt.tikicdn.com/cache/750x750/ts/product/11/22/33/44.jpg', 'Soft cotton', 1000, 'ACTIVE', 3, 3, NOW()),
(21, 'Air Fryer Philps', 'HD9270', 3500000, 4500000, 'Xiaomi', 31, 'https://salt.tikicdn.com/cache/750x750/ts/product/99/00/11/22.jpg', 'Large capacity', 100, 'ACTIVE', 4, 4, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

SET FOREIGN_KEY_CHECKS = 1;
