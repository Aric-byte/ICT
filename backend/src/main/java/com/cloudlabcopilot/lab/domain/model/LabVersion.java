package com.cloudlabcopilot.lab.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabVersion {

    private Long id;
    private Long labId;
    private int versionNo;
    private VersionStatus status;
    private String name;
    private TechStack techStack;
    private String customTechStack;
    private Difficulty difficulty;
    private String objective;
    private Integer estimatedMinutes;
    private List<String> tags;
    private String changeNote;
    private String snapshotHash;
    private OffsetDateTime publishedAt;
    private String publishedBy;
}