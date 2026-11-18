package com.cloudlabcopilot.diagnosis.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("diagnosis_thread")
public class DiagnosisThreadEntity {

    @TableId(type = IdType.INPUT)
    private String threadId;
    private Long labId;
    private String operatorId;
    private String title;
    private String status;
    private Integer turnCount;
    private String activeQuestionId;
    private OffsetDateTime lastActiveAt;
    private OffsetDateTime expiresAt;
    private Integer rowVersion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}