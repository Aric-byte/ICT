package com.cloudlabcopilot.knowledge.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCard {

    private Long id;
    private String techStack;
    private String title;
    private String symptom;
    private String safeNextStep;
    private String verificationMethod;
    private String caseOrigin;
    private String sourceType;
    private String sourceRef;
    private List<String> applicableVersions;
    private String status;
    private String reviewStatus;
    private List<ErrorPattern> errorPatterns;
    private List<Cause> causes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPattern {
        private String pattern;
        private String matchType;
        private int sortNo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cause {
        private String text;
        private String confidence;
        private int sortNo;
    }
}