-- V5 知识库（3 张表）
CREATE TABLE IF NOT EXISTS knowledge_card (
    id BIGSERIAL PRIMARY KEY,
    tech_stack VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    symptom VARCHAR(2000) NOT NULL,
    safe_next_step VARCHAR(1000) NOT NULL,
    verification_method VARCHAR(1000),
    case_origin VARCHAR(16) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(256) NOT NULL,
    applicable_versions JSONB,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(symptom, '') || ' ' || coalesce(safe_next_step, ''))
    ) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_knowledge_tech_stack CHECK (tech_stack IN ('OPENGAUSS', 'MYSQL', 'MYBATIS', 'OTHER')),
    CONSTRAINT ck_knowledge_case_origin CHECK (case_origin IN ('REAL', 'DERIVED')),
    CONSTRAINT ck_knowledge_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    CONSTRAINT ck_knowledge_review_status CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT uk_knowledge_card_source UNIQUE (source_type, source_ref, title)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_card_status ON knowledge_card (status, review_status, tech_stack);
CREATE INDEX IF NOT EXISTS idx_knowledge_card_search ON knowledge_card USING GIN (search_vector);

CREATE TABLE IF NOT EXISTS knowledge_card_error_pattern (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES knowledge_card(id) ON DELETE CASCADE,
    pattern VARCHAR(500) NOT NULL,
    match_type VARCHAR(16) NOT NULL DEFAULT 'KEYWORD',
    sort_no INT NOT NULL DEFAULT 1,
    CONSTRAINT ck_error_pattern_match_type CHECK (match_type IN ('KEYWORD', 'REGEX', 'CODE'))
);

CREATE INDEX IF NOT EXISTS idx_card_error_pattern_card ON knowledge_card_error_pattern (card_id, sort_no);

CREATE TABLE IF NOT EXISTS knowledge_card_cause (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES knowledge_card(id) ON DELETE CASCADE,
    cause_text VARCHAR(1000) NOT NULL,
    confidence VARCHAR(8) NOT NULL DEFAULT 'MEDIUM',
    sort_no INT NOT NULL DEFAULT 1,
    CONSTRAINT ck_card_cause_confidence CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX IF NOT EXISTS idx_card_cause_card ON knowledge_card_cause (card_id, sort_no);