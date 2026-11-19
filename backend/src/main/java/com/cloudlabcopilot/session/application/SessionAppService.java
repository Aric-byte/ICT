package com.cloudlabcopilot.session.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.id.PublicIdGenerator;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.domain.port.LabSnapshot;
import com.cloudlabcopilot.lab.domain.port.LabSnapshotQueryPort;
import com.cloudlabcopilot.session.api.dto.SessionDtos.AbandonRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.ContextView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.SessionView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.StartSessionRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.TextSubmitRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.EnvironmentItemView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.EnvironmentRegisterRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.SessionListItemView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.TimelineItemView;
import com.cloudlabcopilot.session.domain.model.BlockReasonCategory;
import com.cloudlabcopilot.session.domain.model.EnvironmentItem;
import com.cloudlabcopilot.session.domain.model.EventType;
import com.cloudlabcopilot.session.domain.model.ExecutionRecord;
import com.cloudlabcopilot.session.domain.model.ExecutionSession;
import com.cloudlabcopilot.session.domain.model.RecordResult;
import com.cloudlabcopilot.session.domain.model.RecordSource;
import com.cloudlabcopilot.session.domain.model.SessionStatus;
import com.cloudlabcopilot.session.domain.statemachine.SessionStateMachine;
import com.cloudlabcopilot.session.infrastructure.converter.SessionConverter;
import com.cloudlabcopilot.session.infrastructure.entity.EnvironmentItemEntity;
import com.cloudlabcopilot.session.infrastructure.entity.ExecutionRecordEntity;
import com.cloudlabcopilot.session.infrastructure.entity.SessionEntity;
import com.cloudlabcopilot.session.infrastructure.entity.StepProgressEntity;
import com.cloudlabcopilot.session.infrastructure.entity.StateSnapshotEntity;
import com.cloudlabcopilot.session.infrastructure.mapper.EnvironmentItemMapper;
import com.cloudlabcopilot.session.infrastructure.mapper.ExecutionRecordMapper;
import com.cloudlabcopilot.session.infrastructure.mapper.SessionMapper;
import com.cloudlabcopilot.session.infrastructure.mapper.StepProgressMapper;
import com.cloudlabcopilot.session.infrastructure.mapper.StateSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SessionAppService {

    private static final int MAX_TEXT_LENGTH = 20000;

    private final SessionMapper sessionMapper;
    private final StepProgressMapper stepProgressMapper;
    private final ExecutionRecordMapper executionRecordMapper;
    private final StateSnapshotMapper stateSnapshotMapper;
    private final SessionConverter converter;
    private final SessionStateMachine stateMachine;
    private final LabSnapshotQueryPort labSnapshotQueryPort;
    private final EnvironmentItemMapper environmentItemMapper;

    public SessionAppService(
            SessionMapper sessionMapper,
            StepProgressMapper stepProgressMapper,
            ExecutionRecordMapper executionRecordMapper,
            StateSnapshotMapper stateSnapshotMapper,
            SessionConverter converter,
            SessionStateMachine stateMachine,
            LabSnapshotQueryPort labSnapshotQueryPort,
            EnvironmentItemMapper environmentItemMapper) {
        this.sessionMapper = sessionMapper;
        this.stepProgressMapper = stepProgressMapper;
        this.executionRecordMapper = executionRecordMapper;
        this.stateSnapshotMapper = stateSnapshotMapper;
        this.converter = converter;
        this.stateMachine = stateMachine;
        this.labSnapshotQueryPort = labSnapshotQueryPort;
        this.environmentItemMapper = environmentItemMapper;
    }

    @Transactional
    public SessionView start(StartSessionRequest req, String operatorId) {
        if (!Boolean.TRUE.equals(req.preconditionConfirmed())) {
            throw new BusinessException(ErrorCode.PRECONDITION_NOT_CONFIRMED);
        }
        LabSnapshot snapshot = labSnapshotQueryPort.loadPublishedSnapshot(req.labId(), req.versionNo());
        if (snapshot.totalSteps() <= 0) {
            throw new BusinessException(ErrorCode.LAB_NOT_STARTABLE);
        }
        long active = sessionMapper.countActive(req.labId(), operatorId);
        if (active > 0) {
            throw new BusinessException(ErrorCode.ACTIVE_SESSION_EXISTS, "已存在活跃会话");
        }
        String sessionId = PublicIdGenerator.nextId();
        OffsetDateTime now = OffsetDateTime.now();
        ExecutionSession session = ExecutionSession.builder()
                .sessionId(sessionId)
                .labId(req.labId())
                .versionId(snapshot.versionId())
                .versionNo(snapshot.versionNo())
                .operatorId(operatorId)
                .status(SessionStatus.IN_PROGRESS)
                .currentStepNo(1)
                .totalSteps(snapshot.totalSteps())
                .blockedFlag(false)
                .startedAt(now)
                .lastActiveAt(now)
                .preconditionConfirmed(true)
                .preconditionConfirmedAt(now)
                .preconditionSnapshot("{}")
                .rowVersion(0)
                .build();
        SessionEntity entity = converter.toEntity(session);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        sessionMapper.insert(entity);
        appendRecord(sessionId, EventType.SESSION_STARTED, operatorId, RecordSource.STUDENT, 1,
                "会话开始", null, RecordResult.SUCCESS);
        return toView(session, false);
    }

    @Transactional
    public SessionView advance(String sessionId, int expectStepNo, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.isTerminal()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        if (session.getStatus() == SessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.SESSION_PAUSED_CANNOT_ADVANCE);
        }
        if (expectStepNo > session.getCurrentStepNo()) {
            throw new BusinessException(ErrorCode.STEP_EXPECTATION_AHEAD);
        }
        if (expectStepNo < session.getCurrentStepNo()) {
            return toView(session, true);
        }
        if (session.getCurrentStepNo() > session.getTotalSteps()) {
            throw new BusinessException(ErrorCode.STEP_OUT_OF_RANGE);
        }
        boolean isLastStep = session.getCurrentStepNo() == session.getTotalSteps();
        int next = session.getCurrentStepNo() + 1;
        int rows = sessionMapper.casAdvance(sessionId, expectStepNo, isLastStep ? session.getCurrentStepNo() : next);
        if (rows == 0) {
            return toView(loadOwned(sessionId, operatorId), true);
        }
        int attempt = nextAttempt(sessionId, expectStepNo);
        StepProgressEntity p = new StepProgressEntity();
        p.setSessionId(sessionId);
        p.setStepNo(expectStepNo);
        p.setAttemptNo(attempt);
        p.setCompletedAt(OffsetDateTime.now());
        p.setCompletedBy(operatorId);
        stepProgressMapper.insert(p);
        appendRecord(sessionId, EventType.STEP_COMPLETED, operatorId, RecordSource.STUDENT, expectStepNo,
                "完成步骤 " + expectStepNo, null, RecordResult.SUCCESS);
        if (isLastStep) {
            SessionEntity entity = sessionMapper.selectById(sessionId);
            entity.setStatus(SessionStatus.COMPLETED.name());
            entity.setEndedAt(OffsetDateTime.now());
            entity.setLastActiveAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            sessionMapper.updateById(entity);
            appendRecord(sessionId, EventType.SESSION_COMPLETED, "SYSTEM", RecordSource.SYSTEM,
                    session.getTotalSteps(), "会话完成", null, RecordResult.SUCCESS);
        }
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public SessionView rollback(String sessionId, int expectStepNo, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.isTerminal()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        if (expectStepNo != session.getCurrentStepNo()) {
            throw new BusinessException(ErrorCode.STEP_EXPECTATION_AHEAD);
        }
        if (session.getCurrentStepNo() <= 1) {
            throw new BusinessException(ErrorCode.STEP_AT_FIRST);
        }
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setCurrentStepNo(session.getCurrentStepNo() - 1);
        entity.setLastActiveAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.STEP_ROLLED_BACK, operatorId, RecordSource.STUDENT,
                expectStepNo, "回退步骤 " + expectStepNo, null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public SessionView pause(String sessionId, String reason, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        stateMachine.resolve(session.getStatus(), EventType.PAUSED);
        if (session.isBlockedFlag()) {
            appendRecord(sessionId, EventType.UNBLOCKED, "SYSTEM", RecordSource.SYSTEM,
                    session.getCurrentStepNo(), "暂停时自动解除阻塞", null, RecordResult.SUCCESS);
        }
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setStatus(SessionStatus.PAUSED.name());
        entity.setBlockedFlag(false);
        entity.setBlockReasonCategory(null);
        entity.setPauseReason(reason);
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.PAUSED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "会话暂停", null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public SessionView resume(String sessionId, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        stateMachine.resolve(session.getStatus(), EventType.RESUMED);
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setStatus(SessionStatus.IN_PROGRESS.name());
        entity.setPauseReason(null);
        entity.setLastActiveAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.RESUMED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "会话恢复", null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public SessionView abandon(String sessionId, AbandonRequest req, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.isTerminal()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setStatus(SessionStatus.ABANDONED.name());
        entity.setAbandonReason(req.reason());
        entity.setBlockedFlag(false);
        entity.setEndedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.SESSION_ABANDONED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "会话放弃", null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public void submitText(String sessionId, TextSubmitRequest req, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.isTerminal()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        if (req.text().length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(ErrorCode.TEXT_TOO_LONG);
        }
        appendRecord(sessionId, EventType.TEXT_SUBMITTED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), req.summary() == null ? "文本提交" : req.summary(),
                req.text(), RecordResult.UNKNOWN);
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setLastActiveAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
    }

    @Transactional(readOnly = true)
    public SessionView getStatus(String sessionId, String operatorId) {
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional(readOnly = true)
    public List<TimelineItemView> getTimeline(String sessionId, String operatorId) {
        loadOwned(sessionId, operatorId);
        List<ExecutionRecordEntity> records = executionRecordMapper.selectList(
                new LambdaQueryWrapper<ExecutionRecordEntity>()
                        .eq(ExecutionRecordEntity::getSessionId, sessionId)
                        .orderByAsc(ExecutionRecordEntity::getOccurredAt)
                        .orderByAsc(ExecutionRecordEntity::getId));
        return records.stream().map(r -> new TimelineItemView(
                r.getRecordId(), r.getEventType(),
                r.getOccurredAt() == null ? null : r.getOccurredAt().toString(),
                r.getSummary(), r.getStepNo(), r.getResult())).toList();
    }

    @Transactional(readOnly = true)
    public ContextView getContext(String sessionId, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        LabSnapshot snapshot = labSnapshotQueryPort.loadPublishedSnapshot(
                session.getLabId(), session.getVersionNo());
        List<Integer> completed = stepProgressMapper.selectList(
                new LambdaQueryWrapper<StepProgressEntity>()
                        .eq(StepProgressEntity::getSessionId, sessionId)
                        .orderByAsc(StepProgressEntity::getStepNo))
                .stream().map(StepProgressEntity::getStepNo).distinct().toList();
        List<ExecutionRecordEntity> recent = executionRecordMapper.selectList(
                new LambdaQueryWrapper<ExecutionRecordEntity>()
                        .eq(ExecutionRecordEntity::getSessionId, sessionId)
                        .orderByDesc(ExecutionRecordEntity::getOccurredAt)
                        .orderByDesc(ExecutionRecordEntity::getId)
                        .last("limit 20"));
        List<TimelineItemView> recentView = recent.stream().map(r -> new TimelineItemView(
                r.getRecordId(), r.getEventType(),
                r.getOccurredAt() == null ? null : r.getOccurredAt().toString(),
                r.getSummary(), r.getStepNo(), r.getResult())).toList();
        return new ContextView(
                session.getSessionId(), session.getLabId(), session.getVersionNo(),
                snapshot.objective(), session.getCurrentStepNo(), session.getTotalSteps(),
                completed, recentView, resolveLabel(session), session.isBlockedFlag(),
                isOverdue(session), true);
    }

    @Transactional
    public SessionView block(String sessionId, String category, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "仅进行中会话可阻塞");
        }
        BlockReasonCategory reason;
        try {
            reason = BlockReasonCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_ENUM_VALUE);
        }
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setBlockedFlag(true);
        entity.setBlockReasonCategory(reason.name());
        entity.setLastActiveAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.BLOCKED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "会话阻塞: " + reason.name(), null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public SessionView unblock(String sessionId, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (!session.isBlockedFlag()) {
            return toView(session, true);
        }
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setBlockedFlag(false);
        entity.setBlockReasonCategory(null);
        entity.setLastActiveAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.updateById(entity);
        appendRecord(sessionId, EventType.UNBLOCKED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "阻塞已解除", null, RecordResult.SUCCESS);
        return toView(loadOwned(sessionId, operatorId), false);
    }

    @Transactional
    public void registerEnvironment(
            String sessionId, String envKey, EnvironmentRegisterRequest req, String operatorId) {
        ExecutionSession session = loadOwned(sessionId, operatorId);
        if (session.isTerminal()) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        if (envKey == null || envKey.isBlank() || envKey.length() > 64) {
            throw new BusinessException(ErrorCode.ENV_KEY_INVALID);
        }
        EnvironmentItemEntity item = environmentItemMapper.selectOne(
                new LambdaQueryWrapper<EnvironmentItemEntity>()
                        .eq(EnvironmentItemEntity::getSessionId, sessionId)
                        .eq(EnvironmentItemEntity::getEnvKey, envKey));
        OffsetDateTime now = OffsetDateTime.now();
        if (item == null) {
            long count = environmentItemMapper.selectCount(
                    new LambdaQueryWrapper<EnvironmentItemEntity>()
                            .eq(EnvironmentItemEntity::getSessionId, sessionId));
            if (count >= 20) {
                throw new BusinessException(ErrorCode.ENV_ITEM_LIMIT_REACHED);
            }
            item = new EnvironmentItemEntity();
            item.setSessionId(sessionId);
            item.setEnvKey(envKey);
            item.setEnvValue(req.value());
            item.setCapturedAt(now);
            item.setUpdatedAt(now);
            item.setRowVersion(0);
            environmentItemMapper.insert(item);
        } else {
            environmentItemMapper.update(null,
                    new LambdaUpdateWrapper<EnvironmentItemEntity>()
                            .eq(EnvironmentItemEntity::getSessionId, sessionId)
                            .eq(EnvironmentItemEntity::getEnvKey, envKey)
                            .set(EnvironmentItemEntity::getEnvValue, req.value())
                            .set(EnvironmentItemEntity::getUpdatedAt, now));
        }
        appendRecord(sessionId, EventType.ENVIRONMENT_REGISTERED, operatorId, RecordSource.STUDENT,
                session.getCurrentStepNo(), "登记环境信息: " + envKey, null, RecordResult.SUCCESS);
        SessionEntity entity = sessionMapper.selectById(sessionId);
        entity.setLastActiveAt(now);
        entity.setUpdatedAt(now);
        sessionMapper.updateById(entity);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentItemView> listEnvironment(String sessionId, String operatorId) {
        loadOwned(sessionId, operatorId);
        return environmentItemMapper.selectList(
                new LambdaQueryWrapper<EnvironmentItemEntity>()
                        .eq(EnvironmentItemEntity::getSessionId, sessionId)
                        .orderByAsc(EnvironmentItemEntity::getCapturedAt))
                .stream().map(e -> new EnvironmentItemView(
                        e.getEnvKey(), e.getEnvValue(),
                        e.getCapturedAt() == null ? null : e.getCapturedAt().toString(),
                        e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionListItemView> listForTeaching(String operatorId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<SessionEntity>()
                        .orderByDesc(SessionEntity::getStartedAt)
                        .last("limit 100"))
                .stream().map(e -> new SessionListItemView(
                        e.getSessionId(), e.getLabId(), e.getVersionNo(), e.getStatus(),
                        e.getCurrentStepNo(), e.getTotalSteps(),
                        e.getStartedAt() == null ? null : e.getStartedAt().toString(),
                        e.getLastActiveAt() == null ? null : e.getLastActiveAt().toString()))
                .toList();
    }

    private ExecutionSession loadOwned(String sessionId, String operatorId) {
        SessionEntity entity = sessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        if (!operatorId.equals(entity.getOperatorId())) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        return converter.toDomain(entity);
    }

    private void appendRecord(String sessionId, EventType type, String operatorId, RecordSource source,
            Integer stepNo, String summary, String contentText, RecordResult result) {
        ExecutionRecordEntity e = new ExecutionRecordEntity();
        e.setRecordId(PublicIdGenerator.nextId());
        e.setSessionId(sessionId);
        e.setEventType(type.name());
        e.setOccurredAt(OffsetDateTime.now());
        e.setOperatorId(operatorId);
        e.setSource(source.name());
        e.setStepNo(stepNo);
        e.setSummary(summary);
        e.setContentText(contentText);
        e.setResult(result.name());
        e.setCreatedAt(OffsetDateTime.now());
        executionRecordMapper.insert(e);
    }

    private int nextAttempt(String sessionId, int stepNo) {
        StepProgressEntity max = stepProgressMapper.selectOne(
                new LambdaQueryWrapper<StepProgressEntity>()
                        .eq(StepProgressEntity::getSessionId, sessionId)
                        .eq(StepProgressEntity::getStepNo, stepNo)
                        .orderByDesc(StepProgressEntity::getAttemptNo)
                        .last("limit 1"));
        return max == null ? 1 : max.getAttemptNo() + 1;
    }

    private boolean isOverdue(ExecutionSession s) {
        return s.getStatus() == SessionStatus.IN_PROGRESS
                && s.getLastActiveAt() != null
                && Duration.between(s.getLastActiveAt(), OffsetDateTime.now()).toHours() >= 24;
    }

    private String resolveLabel(ExecutionSession s) {
        if (s.getStatus() == SessionStatus.COMPLETED) {
            return "COMPLETED";
        }
        if (s.getStatus() == SessionStatus.ABANDONED) {
            return "ABANDONED";
        }
        if (s.getStatus() == SessionStatus.PAUSED) {
            return "PAUSED";
        }
        if (s.isBlockedFlag()) {
            return "BLOCKED";
        }
        return "IN_PROGRESS";
    }

    private SessionView toView(ExecutionSession s, boolean duplicated) {
        return new SessionView(
                s.getSessionId(), s.getLabId(), s.getVersionNo(),
                s.getStatus() == null ? null : s.getStatus().name(), resolveLabel(s),
                s.getCurrentStepNo(), s.getTotalSteps(), s.isBlockedFlag(),
                s.getStartedAt() == null ? null : s.getStartedAt().toString(),
                s.getLastActiveAt() == null ? null : s.getLastActiveAt().toString(),
                isOverdue(s), duplicated);
    }
}