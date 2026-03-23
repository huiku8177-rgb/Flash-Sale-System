USE flash_sale;

-- 普通商品订单主表
CREATE TABLE IF NOT EXISTS normal_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'normal order id',
    order_no VARCHAR(32) NOT NULL COMMENT 'business order number',
    user_id BIGINT NOT NULL COMMENT 'user id',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-created,1-paid,2-cancelled,3-delivered,4-finished',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'total amount before discounts',
    pay_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'actual paid amount',
    pay_time DATETIME NULL COMMENT 'pay time',
    remark VARCHAR(255) NULL COMMENT 'user remark',
    address_snapshot JSON NULL COMMENT 'shipping address snapshot',
    cancel_reason VARCHAR(64) NULL COMMENT 'cancel reason',
    cancel_time DATETIME NULL COMMENT 'cancel time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_normal_order_order_no (order_no),
    KEY idx_normal_order_user_id (user_id),
    KEY idx_normal_order_status (order_status),
    KEY idx_normal_order_create_time (create_time),
    KEY idx_normal_order_status_create_time (order_status, create_time)
) COMMENT='normal order table';

-- 普通商品订单明细表
CREATE TABLE IF NOT EXISTS normal_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'normal order item id',
    order_id BIGINT NOT NULL COMMENT 'normal order id',
    user_id BIGINT NOT NULL COMMENT 'user id',
    product_id BIGINT NOT NULL COMMENT 'normal product id',
    product_name VARCHAR(128) NOT NULL COMMENT 'product name snapshot',
    product_subtitle VARCHAR(255) NULL COMMENT 'product subtitle snapshot',
    product_image VARCHAR(512) NULL COMMENT 'product main image snapshot',
    sale_price DECIMAL(10, 2) NOT NULL COMMENT 'sale price snapshot',
    quantity INT NOT NULL DEFAULT 1 COMMENT 'purchase quantity',
    item_amount DECIMAL(10, 2) NOT NULL COMMENT 'item total amount',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    KEY idx_normal_order_item_order_id (order_id),
    KEY idx_normal_order_item_user_id (user_id),
    KEY idx_normal_order_item_product_id (product_id)
) COMMENT='normal order item table';
