CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    product_id BIGINT NULL,
    context_type VARCHAR(32) NULL,
    session_status TINYINT NOT NULL DEFAULT 1,
    message_count INT NOT NULL DEFAULT 0,
    last_question VARCHAR(500) NULL,
    last_answer_summary VARCHAR(500) NULL,
    context_state_json LONGTEXT NULL,
    created_at DATETIME NOT NULL,
    last_active_at DATETIME NOT NULL,
    expire_at DATETIME NOT NULL,
    UNIQUE KEY uk_chat_session_session_id (session_id),
    KEY idx_chat_session_user_last_active (user_id, last_active_at),
    KEY idx_chat_session_product_last_active (product_id, last_active_at),
    KEY idx_chat_session_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    product_id BIGINT NULL,
    record_no INT NOT NULL,
    question VARCHAR(500) NOT NULL,
    question_category VARCHAR(32) NULL,
    intent_type VARCHAR(64) NULL,
    route_type VARCHAR(64) NULL,
    rewritten_question VARCHAR(500) NULL,
    answer TEXT NOT NULL,
    answer_policy VARCHAR(64) NOT NULL,
    sources_json LONGTEXT NULL,
    hit_knowledge_json LONGTEXT NULL,
    compare_candidates_json LONGTEXT NULL,
    confidence DECIMAL(6,4) NULL,
    fallback_reason VARCHAR(64) NULL,
    audit_summary VARCHAR(1000) NULL,
    model_name VARCHAR(64) NULL,
    latency_ms INT NULL,
    estimated_tokens INT NULL,
    created_at DATETIME NOT NULL,
    expire_at DATETIME NOT NULL,
    KEY idx_chat_record_session_record_no (session_id, record_no),
    KEY idx_chat_record_session_created_at (session_id, created_at),
    KEY idx_chat_record_product_created_at (product_id, created_at),
    KEY idx_chat_record_user_created_at (user_id, created_at),
    KEY idx_chat_record_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS upgrade_ai_service_chat_schema;

DELIMITER $$

CREATE PROCEDURE upgrade_ai_service_chat_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_session'
          AND column_name = 'context_state_json'
    ) THEN
        ALTER TABLE chat_session
            ADD COLUMN context_state_json LONGTEXT NULL AFTER last_answer_summary;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_record'
          AND column_name = 'intent_type'
    ) THEN
        ALTER TABLE chat_record
            ADD COLUMN intent_type VARCHAR(64) NULL AFTER question_category;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_record'
          AND column_name = 'route_type'
    ) THEN
        ALTER TABLE chat_record
            ADD COLUMN route_type VARCHAR(64) NULL AFTER intent_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_record'
          AND column_name = 'rewritten_question'
    ) THEN
        ALTER TABLE chat_record
            ADD COLUMN rewritten_question VARCHAR(500) NULL AFTER route_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'chat_record'
          AND column_name = 'compare_candidates_json'
    ) THEN
        ALTER TABLE chat_record
            ADD COLUMN compare_candidates_json LONGTEXT NULL AFTER hit_knowledge_json;
    END IF;
END $$

DELIMITER ;

CALL upgrade_ai_service_chat_schema();

DROP PROCEDURE IF EXISTS upgrade_ai_service_chat_schema;
