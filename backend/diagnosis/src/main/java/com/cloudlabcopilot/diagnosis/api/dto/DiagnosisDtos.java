package com.cloudlabcopilot.diagnosis.api.dto;

import java.util.List;

public final class DiagnosisDtos {

    private DiagnosisDtos() {
    }

    public record AskRequest(
            String question,
            String labCode,
            String sessionId,
            String techStack,
            String pastedText) {
    }

    public record CitationView(String sourceType, String sourceRef, String title, String excerpt) {
    }

    public record CauseView(String text, String confidence, List<CitationView> citations) {
    }

    public record NextStepView(
            String action, String expectedObservation, String riskLevel, List<CitationView> citations) {
    }

    public record AnswerView(
            String questionId,
            String generationStatus,
            String restate,
            String explanation,
            List<CauseView> causes,
            NextStepView nextStep,
            List<CitationView> citations,
            String confidence,
            String confidenceReason,
            List<String> evidenceGaps,
            String safetyNotice,
            String techStack,
            String contextCompleteness) {
    }
}