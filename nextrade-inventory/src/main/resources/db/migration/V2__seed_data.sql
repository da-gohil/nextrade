-- Seed categories
INSERT IGNORE INTO categories (name, description, is_active) VALUES
('Electronics', 'Electronic devices and accessories', true),
('Clothing', 'Fashion and apparel', true),
('Home & Garden', 'Home improvement and garden products', true),
('Books', 'Books, magazines, and educational materials', true),
('Sports', 'Sports equipment and outdoor gear', true);

-- Seed products (vendor_id=2 is the vendor user from auth seeding)
INSERT IGNORE INTO products (sku, name, description, category_id, vendor_id, price, stock_quantity, reserved_quantity, low_stock_threshold, is_active)
VALUES
('LAPTOP-PRO-001', 'ProBook Laptop 15"', 'High-performance 15-inch laptop with Intel i7 processor, 16GB RAM, 512GB SSD', 1, 2, 1299.99, 50, 0, 5, true),
('PHONE-X1-001', 'SmartPhone X1', '6.1-inch OLED display, 128GB storage, triple camera system', 1, 2, 799.99, 100, 0, 10, true),
('HEADPHONES-W1', 'Wireless Headphones Pro', 'Active noise cancellation, 30-hour battery life', 1, 2, 249.99, 75, 0, 8, true),
('TSHIRT-CLASSIC-M', 'Classic Cotton T-Shirt (M)', 'Premium 100% organic cotton, medium size', 2, 2, 29.99, 200, 0, 20, true),
('JEANS-SLIM-32', 'Slim Fit Jeans 32"', 'Classic slim fit denim jeans, 32-inch waist', 2, 2, 79.99, 80, 0, 10, true),
('COFFEE-TABLE-001', 'Modern Coffee Table', 'Minimalist wooden coffee table, 120x60cm', 3, 2, 349.99, 20, 0, 3, true),
('DESK-LAMP-LED', 'LED Desk Lamp', 'Adjustable brightness, USB charging port, 360° rotation', 3, 2, 59.99, 60, 0, 8, true),
('BOOK-JAVA-CLEAN', 'Clean Code (Java)', 'A Handbook of Agile Software Craftsmanship by Robert Martin', 4, 2, 39.99, 150, 0, 15, true),
('YOGA-MAT-PRO', 'Professional Yoga Mat', 'Non-slip, 6mm thickness, eco-friendly material', 5, 2, 49.99, 90, 0, 10, true),
('WATER-BOTTLE-32', 'Insulated Water Bottle 32oz', 'Keeps cold 24h, hot 12h, BPA-free stainless steel', 5, 2, 34.99, 120, 0, 15, true);
