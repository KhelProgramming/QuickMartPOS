-- QUICKMART POS DATABASE - Run in MySQL Workbench as an administrator.
CREATE DATABASE IF NOT EXISTS quickmart_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quickmart_pos;

CREATE TABLE IF NOT EXISTS app_metadata (
 id TINYINT PRIMARY KEY, store_uuid CHAR(36) NOT NULL UNIQUE,
 store_name VARCHAR(120) NOT NULL DEFAULT 'QuickMart',
 schema_version INT NOT NULL DEFAULT 1, store_timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Manila',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_metadata_singleton CHECK (id=1)
) ENGINE=InnoDB;
INSERT INTO app_metadata(id,store_uuid,store_name,schema_version,store_timezone)
VALUES(1,UUID(),'QuickMart',1,'Asia/Manila')
ON DUPLICATE KEY UPDATE store_name=VALUES(store_name);

CREATE TABLE IF NOT EXISTS users (
 id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE,
 full_name VARCHAR(120) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'CASHIER',
 active BOOLEAN NOT NULL DEFAULT TRUE, password_hash VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_users_active(active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS terminals (
 id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE,
 name VARCHAR(80) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
INSERT INTO terminals(code,name,active) VALUES('T1','QuickMart Terminal 1',TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

CREATE TABLE IF NOT EXISTS products (
 id INT AUTO_INCREMENT PRIMARY KEY, barcode VARCHAR(80) NOT NULL UNIQUE,
 name VARCHAR(150) NOT NULL, category VARCHAR(80) NOT NULL DEFAULT 'General',
 price DECIMAL(12,2) NOT NULL,
 discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
 stock INT NOT NULL DEFAULT 0,
 photo_jpeg MEDIUMBLOB NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE, version INT NOT NULL DEFAULT 1,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT chk_product_price CHECK(price>=0), CONSTRAINT chk_product_stock CHECK(stock>=0),
 CONSTRAINT chk_product_discount CHECK(discount_percent BETWEEN 0 AND 100),
 INDEX idx_product_name(name), INDEX idx_product_active(active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cash_sessions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, terminal_id INT NOT NULL,
 opening_float DECIMAL(12,2) NOT NULL DEFAULT 0, expected_cash DECIMAL(12,2) NULL,
 actual_cash DECIMAL(12,2) NULL, variance DECIMAL(12,2) NULL,
 status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
 opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, closed_at DATETIME NULL,
 notes VARCHAR(500) NULL,
 CONSTRAINT fk_session_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_session_terminal FOREIGN KEY(terminal_id) REFERENCES terminals(id),
 CONSTRAINT chk_opening CHECK(opening_float>=0),
 INDEX idx_session_status_terminal(status,terminal_id), INDEX idx_session_opened(opened_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, receipt_no VARCHAR(40) NOT NULL UNIQUE,
 request_id CHAR(36) NOT NULL UNIQUE, request_hash CHAR(64) NULL,
 session_id BIGINT NOT NULL, user_id INT NOT NULL, terminal_id INT NOT NULL,
 total DECIMAL(12,2) NOT NULL, tender DECIMAL(12,2) NOT NULL,
 change_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
 status ENUM('COMPLETED','VOIDED','REFUNDED') NOT NULL DEFAULT 'COMPLETED',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_sale_session FOREIGN KEY(session_id) REFERENCES cash_sessions(id),
 CONSTRAINT fk_sale_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_sale_terminal FOREIGN KEY(terminal_id) REFERENCES terminals(id),
 CONSTRAINT chk_sale_amounts CHECK(total>=0 AND tender>=total AND change_amount>=0),
 INDEX idx_sales_date(created_at), INDEX idx_sales_session(session_id), INDEX idx_sales_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, sale_id BIGINT NOT NULL, product_id INT NOT NULL,
 product_name_snapshot VARCHAR(150) NOT NULL, barcode_snapshot VARCHAR(80) NOT NULL,
 unit_price DECIMAL(12,2) NOT NULL, regular_unit_price DECIMAL(12,2) NULL,
 discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
 quantity INT NOT NULL, line_total DECIMAL(12,2) NOT NULL,
 CONSTRAINT fk_item_sale FOREIGN KEY(sale_id) REFERENCES sales(id),
 CONSTRAINT fk_item_product FOREIGN KEY(product_id) REFERENCES products(id),
 CONSTRAINT chk_item_qty CHECK(quantity>0),
 CONSTRAINT chk_item_discount CHECK(discount_percent BETWEEN 0 AND 100),
 INDEX idx_items_sale(sale_id), INDEX idx_items_product(product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_movements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, user_id INT NOT NULL,
 sale_id BIGINT NULL, movement_type ENUM('SALE','RESTOCK','ADJUSTMENT','RETURN') NOT NULL,
 quantity_delta INT NOT NULL, reason VARCHAR(255) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_stock_product FOREIGN KEY(product_id) REFERENCES products(id),
 CONSTRAINT fk_stock_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_stock_sale FOREIGN KEY(sale_id) REFERENCES sales(id),
 INDEX idx_stock_product_date(product_id,created_at), INDEX idx_stock_sale(sale_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_reversals (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, sale_id BIGINT NOT NULL UNIQUE,
 session_id BIGINT NOT NULL, requester_id INT NOT NULL, authorizer_id INT NULL,
 amount DECIMAL(12,2) NOT NULL, reason VARCHAR(255) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_rev_sale FOREIGN KEY(sale_id) REFERENCES sales(id),
 CONSTRAINT fk_rev_session FOREIGN KEY(session_id) REFERENCES cash_sessions(id),
 CONSTRAINT fk_rev_requester FOREIGN KEY(requester_id) REFERENCES users(id),
 CONSTRAINT fk_rev_authorizer FOREIGN KEY(authorizer_id) REFERENCES users(id),
 INDEX idx_rev_date(created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, terminal_id INT NULL,
 action VARCHAR(80) NOT NULL, details TEXT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_audit_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_audit_terminal FOREIGN KEY(terminal_id) REFERENCES terminals(id),
 INDEX idx_audit_date(created_at), INDEX idx_audit_action(action)
) ENGINE=InnoDB;

INSERT INTO products(barcode,name,category,price,stock,active) VALUES
('4800012345678','Bottled Water 500ml','Beverages',20.00,50,TRUE),
('4800012345679','Potato Chips 50g','Snacks',35.00,40,TRUE),
('4800012345680','Instant Noodles','Food',18.50,60,TRUE),
('4800012345681','Bath Soap 90g','Personal Care',32.00,25,TRUE),
('4800012345682','Isopropyl Alcohol 250ml','Health',94.75,20,TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Optional restricted local database account used by the Java app:
CREATE USER IF NOT EXISTS 'quickmart_app'@'localhost' IDENTIFIED BY 'ChangeThis_QM_2026!';
GRANT SELECT,INSERT,UPDATE,DELETE ON quickmart_pos.* TO 'quickmart_app'@'localhost';
FLUSH PRIVILEGES;

-- Java creates these initial accounts on first launch if users is empty:
-- cashier / cashier123
-- admin / admin123
