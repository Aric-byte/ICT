package com.cloudlabcopilot.knowledge.api.dto;

import java.util.List;

public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record ErrorPatternDto(String pattern, String matchType, int sortNo) {
    }

    public record CauseDto(String text, String confidence, int sortNo) {
    }

    public record CreateCardRequest(
            String techStack,
            String title,
            String symptom,
            String safeNextStep,
            String verificationMethod,
            String caseOrigin,
            String sourceType,
            String sourceRef,
            List<String> applicableVersions,
            List<ErrorPatternDto> errorPatterns,
            List<CauseDto> causes) {
    }

    public record UpdateStatusRequest(String status, String reviewStatus) {
    }

    public record CardView(
            Long id,
            String techStack,
            String title,
            String symptom,
            String safeNextStep,
            String status,
            String reviewStatus,
            List<String> applicableVersions) {
    }

    public record PagedCards(List<CardView> items, long total, int page, int pageSize) {
    }
}