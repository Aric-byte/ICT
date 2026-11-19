-- ============================================================
-- 基线迁移脚本 V1__init.sql
-- 仅建立基础设施表，禁止业务实体表/业务关系表/业务索引/预填业务数据
-- 幂等保证：CREATE TABLE IF NOT EXISTS + INSERT ... ON CONFLICT DO NOTHING
-- ============================================================

CREATE TABLE IF NOT EXISTS app_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT,
    description  VARCHAR(256),
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_config (config_key, config_value, description) VALUES
    ('app.name', 'CloudLab Copilot', '应用名称'),
    ('app.version', '1.0.0-SNAPSHOT', '应用版本'),
    ('migration.baseline', 'V1', '迁移基线版本')
ON CONFLICT (config_key) DO NOTHING;