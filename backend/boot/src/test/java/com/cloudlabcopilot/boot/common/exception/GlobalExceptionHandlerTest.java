package com.cloudlabcopilot.boot.common.exception;

import com.cloudlabcopilot.boot.common.response.ApiResponse;
import com.cloudlabcopilot.boot.common.response.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionShouldReturnErrorCode() {
        BusinessException e = new BusinessException(400, "test error");
        ApiResponse<Void> response = handler.handleBusinessException(e);
        assertEquals(400, response.getCode());
        assertEquals("test error", response.getMessage());
    }

    @Test
    void handleMethodNotAllowedShouldReturn405() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("DELETE");
        ApiResponse<Void> response = handler.handleHttpRequestMethodNotSupportedException(e);
        assertEquals(ResultCode.METHOD_NOT_ALLOWED.getCode(), response.getCode());
    }
}