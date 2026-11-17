package com.cloudlabcopilot.session.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_execution_record")
public class ExecutionRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordId;
    private String sessionId;
    private String eventType;
    private OffsetDateTime occurredAt;
    private String operatorId;
    private String source;
    private Integer stepNo;
    private String summary;
    private String contentText;
    private String result;
    private OffsetDateTime createdAt;
}