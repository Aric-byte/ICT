package com.cloudlabcopilot.boot.common.response;

import com.cloudlabcopilot.boot.common.constant.AppConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void successWithoutTraceIdShouldHaveNullTraceId() {
        MDC.clear();
        ApiResponse<String> response = ApiResponse.success("data");
        assertNull(response.getTraceId());
        assertEquals(200, response.getCode());
        assertEquals("data", response.getData());
    }

    @Test
    void successWithTraceIdShouldReadFromMdc() {
        MDC.put(AppConstants.TRACE_ID_MDC_KEY, "test-trace-id");
        ApiResponse<String> response = ApiResponse.success("data");
        assertEquals("test-trace-id", response.getTraceId());
        MDC.clear();
    }

    @Test
    void errorShouldReadTraceIdFromMdc() {
        MDC.put(AppConstants.TRACE_ID_MDC_KEY, "err-trace-id");
        ApiResponse<Void> response = ApiResponse.error(400, "bad request");
        assertEquals(400, response.getCode());
        assertEquals("err-trace-id", response.getTraceId());
        MDC.clear();
    }
}