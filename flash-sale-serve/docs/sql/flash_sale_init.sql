CREATE DATABASE IF NOT EXISTS flash_sale
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE flash_sale;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'product id',
    name VARCHAR(128) NOT NULL COMMENT 'product name',
    price DECIMAL(10, 2) NOT NULL COMMENT 'base price',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT 'seckill price',
    stock INT NOT NULL DEFAULT 0 COMMENT 'stock',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-offline,1-online',
    start_time DATETIME NULL COMMENT 'seckill start time',
    end_time DATETIME NULL COMMENT 'seckill end time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time'
) COMMENT='product table';

CREATE TABLE IF NOT EXISTS seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'order id',
    user_id BIGINT NOT NULL COMMENT 'user id',
    product_id BIGINT NOT NULL COMMENT 'product id',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT 'seckill price at order time',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-created,1-paid,2-cancelled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_order_user_product (user_id, product_id),
    KEY idx_order_user_id (user_id),
    KEY idx_order_product_id (product_id),
    KEY idx_order_create_time (create_time)
) COMMENT='seckill order table';

INSERT INTO product (name, price, seckill_price, stock, status, start_time, end_time)
VALUES ('sample seckill product', 199.00, 99.00, 100, 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));
