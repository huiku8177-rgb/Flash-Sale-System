USE flash_sale;

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'product'
    ) AND NOT EXISTS(
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
    ),
    'RENAME TABLE product TO seckill_product',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'price'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'base price' AFTER name"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'seckill_price'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN seckill_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'seckill price' AFTER price"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'stock'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN stock INT NOT NULL DEFAULT 0 COMMENT 'seckill stock' AFTER seckill_price"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'status'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0-offline,1-online' AFTER stock"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'start_time'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN start_time DATETIME NULL COMMENT 'seckill start time' AFTER status"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'end_time'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN end_time DATETIME NULL COMMENT 'seckill end time' AFTER start_time"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'create_time'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time' AFTER end_time"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'seckill_product'
          AND COLUMN_NAME = 'update_time'
    ),
    'SELECT 1',
    "ALTER TABLE seckill_product ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time' AFTER create_time"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE seckill_product
SET seckill_price = price
WHERE seckill_price = 0.00;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'normal product id',
    name VARCHAR(128) NOT NULL COMMENT 'normal product name',
    subtitle VARCHAR(255) NULL COMMENT 'product subtitle',
    category_id BIGINT NULL COMMENT 'category id',
    price DECIMAL(10, 2) NOT NULL COMMENT 'sale price',
    market_price DECIMAL(10, 2) NULL COMMENT 'market price',
    stock INT NOT NULL DEFAULT 0 COMMENT 'normal stock',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-offline,1-online',
    main_image VARCHAR(512) NULL COMMENT 'main image url',
    detail TEXT NULL COMMENT 'product detail content',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    KEY idx_product_category_id (category_id),
    KEY idx_product_status (status)
) COMMENT='normal product table';
