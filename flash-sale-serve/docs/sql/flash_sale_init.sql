CREATE DATABASE IF NOT EXISTS flash_sale
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE flash_sale;

CREATE TABLE IF NOT EXISTS seckill_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'seckill product id',
    name VARCHAR(128) NOT NULL COMMENT 'seckill product name',
    price DECIMAL(10, 2) NOT NULL COMMENT 'base price',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT 'seckill price',
    stock INT NOT NULL DEFAULT 0 COMMENT 'seckill stock',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-offline,1-online',
    start_time DATETIME NULL COMMENT 'seckill start time',
    end_time DATETIME NULL COMMENT 'seckill end time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time'
) COMMENT='seckill product table';

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

CREATE TABLE IF NOT EXISTS seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'order id',
    order_no VARCHAR(32) NOT NULL COMMENT 'order number',
    user_id BIGINT NOT NULL COMMENT 'user id',
    product_id BIGINT NOT NULL COMMENT 'seckill product id',
    seckill_price DECIMAL(10, 2) NOT NULL COMMENT 'seckill price at order time',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-created,1-paid,2-cancelled',
    pay_time DATETIME NULL COMMENT 'pay time',
    cancel_reason VARCHAR(64) NULL COMMENT 'cancel reason',
    cancel_time DATETIME NULL COMMENT 'cancel time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_order_user_product (user_id, product_id),
    UNIQUE KEY uk_seckill_order_order_no (order_no),
    KEY idx_order_user_id (user_id),
    KEY idx_order_product_id (product_id),
    KEY idx_order_create_time (create_time),
    KEY idx_order_status_create_time (status, create_time)
) COMMENT='seckill order table';

INSERT INTO seckill_product (name, price, seckill_price, stock, status, start_time, end_time)
VALUES ('sample seckill product', 199.00, 99.00, 100, 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
VALUES
('每日坚果礼盒', '办公室下午茶常备零食', 1, 39.90, 59.90, 260, 1, NULL, '混合坚果礼盒，适合办公室和家庭日常囤货。'),
('挂耳咖啡套装', '10 包装黑咖啡', 1, 24.90, 36.90, 480, 1, NULL, '适合通勤和办公室场景的便携挂耳咖啡。'),
('蓝牙音箱 Mini', '桌面便携小音箱', 2, 129.00, 169.00, 120, 1, NULL, '适合宿舍、办公桌和小空间的桌面音箱。'),
('人体工学鼠标', '长时间办公更舒适', 2, 89.00, 119.00, 320, 1, NULL, '人体工学设计，适合高频办公和轻度设计场景。'),
('无糖气泡水整箱', '24 瓶装家庭囤货', 3, 59.90, 79.90, 200, 1, NULL, '适合家庭冰箱和办公室茶水间整箱采购。'),
('厨房纸巾四连包', '家用日耗补货款', 4, 19.90, 29.90, 520, 1, NULL, '高频复购商品，适合作为首页日销品展示。'),
('数据线双口快充套装', 'Type-C + Lightning', 5, 49.00, 69.00, 340, 1, NULL, '适合手机、平板和桌面设备日常补件。'),
('护手霜礼盒', '清爽不黏腻', 6, 35.00, 49.90, 160, 1, NULL, '适合个人护理和节日礼盒场景。');
