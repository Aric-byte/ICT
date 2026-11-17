package com.cloudlabcopilot.lab.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabDefinition {

    private Long id;
    private String labCode;
    private String name;
    private TechStack techStack;
    private String customTechStack;
    private Difficulty difficulty;
    private String objective;
    private Integer estimatedMinutes;
    private List<String> tags;
    private LabStatus status;
    private Integer effectiveVersionNo;
    private String ownerId;
    private String internalNote;
    private List<LabStep> draftSteps;
    private List<Precondition> draftPreconditions;
    private Integer rowVersion;
}