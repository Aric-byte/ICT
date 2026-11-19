-- V3 lab 表（实验定义、版本、步骤、前置条件）

CREATE TABLE IF NOT EXISTS lab_definition (
    id BIGSERIAL PRIMARY KEY,
    lab_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    tech_stack VARCHAR(16) NOT NULL,
    custom_tech_stack VARCHAR(64),
    difficulty VARCHAR(16) NOT NULL,
    objective VARCHAR(2000) NOT NULL,
    estimated_minutes INT,
    tags JSONB NOT NULL DEFAULT '[]',
    status VARCHAR(16) NOT NULL,
    effective_version_no INT,
    owner_id VARCHAR(64) NOT NULL,
    internal_note VARCHAR(500),
    draft_steps JSONB NOT NULL DEFAULT '[]',
    draft_preconditions JSONB NOT NULL DEFAULT '[]',
    row_version INT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_lab_definition_tech_stack CHECK (tech_stack IN ('OPEN_GAUSS','MYSQL','MYBATIS','OTHER')),
    CONSTRAINT ck_lab_definition_difficulty CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    CONSTRAINT ck_lab_definition_status CHECK (status IN ('DRAFT','PUBLISHED','UNPUBLISHED')),
    CONSTRAINT ck_lab_definition_estimated CHECK (estimated_minutes IS NULL OR (estimated_minutes >= 1 AND estimated_minutes <= 1440)),
    CONSTRAINT ck_lab_definition_effective CHECK (effective_version_no IS NULL OR effective_version_no > 0),
    CONSTRAINT ck_lab_definition_other_tech CHECK (tech_stack <> 'OTHER' OR custom_tech_stack IS NOT NULL),
    CONSTRAINT ck_lab_definition_published_version CHECK (status <> 'PUBLISHED' OR effective_version_no IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_lab_definition_catalog ON lab_definition (status, deleted, updated_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_lab_definition_tags ON lab_definition USING GIN (tags jsonb_path_ops);

CREATE TABLE IF NOT EXISTS lab_version (
    id BIGSERIAL PRIMARY KEY,
    lab_id BIGINT NOT NULL REFERENCES lab_definition(id) ON DELETE RESTRICT,
    version_no INT NOT NULL CHECK (version_no >= 1),
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    name VARCHAR(120) NOT NULL,
    tech_stack VARCHAR(16) NOT NULL CHECK (tech_stack IN ('OPEN_GAUSS','MYSQL','MYBATIS','OTHER')),
    custom_tech_stack VARCHAR(64),
    difficulty VARCHAR(16) NOT NULL CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    objective VARCHAR(2000) NOT NULL,
    estimated_minutes INT,
    tags JSONB NOT NULL,
    change_note VARCHAR(500),
    snapshot_hash CHAR(64) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    published_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lab_version_no UNIQUE (lab_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_lab_version_lab ON lab_version (lab_id, version_no DESC);

CREATE TABLE IF NOT EXISTS lab_step (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES lab_version(id) ON DELETE RESTRICT,
    step_no INT NOT NULL CHECK (step_no >= 1 AND step_no <= 50),
    title VARCHAR(120) NOT NULL CHECK (char_length(title) BETWEEN 1 AND 120),
    instruction VARCHAR(5000) NOT NULL CHECK (char_length(instruction) BETWEEN 1 AND 5000),
    completion_criteria VARCHAR(1000) NOT NULL CHECK (char_length(completion_criteria) BETWEEN 1 AND 1000),
    hint VARCHAR(1000),
    CONSTRAINT uk_lab_step_no UNIQUE (version_id, step_no)
);

CREATE INDEX IF NOT EXISTS idx_lab_step_version ON lab_step (version_id, step_no);

CREATE TABLE IF NOT EXISTS lab_precondition (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES lab_version(id) ON DELETE RESTRICT,
    sort_no INT NOT NULL CHECK (sort_no >= 1),
    type VARCHAR(16) NOT NULL CHECK (type IN ('ENV','KNOWLEDGE','RESOURCE')),
    level VARCHAR(16) NOT NULL CHECK (level IN ('REQUIRED','RECOMMENDED')),
    description VARCHAR(500) NOT NULL CHECK (char_length(description) BETWEEN 1 AND 500),
    check_mode VARCHAR(16) NOT NULL DEFAULT 'SELF_CONFIRM' CHECK (check_mode = 'SELF_CONFIRM'),
    CONSTRAINT uk_lab_precondition_sort UNIQUE (version_id, sort_no)
);

CREATE INDEX IF NOT EXISTS idx_lab_precondition_version ON lab_precondition (version_id, sort_no);