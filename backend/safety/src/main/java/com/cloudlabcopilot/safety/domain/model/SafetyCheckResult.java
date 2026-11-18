package com.cloudlabcopilot.safety.domain.model;

public record SafetyCheckResult(
        boolean dangerous,
        String category,
        String riskLevel,
        String description,
        String safeAlternative) {

    public static SafetyCheckResult clean() {
        return new SafetyCheckResult(false, null, "LOW", null, null);
    }
}