-- V2 公共基础设施表（审计、幂等）
-- 幂等：CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS；约束随建表一次性创建

CREATE TABLE IF NOT EXISTS sys_audit_event (
    id BIGSERIAL PRIMARY KEY,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    result VARCHAR(16) NOT NULL,
    detail JSONB,
    trace_id VARCHAR(32),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_actor_role CHECK (actor_role IN ('AUTHOR','TEACHER','STUDENT','SYSTEM')),
    CONSTRAINT ck_audit_action CHECK (action IN ('LAB_PUBLISH','LAB_UNPUBLISH','EFFECTIVE_VERSION_SWITCH','VERSION_ARCHIVE','SESSION_ABANDON')),
    CONSTRAINT ck_audit_target_type CHECK (target_type IN ('LAB','VERSION','SESSION')),
    CONSTRAINT ck_audit_result CHECK (result IN ('SUCCESS','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_audit_target ON sys_audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON sys_audit_event (actor_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS sys_idempotency_key (
    operator_id VARCHAR(64) NOT NULL,
    idem_key VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_sys_idempotency_key PRIMARY KEY (operator_id, idem_key)
);

CREATE INDEX IF NOT EXISTS idx_idem_expires ON sys_idempotency_key (expires_at);