-- Run once per existing QuickMart database as a MySQL administrator.
-- Safe to rerun after either ALTER TABLE has succeeded.
USE quickmart_pos;

SET @discount_upgrade_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'discount_percent'),
    'SELECT ''Product discount column already exists'' AS upgrade_status',
    'ALTER TABLE products
        ADD COLUMN discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER price,
        ADD CONSTRAINT chk_product_discount CHECK (discount_percent BETWEEN 0 AND 100)'
);
PREPARE discount_upgrade FROM @discount_upgrade_sql;
EXECUTE discount_upgrade;
DEALLOCATE PREPARE discount_upgrade;

SET @discount_upgrade_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'sale_items' AND column_name = 'regular_unit_price'),
    'SELECT ''Sale item discount columns already exist'' AS upgrade_status',
    'ALTER TABLE sale_items
        ADD COLUMN regular_unit_price DECIMAL(12,2) NULL AFTER unit_price,
        ADD COLUMN discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER regular_unit_price,
        ADD CONSTRAINT chk_item_discount CHECK (discount_percent BETWEEN 0 AND 100)'
);
PREPARE discount_upgrade FROM @discount_upgrade_sql;
EXECUTE discount_upgrade;
DEALLOCATE PREPARE discount_upgrade;
SET @discount_upgrade_sql = NULL;
