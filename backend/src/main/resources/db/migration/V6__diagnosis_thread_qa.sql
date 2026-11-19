-- V6 诊断线程与问答（4 张表）
CREATE TABLE IF NOT EXISTS diagnosis_thread (
    thread_id VARCHAR(32) PRIMARY KEY,
    lab_id BIGINT,
    operator_id VARCHAR(64) NOT NULL,
    title VARCHAR(200),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    turn_count INT NOT NULL DEFAULT 0,
    active_question_id VARCHAR(32),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_thread_status CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED')),
    CONSTRAINT ck_thread_turn_count CHECK (turn_count >= 0 AND turn_count <= 20)
);

CREATE INDEX IF NOT EXISTS idx_thread_operator ON diagnosis_thread (operator_id, last_active_at DESC);
CREATE INDEX IF NOT EXISTS idx_thread_teacher ON diagnosis_thread (lab_id, last_active_at DESC);
CREATE INDEX IF NOT EXISTS idx_thread_status ON diagnosis_thread (status, expires_at);

CREATE TABLE IF NOT EXISTS diagnosis_question (
    question_id VARCHAR(32) PRIMARY KEY,
    thread_id VARCHAR(32),
    follow_up_index INT NOT NULL DEFAULT 0,
    operator_id VARCHAR(64) NOT NULL,
    lab_id BIGINT,
    session_id VARCHAR(32),
    lab_code VARCHAR(64),
    tech_stack VARCHAR(16),
    question_text VARCHAR(20000) NOT NULL,
    pasted_text VARCHAR(20000),
    context_completeness VARCHAR(16),
    idempotency_key VARCHAR(64),
    retention_expire_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_question_turn UNIQUE (thread_id, follow_up_index)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_question_idem ON diagnosis_question (operator_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_question_retention_scan ON diagnosis_question (retention_expire_at);
CREATE INDEX IF NOT EXISTS idx_question_thread ON diagnosis_question (thread_id, follow_up_index);
CREATE INDEX IF NOT EXISTS idx_qa_history_status ON diagnosis_question (operator_id, created_at DESC);

CREATE TABLE IF NOT EXISTS diagnosis_answer (
    answer_id VARCHAR(32) PRIMARY KEY,
    question_id VARCHAR(32) NOT NULL,
    generation_status VARCHAR(32) NOT NULL,
    stream_state VARCHAR(16) NOT NULL DEFAULT 'NOT_STREAMED',
    restate VARCHAR(200),
    explanation VARCHAR(2000),
    causes JSONB NOT NULL DEFAULT '[]',
    next_steps JSONB NOT NULL DEFAULT '[]',
    evidence_gaps JSONB NOT NULL DEFAULT '[]',
    confidence VARCHAR(8),
    safety_notice VARCHAR(1000),
    context_digest JSONB,
    prompt_version VARCHAR(32),
    model_id VARCHAR(128),
    citation_validated_count INT NOT NULL DEFAULT 0,
    citation_invalid_count INT NOT NULL DEFAULT 0,
    elapsed_ms INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_answer_generation_status CHECK (generation_status IN ('NORMAL', 'KNOWLEDGE_FALLBACK', 'INSUFFICIENT_EVIDENCE', 'UNAVAILABLE', 'SAFETY_REJECTED')),
    CONSTRAINT ck_answer_stream_state CHECK (stream_state IN ('NOT_STREAMED', 'COMPLETED', 'ABORTED', 'FAILED')),
    CONSTRAINT ck_answer_confidence CHECK (confidence IS NULL OR confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_answer_causes_len CHECK (jsonb_array_length(causes) <= 5),
    CONSTRAINT ck_answer_next_steps_len CHECK (jsonb_array_length(next_steps) <= 3)
);

CREATE INDEX IF NOT EXISTS idx_answer_question ON diagnosis_answer (question_id);
CREATE INDEX IF NOT EXISTS idx_answer_status ON diagnosis_answer (generation_status, created_at DESC);

CREATE TABLE IF NOT EXISTS diagnosis_citation (
    id BIGSERIAL PRIMARY KEY,
    answer_id VARCHAR(32) NOT NULL,
    cause_index INT,
    source_type VARCHAR(16) NOT NULL,
    source_ref VARCHAR(128) NOT NULL,
    title VARCHAR(200) NOT NULL,
    excerpt VARCHAR(200),
    valid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_citation_source_type CHECK (source_type IN ('KNOWLEDGE_CARD', 'LAB_STEP', 'CONTEXT_FACT')),
    CONSTRAINT uk_citation UNIQUE (answer_id, source_type, source_ref)
);

CREATE INDEX IF NOT EXISTS idx_citation_answer ON diagnosis_citation (answer_id);