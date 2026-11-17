package com.cloudlabcopilot.session.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_session")
public class SessionEntity {

    @TableId(type = IdType.INPUT)
    private String sessionId;
    private Long labId;
    private Long versionId;
    private Integer versionNo;
    private String operatorId;
    private String status;
    private Integer currentStepNo;
    private Integer totalSteps;
    private Boolean blockedFlag;
    private String blockReasonCategory;
    private String pauseReason;
    private String abandonReason;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActiveAt;
    private OffsetDateTime endedAt;
    private Boolean preconditionConfirmed;
    private OffsetDateTime preconditionConfirmedAt;
    private String preconditionSnapshot;
    @Version
    private Integer rowVersion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}