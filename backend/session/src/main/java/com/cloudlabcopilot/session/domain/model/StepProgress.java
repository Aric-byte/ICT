package com.cloudlabcopilot.session.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepProgress {

    private Long id;
    private String sessionId;
    private int stepNo;
    private int attemptNo;
    private OffsetDateTime completedAt;
    private String completedBy;
    private String note;
}