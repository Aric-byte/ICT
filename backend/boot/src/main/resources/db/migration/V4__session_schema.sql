-- V4 session 表（会话、步骤进度、执行记录、环境信息、状态快照）

CREATE TABLE IF NOT EXISTS lab_session (
    session_id VARCHAR(32) PRIMARY KEY,
    lab_id BIGINT NOT NULL REFERENCES lab_definition(id) ON DELETE RESTRICT,
    version_id BIGINT NOT NULL REFERENCES lab_version(id) ON DELETE RESTRICT,
    version_no INT NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    current_step_no INT NOT NULL CHECK (current_step_no >= 1),
    total_steps INT NOT NULL CHECK (total_steps BETWEEN 1 AND 50),
    blocked_flag BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason_category VARCHAR(16),
    pause_reason VARCHAR(200),
    abandon_reason VARCHAR(500),
    started_at TIMESTAMPTZ NOT NULL,
    last_active_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    precondition_confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    precondition_confirmed_at TIMESTAMPTZ NOT NULL,
    precondition_snapshot JSONB NOT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_session_status CHECK (status IN ('IN_PROGRESS','PAUSED','COMPLETED','ABANDONED')),
    CONSTRAINT ck_session_block_reason_category CHECK (block_reason_category IS NULL OR block_reason_category IN ('SYNTAX','ENVIRONMENT','CONFIG','PERMISSION','UNKNOWN')),
    CONSTRAINT ck_session_precondition_confirmed CHECK (precondition_confirmed = TRUE),
    CONSTRAINT ck_session_terminal_ended CHECK ((status IN ('COMPLETED','ABANDONED')) = (ended_at IS NOT NULL)),
    CONSTRAINT ck_session_blocked_only_active CHECK (blocked_flag = FALSE OR status = 'IN_PROGRESS'),
    CONSTRAINT ck_session_block_reason CHECK (blocked_flag = FALSE OR block_reason_category IS NOT NULL),
    CONSTRAINT ck_session_abandon_reason CHECK (status <> 'ABANDONED' OR char_length(abandon_reason) >= 5)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_session_active ON lab_session (lab_id, operator_id) WHERE status IN ('IN_PROGRESS','PAUSED');
CREATE INDEX IF NOT EXISTS idx_session_operator ON lab_session (operator_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_session_teacher ON lab_session (lab_id, started_at DESC, session_id);
CREATE INDEX IF NOT EXISTS idx_session_overdue ON lab_session (status, last_active_at);

CREATE TABLE IF NOT EXISTS lab_session_step_progress (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL REFERENCES lab_session(session_id) ON DELETE RESTRICT,
    step_no INT NOT NULL CHECK (step_no >= 1),
    attempt_no INT NOT NULL CHECK (attempt_no >= 1),
    completed_at TIMESTAMPTZ NOT NULL,
    completed_by VARCHAR(64) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT uk_step_progress UNIQUE (session_id, step_no, attempt_no)
);

CREATE INDEX IF NOT EXISTS idx_step_progress_session ON lab_session_step_progress (session_id, step_no);

CREATE TABLE IF NOT EXISTS lab_execution_record (
    id BIGSERIAL PRIMARY KEY,
    record_id VARCHAR(32) NOT NULL UNIQUE,
    session_id VARCHAR(32) NOT NULL REFERENCES lab_session(session_id) ON DELETE RESTRICT,
    event_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    source VARCHAR(16) NOT NULL,
    step_no INT,
    summary VARCHAR(2000) NOT NULL CHECK (char_length(summary) BETWEEN 1 AND 2000),
    content_text TEXT CHECK (content_text IS NULL OR char_length(content_text) <= 20000),
    result VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_record_event_type CHECK (event_type IN ('SESSION_STARTED','STEP_COMPLETED','STEP_ROLLED_BACK','BLOCKED','UNBLOCKED','PAUSED','RESUMED','SESSION_COMPLETED','SESSION_ABANDONED','ENVIRONMENT_REGISTERED','TEXT_SUBMITTED')),
    CONSTRAINT ck_record_source CHECK (source IN ('STUDENT','TEACHER','SYSTEM')),
    CONSTRAINT ck_record_result CHECK (result IN ('SUCCESS','FAILED','UNKNOWN'))
);

CREATE INDEX IF NOT EXISTS idx_record_timeline ON lab_execution_record (session_id, occurred_at, id);
CREATE INDEX IF NOT EXISTS idx_record_type ON lab_execution_record (session_id, event_type, occurred_at, id);
CREATE INDEX IF NOT EXISTS idx_record_step ON lab_execution_record (session_id, step_no, occurred_at);

CREATE TABLE IF NOT EXISTS lab_session_environment_item (
    session_id VARCHAR(32) NOT NULL REFERENCES lab_session(session_id) ON DELETE RESTRICT,
    env_key VARCHAR(64) NOT NULL CHECK (char_length(env_key) BETWEEN 1 AND 64),
    env_value VARCHAR(512) NOT NULL CHECK (char_length(env_value) BETWEEN 1 AND 512),
    captured_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_session_env PRIMARY KEY (session_id, env_key)
);

CREATE INDEX IF NOT EXISTS idx_env_session ON lab_session_environment_item (session_id, captured_at);

CREATE TABLE IF NOT EXISTS lab_session_state_snapshot (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL REFERENCES lab_session(session_id) ON DELETE RESTRICT,
    status_before VARCHAR(16) NOT NULL,
    status_after VARCHAR(16) NOT NULL,
    step_before INT NOT NULL,
    step_after INT NOT NULL,
    blocked_after BOOLEAN NOT NULL,
    state_label VARCHAR(16) NOT NULL,
    trigger_event VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_snapshot_session ON lab_session_state_snapshot (session_id, computed_at DESC);