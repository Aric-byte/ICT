package com.cloudlabcopilot.diagnosis.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.diagnosis.infrastructure.entity.DiagnosisQuestionEntity;
import com.cloudlabcopilot.diagnosis.infrastructure.mapper.DiagnosisQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RetentionExportService {

    private final DiagnosisQuestionMapper questionMapper;

    public RetentionExportService(DiagnosisQuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Transactional
    public int purgeExpired() {
        return questionMapper.delete(
                new LambdaQueryWrapper<DiagnosisQuestionEntity>()
                        .lt(DiagnosisQuestionEntity::getRetentionExpireAt, OffsetDateTime.now()));
    }

    public String exportCsv(String operatorId, int limit) {
        int max = Math.max(1, Math.min(limit, 1000));
        List<DiagnosisQuestionEntity> list = questionMapper.selectList(
                new LambdaQueryWrapper<DiagnosisQuestionEntity>()
                        .eq(DiagnosisQuestionEntity::getOperatorId, operatorId)
                        .orderByDesc(DiagnosisQuestionEntity::getCreatedAt)
                        .last("limit " + max));
        StringBuilder sb = new StringBuilder("questionId,threadId,techStack,questionText,createdAt\n");
        for (DiagnosisQuestionEntity q : list) {
            sb.append(q.getQuestionId()).append(',')
                    .append(q.getThreadId() == null ? "" : q.getThreadId()).append(',')
                    .append(q.getTechStack() == null ? "" : q.getTechStack()).append(',')
                    .append('"').append(q.getQuestionText() == null ? "" : q.getQuestionText().replace("\"", "\"\""))
                    .append('"').append(',')
                    .append(q.getCreatedAt()).append('\n');
        }
        return sb.toString();
    }
}