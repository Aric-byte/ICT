-- ============================================================
-- V2 迁移：补全 app_config 审计字段并创建 updated_at 自动刷新触发器
-- 幂等：ADD COLUMN IF NOT EXISTS + CREATE OR REPLACE FUNCTION + DROP TRIGGER IF EXISTS
-- ============================================================

ALTER TABLE app_config ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE app_config SET created_at = updated_at WHERE created_at IS NULL;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_app_config_updated_at ON app_config;

CREATE TRIGGER trg_app_config_updated_at
    BEFORE UPDATE ON app_config
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();