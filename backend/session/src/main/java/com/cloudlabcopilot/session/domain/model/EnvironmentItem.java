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
public class EnvironmentItem {

    private String sessionId;
    private String envKey;
    private String envValue;
    private OffsetDateTime capturedAt;
    private OffsetDateTime updatedAt;
    private int rowVersion;
}