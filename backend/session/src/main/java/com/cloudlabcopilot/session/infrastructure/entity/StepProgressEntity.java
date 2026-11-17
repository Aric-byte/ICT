package com.cloudlabcopilot.session.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_session_step_progress")
public class StepProgressEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer stepNo;
    private Integer attemptNo;
    private OffsetDateTime completedAt;
    private String completedBy;
    private String note;
}