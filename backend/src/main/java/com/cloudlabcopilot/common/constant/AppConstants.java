package com.cloudlabcopilot.common.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String API_PREFIX = "/api";
    public static final String API_V1_PREFIX = "/api/v1";
    public static final String DEFAULT_TRACE_ID_HEADER = "X-Trace-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_OPERATOR_ID = "X-Operator-Id";
    public static final String HEADER_OPERATOR_ROLE = "X-Operator-Role";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String TRACE_ID_MDC_KEY = "traceId";
}