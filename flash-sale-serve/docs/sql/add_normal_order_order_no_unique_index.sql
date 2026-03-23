USE flash_sale;

-- 为已存在的 normal_order 表补充 order_no 唯一索引
-- 如果库中已经存在重复 order_no，执行前需要先手工去重

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'normal_order'
      AND index_name = 'uk_normal_order_order_no'
);

SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE normal_order ADD UNIQUE KEY uk_normal_order_order_no (order_no)',
    'SELECT ''uk_normal_order_order_no already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SHOW INDEX FROM normal_order WHERE Key_name = 'uk_normal_order_order_no';
