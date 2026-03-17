USE flash_sale;

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '每日坚果礼盒', '办公室下午茶常备零食', 1, 39.90, 59.90, 260, 1, NULL, '混合坚果礼盒，适合办公室和家庭日常囤货。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '每日坚果礼盒');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '挂耳咖啡套装', '10 包装黑咖啡', 1, 24.90, 36.90, 480, 1, NULL, '适合通勤和办公室场景的便携挂耳咖啡。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '挂耳咖啡套装');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '蓝牙音箱 Mini', '桌面便携小音箱', 2, 129.00, 169.00, 120, 1, NULL, '适合宿舍、办公桌和小空间的桌面音箱。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '蓝牙音箱 Mini');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '人体工学鼠标', '长时间办公更舒适', 2, 89.00, 119.00, 320, 1, NULL, '人体工学设计，适合高频办公和轻度设计场景。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '人体工学鼠标');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '无糖气泡水整箱', '24 瓶装家庭囤货', 3, 59.90, 79.90, 200, 1, NULL, '适合家庭冰箱和办公室茶水间整箱采购。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '无糖气泡水整箱');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '厨房纸巾四连包', '家用日耗补货款', 4, 19.90, 29.90, 520, 1, NULL, '高频复购商品，适合作为首页日销品展示。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '厨房纸巾四连包');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '数据线双口快充套装', 'Type-C + Lightning', 5, 49.00, 69.00, 340, 1, NULL, '适合手机、平板和桌面设备日常补件。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '数据线双口快充套装');

INSERT INTO product (name, subtitle, category_id, price, market_price, stock, status, main_image, detail)
SELECT '护手霜礼盒', '清爽不黏腻', 6, 35.00, 49.90, 160, 1, NULL, '适合个人护理和节日礼盒场景。'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '护手霜礼盒');
