USE flash_sale;

-- 为 seckill_order 补充长订单号字段，并为历史数据回填唯一订单号。

SET @order_no_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND column_name = 'order_no'
);

SET @ddl = IF(
    @order_no_exists = 0,
    'ALTER TABLE seckill_order ADD COLUMN order_no VARCHAR(32) NULL COMMENT ''order number'' AFTER id',
    'SELECT ''order_no already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE seckill_order
SET order_no = REPLACE(UUID(), '-', '')
WHERE order_no IS NULL
   OR order_no = '';

SET @order_no_not_null = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND column_name = 'order_no'
      AND is_nullable = 'NO'
);

SET @ddl = IF(
    @order_no_not_null = 0,
    'ALTER TABLE seckill_order MODIFY COLUMN order_no VARCHAR(32) NOT NULL COMMENT ''order number''',
    'SELECT ''order_no already not null'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND index_name = 'uk_seckill_order_order_no'
);

SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE seckill_order ADD UNIQUE KEY uk_seckill_order_order_no (order_no)',
    'SELECT ''uk_seckill_order_order_no already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
