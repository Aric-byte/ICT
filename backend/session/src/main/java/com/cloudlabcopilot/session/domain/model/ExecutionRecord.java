package com.cloudlabcopilot.session.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    private Long id;
    private String recordId;
    private String sessionId;
    private EventType eventType;
    private OffsetDateTime occurredAt;
    private String operatorId;
    private RecordSource source;
    private Integer stepNo;
    private String summary;
    private String contentText;
    private RecordResult result;
}