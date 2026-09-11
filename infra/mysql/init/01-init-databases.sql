-- Initialize databases for all microservices
-- This script runs automatically when MySQL container starts

-- Create databases for each service
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS template_storage_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS review_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS warehouse_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS settlement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS b2b_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS live_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to 'sa' user on all databases
GRANT ALL PRIVILEGES ON product_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON cart_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON user_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON shop_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON template_storage_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON review_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON warehouse_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON analytics_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON settlement_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON b2b_db.* TO 'sa'@'%';
GRANT ALL PRIVILEGES ON live_db.* TO 'sa'@'%';

-- Flush privileges to apply changes
FLUSH PRIVILEGES;

-- Show all databases
SHOW DATABASES;
