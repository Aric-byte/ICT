package com.cloudlabcopilot.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;
    private String errorCode;
    private Object details;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, null, null, null);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpCode(), errorCode.getMessage(), null, null, errorCode.name(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, Object details) {
        return new ApiResponse<>(errorCode.getHttpCode(), errorCode.getMessage(), null, null, errorCode.name(), details);
    }

    public static <T> ApiResponse<T> error(int code, String message, ErrorCode errorCode) {
        return new ApiResponse<>(code, message, null, null, errorCode == null ? null : errorCode.name(), null);
    }
}