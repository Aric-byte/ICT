-- V8 索引补齐与默认配置
CREATE INDEX IF NOT EXISTS idx_qa_history_status ON diagnosis_question (operator_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_answer_status ON diagnosis_answer (generation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_thread_teacher ON diagnosis_thread (lab_id, last_active_at DESC);
CREATE INDEX IF NOT EXISTS idx_question_retention_scan ON diagnosis_question (retention_expire_at);

INSERT INTO app_config (config_key, config_value, description) VALUES
    ('diagnosis.retention.days', '90', '问答历史保留天数'),
    ('diagnosis.stream.version', '1.0', 'SSE 流式协议版本'),
    ('diagnosis.provider.prompt-version', 'diag-p1', '提示模板版本'),
    ('diagnosis.vector.enabled', 'false', '向量检索开关'),
    ('diagnosis.provider.unverified', 'endpoint,model,auth', '待核实配置项')
ON CONFLICT (config_key) DO NOTHING;