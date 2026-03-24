USE flash_sale;

-- 为 seckill_order 补支付时间、取消原因、取消时间字段，并增加超时扫描联合索引。

SET @pay_time_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND column_name = 'pay_time'
);

SET @ddl = IF(
    @pay_time_exists = 0,
    'ALTER TABLE seckill_order ADD COLUMN pay_time DATETIME NULL COMMENT ''pay time'' AFTER status',
    'SELECT ''pay_time already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cancel_reason_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND column_name = 'cancel_reason'
);

SET @ddl = IF(
    @cancel_reason_exists = 0,
    'ALTER TABLE seckill_order ADD COLUMN cancel_reason VARCHAR(64) NULL COMMENT ''cancel reason'' AFTER pay_time',
    'SELECT ''cancel_reason already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cancel_time_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND column_name = 'cancel_time'
);

SET @ddl = IF(
    @cancel_time_exists = 0,
    'ALTER TABLE seckill_order ADD COLUMN cancel_time DATETIME NULL COMMENT ''cancel time'' AFTER cancel_reason',
    'SELECT ''cancel_time already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND index_name = 'idx_order_status_create_time'
);

SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE seckill_order ADD KEY idx_order_status_create_time (status, create_time)',
    'SELECT ''idx_order_status_create_time already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
