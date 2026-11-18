-- V7 Provider 配置与审计（3 张表）
CREATE TABLE IF NOT EXISTS ai_provider_config (
    id BIGSERIAL PRIMARY KEY,
    provider_type VARCHAR(32) NOT NULL,
    endpoint_url VARCHAR(512) NOT NULL DEFAULT 'https://<待核实>',
    model_name VARCHAR(128) NOT NULL DEFAULT '<待核实>',
    auth_mode VARCHAR(32) NOT NULL DEFAULT '<待核实>',
    credential_ref VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    timeout_ms INT NOT NULL DEFAULT 8000,
    max_retries INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_provider_type CHECK (provider_type IN ('MAAS', 'OPENAI_COMPATIBLE', 'NONE'))
);

CREATE TABLE IF NOT EXISTS qa_audit_event (
    id BIGSERIAL PRIMARY KEY,
    question_id VARCHAR(32),
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason_code VARCHAR(32),
    hit_count INT,
    detail JSONB,
    trace_id VARCHAR(32),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_qa_audit_action CHECK (action IN ('QA_ASK', 'QA_FOLLOW_UP', 'QA_REJECT', 'QA_DEGRADE', 'QA_STREAM_ABORT', 'QA_FORBIDDEN', 'QA_EXPORT', 'QA_PURGE', 'QA_CLOSE_THREAD'))
);

CREATE INDEX IF NOT EXISTS idx_qa_audit_question ON qa_audit_event (question_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_qa_audit_actor ON qa_audit_event (actor_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS diagnosis_retention_run (
    id BIGSERIAL PRIMARY KEY,
    batch_no INT NOT NULL,
    deleted_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    CONSTRAINT ck_retention_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_retention_run_started ON diagnosis_retention_run (started_at DESC);