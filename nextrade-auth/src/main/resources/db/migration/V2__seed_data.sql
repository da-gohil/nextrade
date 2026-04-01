-- Seed admin user (password: admin123)
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, role, is_active)
VALUES ('admin@nextrade.com', '$2a$12$GmxvpqXUG.xOF.C0OHbLmOGrCM0VbbTYqFkQcFq/N5.PqM.4MkNxS', 'Admin', 'User', 'ADMIN', true);

-- Seed vendor user (password: vendor123)
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, role, is_active)
VALUES ('vendor@nextrade.com', '$2a$12$GmxvpqXUG.xOF.C0OHbLmOGrCM0VbbTYqFkQcFq/N5.PqM.4MkNxS', 'Jane', 'Vendor', 'VENDOR', true);

-- Seed customer user (password: customer123)
INSERT IGNORE INTO users (email, password_hash, first_name, last_name, role, is_active)
VALUES ('customer@nextrade.com', '$2a$12$GmxvpqXUG.xOF.C0OHbLmOGrCM0VbbTYqFkQcFq/N5.PqM.4MkNxS', 'John', 'Customer', 'CUSTOMER', true);
