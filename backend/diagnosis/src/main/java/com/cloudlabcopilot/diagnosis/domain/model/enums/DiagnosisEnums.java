package com.cloudlabcopilot.diagnosis.domain.model.enums;

public final class DiagnosisEnums {

    private DiagnosisEnums() {
    }

    public enum TechStack {
        OPENGAUSS, MYSQL, MYBATIS, OTHER
    }

    public enum GenerationStatus {
        NORMAL, KNOWLEDGE_FALLBACK, INSUFFICIENT_EVIDENCE, UNAVAILABLE, SAFETY_REJECTED
    }

    public enum StreamState {
        NOT_STREAMED, COMPLETED, ABORTED, FAILED
    }

    public enum ContextCompleteness {
        FULL, PARTIAL, NONE
    }

    public enum ConfidenceLevel {
        HIGH, MEDIUM, LOW
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public enum ProviderType {
        MAAS, OPENAI_COMPATIBLE, NONE
    }

    public enum CitationSourceType {
        KNOWLEDGE_CARD, LAB_STEP, CONTEXT_FACT
    }

    public enum HitChannel {
        KEYWORD, VECTOR, BOTH
    }

    public enum Applicability {
        APPLICABLE, VERSION_MISMATCH, CROSS_STACK
    }

    public enum ThreadStatus {
        OPEN, CLOSED, EXPIRED
    }

    public enum StreamSection {
        RESTATE, EXPLANATION, CAUSES, NEXT_STEP
    }

    public enum DegradeReasonCode {
        AI_UNCONFIGURED,
        AI_TIMEOUT,
        AI_ERROR,
        AI_RATE_LIMITED,
        OUTPUT_INVALID,
        CITATION_INVALID,
        SAFETY_REJECTED,
        RETRIEVAL_UNAVAILABLE
    }
}