package com.cloudlabcopilot.lab.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class LabDtos {

    private LabDtos() {
    }

    public record StepDto(
            @Min(1) int stepNo,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 5000) String instruction,
            @NotBlank @Size(max = 1000) String completionCriteria,
            @Size(max = 1000) String hint) {
    }

    public record PreconditionDto(
            @Min(1) int sortNo,
            @NotBlank String type,
            @NotBlank String level,
            @NotBlank @Size(max = 500) String description) {
    }

    public record CreateLabRequest(
            @NotBlank @Size(min = 3, max = 64) String labCode,
            @NotBlank @Size(max = 120) String name,
            @NotBlank String techStack,
            @Size(max = 64) String customTechStack,
            @NotBlank String difficulty,
            @NotBlank @Size(max = 2000) String objective,
            @Min(1) @Max(1440) Integer estimatedMinutes,
            List<String> tags,
            @Size(max = 500) String internalNote) {
    }

    public record UpdateLabRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String techStack,
            @Size(max = 64) String customTechStack,
            @NotBlank String difficulty,
            @NotBlank @Size(max = 2000) String objective,
            @Min(1) @Max(1440) Integer estimatedMinutes,
            List<String> tags,
            @Size(max = 500) String internalNote,
            @NotNull Integer rowVersion) {
    }

    public record SaveStepsRequest(@NotNull List<StepDto> steps) {
    }

    public record SavePreconditionsRequest(@NotNull List<PreconditionDto> preconditions) {
    }

    public record SwitchVersionRequest(@Min(1) int versionNo) {
    }

    public record StepView(
            int stepNo, String title, String instruction, String completionCriteria, String hint) {
    }

    public record PreconditionView(int sortNo, String type, String level, String description) {
    }

    public record LabSummaryView(
            String labCode,
            String name,
            String techStack,
            String difficulty,
            String objective,
            String status,
            Integer effectiveVersionNo,
            List<String> tags) {
    }

    public record LabDetailView(
            Long labId,
            String labCode,
            String name,
            String techStack,
            String difficulty,
            String objective,
            Integer estimatedMinutes,
            List<String> tags,
            Integer versionNo,
            List<StepView> steps,
            List<PreconditionView> preconditions) {
    }

    public record StartEligibilityView(
            String labCode,
            Integer versionNo,
            boolean canStart,
            String activeSessionId,
            List<String> blockReasons,
            List<PreconditionView> preconditions) {
    }

    public record PagedLabView(List<LabSummaryView> items, long total, int page, int size) {
    }
}