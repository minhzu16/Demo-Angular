-- Seed 5 warehouses
INSERT IGNORE INTO warehouses (id, code, name, address, created_at) VALUES
(1, 'WH-HN-01', 'Kho Hà Nội Chủ Đạo', '{"detail": "Khu Công Nghiệp Bắc Thăng Long", "city": "Hà Nội"}', NOW()),
(2, 'WH-HCM-01', 'Kho TP.HCM Trung Tâm', '{"detail": "Khu Công Nghệ Cao, Quận 9", "city": "TP.HCM"}', NOW()),
(3, 'WH-DN-01', 'Kho Đà Nẵng', '{"detail": "Quận Liên Chiểu", "city": "Đà Nẵng"}', NOW()),
(4, 'WH-CT-01', 'Kho Cần Thơ', '{"detail": "Quận Ninh Kiều", "city": "Cần Thơ"}', NOW()),
(5, 'WH-HP-01', 'Kho Hải Phòng', '{"detail": "Huyện Thủy Nguyên", "city": "Hải Phòng"}', NOW());

-- Seed inventory items for 25 products
INSERT IGNORE INTO inventory_items (product_id, warehouse_id, quantity_available, quantity_reserved) VALUES
(1, 1, 100, 5),
(2, 1, 150, 10),
(3, 2, 85, 2),
(4, 2, 110, 8),
(5, 1, 45, 1),
(6, 2, 30, 0),
(7, 3, 15, 3),
(8, 3, 60, 4),
(9, 4, 40, 2),
(10, 5, 55, 6),
(11, 1, 25, 0),
(12, 2, 20, 1),
(13, 3, 12, 0),
(14, 4, 35, 2),
(15, 5, 18, 4),
(16, 1, 90, 5),
(17, 2, 50, 2),
(18, 3, 100, 0),
(19, 4, 75, 3),
(20, 5, 5, 1),
(21, 1, 20, 2),
(22, 2, 30, 0),
(23, 3, 45, 1),
(24, 4, 22, 2),
(25, 5, 50, 10);
