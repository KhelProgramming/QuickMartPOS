-- Run as a MySQL administrator on an existing QuickMart installation.
USE quickmart_pos;

SET @photo_upgrade_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'photo_jpeg'),
    'SELECT ''Product photo column already exists'' AS upgrade_status',
    'ALTER TABLE products ADD COLUMN photo_jpeg MEDIUMBLOB NULL AFTER stock'
);
PREPARE photo_upgrade FROM @photo_upgrade_sql;
EXECUTE photo_upgrade;
DEALLOCATE PREPARE photo_upgrade;
SET @photo_upgrade_sql = NULL;
