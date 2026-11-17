package com.cloudlabcopilot.session.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_session_environment_item")
public class EnvironmentItemEntity {

    private String sessionId;
    private String envKey;
    private String envValue;
    private OffsetDateTime capturedAt;
    private OffsetDateTime updatedAt;
    private Integer rowVersion;
}