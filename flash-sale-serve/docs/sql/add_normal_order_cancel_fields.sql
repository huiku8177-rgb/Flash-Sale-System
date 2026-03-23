USE flash_sale;

-- 为普通订单补充取消原因、取消时间字段，并增加超时扫描联合索引。

SET @cancel_reason_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'normal_order'
      AND column_name = 'cancel_reason'
);

SET @cancel_reason_ddl = IF(
    @cancel_reason_exists = 0,
    'ALTER TABLE normal_order ADD COLUMN cancel_reason VARCHAR(64) NULL COMMENT ''cancel reason'' AFTER address_snapshot',
    'SELECT ''cancel_reason already exists'''
);

PREPARE stmt FROM @cancel_reason_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cancel_time_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'normal_order'
      AND column_name = 'cancel_time'
);

SET @cancel_time_ddl = IF(
    @cancel_time_exists = 0,
    'ALTER TABLE normal_order ADD COLUMN cancel_time DATETIME NULL COMMENT ''cancel time'' AFTER cancel_reason',
    'SELECT ''cancel_time already exists'''
);

PREPARE stmt FROM @cancel_time_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'normal_order'
      AND index_name = 'idx_normal_order_status_create_time'
);

SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE normal_order ADD KEY idx_normal_order_status_create_time (order_status, create_time)',
    'SELECT ''idx_normal_order_status_create_time already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
