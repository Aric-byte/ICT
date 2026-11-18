package com.cloudlabcopilot.diagnosis.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("diagnosis_question")
public class DiagnosisQuestionEntity {

    @TableId(type = IdType.INPUT)
    private String questionId;
    private String threadId;
    private Integer followUpIndex;
    private String operatorId;
    private Long labId;
    private String sessionId;
    private String labCode;
    private String techStack;
    private String questionText;
    private String pastedText;
    private String contextCompleteness;
    private String idempotencyKey;
    private OffsetDateTime retentionExpireAt;
    private OffsetDateTime createdAt;
}