package com.cloudlabcopilot.diagnosis.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.id.PublicIdGenerator;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.diagnosis.infrastructure.entity.DiagnosisQuestionEntity;
import com.cloudlabcopilot.diagnosis.infrastructure.entity.DiagnosisThreadEntity;
import com.cloudlabcopilot.diagnosis.infrastructure.mapper.DiagnosisQuestionMapper;
import com.cloudlabcopilot.diagnosis.infrastructure.mapper.DiagnosisThreadMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class QaThreadService {

    private final DiagnosisThreadMapper threadMapper;
    private final DiagnosisQuestionMapper questionMapper;

    public QaThreadService(DiagnosisThreadMapper threadMapper, DiagnosisQuestionMapper questionMapper) {
        this.threadMapper = threadMapper;
        this.questionMapper = questionMapper;
    }

    @Transactional
    public DiagnosisThreadEntity createThread(Long labId, String operatorId, String title) {
        String threadId = PublicIdGenerator.nextId();
        OffsetDateTime now = OffsetDateTime.now();
        DiagnosisThreadEntity t = new DiagnosisThreadEntity();
        t.setThreadId(threadId);
        t.setLabId(labId);
        t.setOperatorId(operatorId);
        t.setTitle(title);
        t.setStatus("OPEN");
        t.setTurnCount(0);
        t.setLastActiveAt(now);
        t.setExpiresAt(now.plusHours(24));
        t.setRowVersion(0);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        threadMapper.insert(t);
        return t;
    }

    @Transactional
    public DiagnosisThreadEntity closeThread(String threadId, String operatorId) {
        DiagnosisThreadEntity t = threadMapper.selectById(threadId);
        if (t == null) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_THREAD_NOT_FOUND);
        }
        if (!t.getOperatorId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_OPERATION);
        }
        t.setStatus("CLOSED");
        t.setUpdatedAt(OffsetDateTime.now());
        threadMapper.updateById(t);
        return t;
    }

    public DiagnosisThreadEntity getThread(String threadId, String operatorId) {
        DiagnosisThreadEntity t = threadMapper.selectById(threadId);
        if (t == null) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_THREAD_NOT_FOUND);
        }
        return t;
    }

    public List<DiagnosisQuestionEntity> history(String operatorId, int page, int pageSize) {
        int size = Math.max(1, Math.min(pageSize, 50));
        int offset = (Math.max(page, 1) - 1) * size;
        return questionMapper.selectList(
                new LambdaQueryWrapper<DiagnosisQuestionEntity>()
                        .eq(DiagnosisQuestionEntity::getOperatorId, operatorId)
                        .orderByDesc(DiagnosisQuestionEntity::getCreatedAt)
                        .last("limit " + size + " offset " + offset));
    }
}