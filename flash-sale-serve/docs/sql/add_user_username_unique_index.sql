USE flash_sale;

SET @table_exists = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
);

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'uk_user_username'
);

SET @ddl = IF(
    @table_exists = 0,
    'SELECT ''table `user` does not exist''',
    IF(
        @index_exists = 0,
        'ALTER TABLE `user` ADD UNIQUE KEY uk_user_username (username)',
        'SELECT ''uk_user_username already exists'''
    )
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SHOW INDEX FROM `user` WHERE Key_name = 'uk_user_username';
