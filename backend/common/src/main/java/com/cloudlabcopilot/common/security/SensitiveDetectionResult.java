package com.cloudlabcopilot.common.security;

public record SensitiveDetectionResult(boolean hit, SensitiveCategory category, int contentLength) {

    public static SensitiveDetectionResult clean(int contentLength) {
        return new SensitiveDetectionResult(false, null, contentLength);
    }

    public static SensitiveDetectionResult hit(SensitiveCategory category, int contentLength) {
        return new SensitiveDetectionResult(true, category, contentLength);
    }
}