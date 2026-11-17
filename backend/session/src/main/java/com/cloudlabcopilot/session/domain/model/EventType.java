package com.cloudlabcopilot.session.domain.model;

public enum EventType {
    SESSION_STARTED,
    STEP_COMPLETED,
    STEP_ROLLED_BACK,
    BLOCKED,
    UNBLOCKED,
    PAUSED,
    RESUMED,
    SESSION_COMPLETED,
    SESSION_ABANDONED,
    ENVIRONMENT_REGISTERED,
    TEXT_SUBMITTED
}