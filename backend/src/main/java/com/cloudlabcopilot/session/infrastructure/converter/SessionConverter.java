package com.cloudlabcopilot.session.infrastructure.converter;

import com.cloudlabcopilot.session.domain.model.BlockReasonCategory;
import com.cloudlabcopilot.session.domain.model.EventType;
import com.cloudlabcopilot.session.domain.model.ExecutionRecord;
import com.cloudlabcopilot.session.domain.model.ExecutionSession;
import com.cloudlabcopilot.session.domain.model.RecordResult;
import com.cloudlabcopilot.session.domain.model.RecordSource;
import com.cloudlabcopilot.session.domain.model.SessionStatus;
import com.cloudlabcopilot.session.domain.model.StepProgress;
import com.cloudlabcopilot.session.infrastructure.entity.ExecutionRecordEntity;
import com.cloudlabcopilot.session.infrastructure.entity.SessionEntity;
import com.cloudlabcopilot.session.infrastructure.entity.StepProgressEntity;
import org.springframework.stereotype.Component;

@Component
public class SessionConverter {

    public ExecutionSession toDomain(SessionEntity e) {
        return ExecutionSession.builder()
                .sessionId(e.getSessionId())
                .labId(e.getLabId())
                .versionId(e.getVersionId())
                .versionNo(e.getVersionNo() == null ? 0 : e.getVersionNo())
                .operatorId(e.getOperatorId())
                .status(e.getStatus() == null ? null : SessionStatus.valueOf(e.getStatus()))
                .currentStepNo(e.getCurrentStepNo() == null ? 0 : e.getCurrentStepNo())
                .totalSteps(e.getTotalSteps() == null ? 0 : e.getTotalSteps())
                .blockedFlag(Boolean.TRUE.equals(e.getBlockedFlag()))
                .blockReasonCategory(
                        e.getBlockReasonCategory() == null
                                ? null
                                : BlockReasonCategory.valueOf(e.getBlockReasonCategory()))
                .pauseReason(e.getPauseReason())
                .abandonReason(e.getAbandonReason())
                .startedAt(e.getStartedAt())
                .lastActiveAt(e.getLastActiveAt())
                .endedAt(e.getEndedAt())
                .preconditionConfirmed(Boolean.TRUE.equals(e.getPreconditionConfirmed()))
                .preconditionConfirmedAt(e.getPreconditionConfirmedAt())
                .preconditionSnapshot(e.getPreconditionSnapshot())
                .rowVersion(e.getRowVersion() == null ? 0 : e.getRowVersion())
                .build();
    }

    public SessionEntity toEntity(ExecutionSession s) {
        SessionEntity e = new SessionEntity();
        e.setSessionId(s.getSessionId());
        e.setLabId(s.getLabId());
        e.setVersionId(s.getVersionId());
        e.setVersionNo(s.getVersionNo());
        e.setOperatorId(s.getOperatorId());
        e.setStatus(s.getStatus() == null ? null : s.getStatus().name());
        e.setCurrentStepNo(s.getCurrentStepNo());
        e.setTotalSteps(s.getTotalSteps());
        e.setBlockedFlag(s.isBlockedFlag());
        e.setBlockReasonCategory(
                s.getBlockReasonCategory() == null ? null : s.getBlockReasonCategory().name());
        e.setPauseReason(s.getPauseReason());
        e.setAbandonReason(s.getAbandonReason());
        e.setStartedAt(s.getStartedAt());
        e.setLastActiveAt(s.getLastActiveAt());
        e.setEndedAt(s.getEndedAt());
        e.setPreconditionConfirmed(s.isPreconditionConfirmed());
        e.setPreconditionConfirmedAt(s.getPreconditionConfirmedAt());
        e.setPreconditionSnapshot(s.getPreconditionSnapshot());
        e.setRowVersion(s.getRowVersion());
        return e;
    }

    public ExecutionRecord toDomain(ExecutionRecordEntity e) {
        return ExecutionRecord.builder()
                .id(e.getId())
                .recordId(e.getRecordId())
                .sessionId(e.getSessionId())
                .eventType(e.getEventType() == null ? null : EventType.valueOf(e.getEventType()))
                .occurredAt(e.getOccurredAt())
                .operatorId(e.getOperatorId())
                .source(e.getSource() == null ? null : RecordSource.valueOf(e.getSource()))
                .stepNo(e.getStepNo())
                .summary(e.getSummary())
                .contentText(e.getContentText())
                .result(e.getResult() == null ? null : RecordResult.valueOf(e.getResult()))
                .build();
    }

    public ExecutionRecordEntity toEntity(ExecutionRecord r) {
        ExecutionRecordEntity e = new ExecutionRecordEntity();
        e.setId(r.getId());
        e.setRecordId(r.getRecordId());
        e.setSessionId(r.getSessionId());
        e.setEventType(r.getEventType() == null ? null : r.getEventType().name());
        e.setOccurredAt(r.getOccurredAt());
        e.setOperatorId(r.getOperatorId());
        e.setSource(r.getSource() == null ? null : r.getSource().name());
        e.setStepNo(r.getStepNo());
        e.setSummary(r.getSummary());
        e.setContentText(r.getContentText());
        e.setResult(r.getResult() == null ? null : r.getResult().name());
        return e;
    }

    public StepProgress toDomain(StepProgressEntity e) {
        return StepProgress.builder()
                .id(e.getId())
                .sessionId(e.getSessionId())
                .stepNo(e.getStepNo() == null ? 0 : e.getStepNo())
                .attemptNo(e.getAttemptNo() == null ? 0 : e.getAttemptNo())
                .completedAt(e.getCompletedAt())
                .completedBy(e.getCompletedBy())
                .note(e.getNote())
                .build();
    }
}