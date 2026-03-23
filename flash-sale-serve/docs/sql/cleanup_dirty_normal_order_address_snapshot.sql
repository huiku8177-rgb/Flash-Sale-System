USE flash_sale;

-- 历史普通订单脏地址快照清理脚本
-- 适用场景：
-- 1. address_snapshot 为空
-- 2. address_snapshot 缺少 receiver / mobile / detail 任一字段
-- 3. receiver / detail 为空字符串
-- 4. mobile 不是 11 位大陆手机号
--
-- 执行建议：
-- 1. 先执行“预览脏数据”
-- 2. 确认结果无误后，再执行“备份脏数据”
-- 3. 最后执行“删除脏数据”


-- =========================
-- 1. 预览脏数据
-- =========================

SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.order_status,
    o.address_snapshot,
    o.create_time
FROM normal_order o
WHERE o.address_snapshot IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver'))) = ''
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) NOT REGEXP '^1[0-9]{10}$'
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail'))) = ''
ORDER BY o.create_time DESC, o.id DESC;


-- =========================
-- 2. 备份脏数据
-- =========================

CREATE TABLE IF NOT EXISTS normal_order_dirty_backup_20260323 AS
SELECT *
FROM normal_order
WHERE 1 = 0;

CREATE TABLE IF NOT EXISTS normal_order_item_dirty_backup_20260323 AS
SELECT *
FROM normal_order_item
WHERE 1 = 0;

INSERT INTO normal_order_dirty_backup_20260323
SELECT o.*
FROM normal_order o
WHERE o.address_snapshot IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver'))) = ''
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) NOT REGEXP '^1[0-9]{10}$'
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail'))) = '';

INSERT INTO normal_order_item_dirty_backup_20260323
SELECT i.*
FROM normal_order_item i
WHERE i.order_id IN (
    SELECT o.id
    FROM normal_order o
    WHERE o.address_snapshot IS NULL
       OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver')) IS NULL
       OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver'))) = ''
       OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) IS NULL
       OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) NOT REGEXP '^1[0-9]{10}$'
       OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail')) IS NULL
       OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail'))) = ''
);


-- =========================
-- 3. 删除脏数据
-- =========================

DELETE FROM normal_order_item
WHERE order_id IN (
    SELECT id
    FROM normal_order_dirty_backup_20260323
);

DELETE FROM normal_order
WHERE id IN (
    SELECT id
    FROM normal_order_dirty_backup_20260323
);


-- =========================
-- 4. 清理后复核
-- =========================

SELECT COUNT(1) AS remaining_dirty_order_count
FROM normal_order o
WHERE o.address_snapshot IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.receiver'))) = ''
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) IS NULL
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.mobile')) NOT REGEXP '^1[0-9]{10}$'
   OR JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail')) IS NULL
   OR TRIM(JSON_UNQUOTE(JSON_EXTRACT(o.address_snapshot, '$.detail'))) = '';
