package com.cloudlabcopilot.session.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SessionDtos {

        private SessionDtos() {
        }

        public record StartSessionRequest(
                        @NotNull Long labId,
                        @NotNull @Min(1) Integer versionNo,
                        Boolean preconditionConfirmed,
                        List<Integer> confirmedPreconditionIds) {
        }

        public record AdvanceRequest(@NotNull @Min(1) Integer expectStepNo) {
        }

        public record PauseRequest(@Size(max = 200) String pauseReason) {
        }

        public record ResumeRequest() {
        }

        public record AbandonRequest(@NotBlank @Size(min = 5, max = 500) String reason) {
        }

        public record BlockRequest(@NotBlank String category) {
        }

        public record UnblockRequest() {
        }

        public record TextSubmitRequest(
                        @NotBlank @Size(max = 20000) String text, @Size(max = 2000) String summary) {
        }

        public record SessionView(
                        String sessionId,
                        Long labId,
                        Integer versionNo,
                        String status,
                        String stateLabel,
                        Integer currentStepNo,
                        Integer totalSteps,
                        Boolean blockedFlag,
                        String startedAt,
                        String lastActiveAt,
                        Boolean overdueFlag,
                        boolean duplicated) {
        }

        public record TimelineItemView(
                        String recordId,
                        String eventType,
                        String occurredAt,
                        String summary,
                        Integer stepNo,
                        String result) {
        }

        public record ContextView(
                        String sessionId,
                        Long labId,
                        Integer versionNo,
                        String objective,
                        Integer currentStepNo,
                        Integer totalSteps,
                        List<Integer> completedSteps,
                        List<TimelineItemView> recentRecords,
                        String stateLabel,
                        Boolean blockedFlag,
                        Boolean overdueFlag,
                        boolean readOnly) {
        }

        public record EnvironmentRegisterRequest(
                        @NotBlank @Size(min = 1, max = 512) String value) {
        }

        public record EnvironmentItemView(
                        String key, String value, String capturedAt, String updatedAt) {
        }

        public record SessionListItemView(
                        String sessionId,
                        Long labId,
                        Integer versionNo,
                        String status,
                        Integer currentStepNo,
                        Integer totalSteps,
                        String startedAt,
                        String lastActiveAt) {
        }
}