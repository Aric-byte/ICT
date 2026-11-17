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
public class ExecutionSession {

    private String sessionId;
    private Long labId;
    private Long versionId;
    private int versionNo;
    private String operatorId;
    private SessionStatus status;
    private int currentStepNo;
    private int totalSteps;
    private boolean blockedFlag;
    private BlockReasonCategory blockReasonCategory;
    private String pauseReason;
    private String abandonReason;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActiveAt;
    private OffsetDateTime endedAt;
    private boolean preconditionConfirmed;
    private OffsetDateTime preconditionConfirmedAt;
    private String preconditionSnapshot;
    private int rowVersion;

    public boolean isTerminal() {
        return status == SessionStatus.COMPLETED || status == SessionStatus.ABANDONED;
    }
}