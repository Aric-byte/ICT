package com.cloudlabcopilot.session.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_session_state_snapshot")
public class StateSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String statusBefore;
    private String statusAfter;
    private Integer stepBefore;
    private Integer stepAfter;
    private Boolean blockedAfter;
    private String stateLabel;
    private String triggerEvent;
    private String operatorId;
    private OffsetDateTime computedAt;
}