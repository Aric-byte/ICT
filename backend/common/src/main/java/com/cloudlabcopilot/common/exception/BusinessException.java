package com.cloudlabcopilot.common.exception;

import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.common.response.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final ErrorCode errorCode;
    private final Object details;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
        this.details = null;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.errorCode = null;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getHttpCode();
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.code = errorCode.getHttpCode();
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(ErrorCode errorCode, String messageOverride) {
        super(messageOverride);
        this.code = errorCode.getHttpCode();
        this.errorCode = errorCode;
        this.details = null;
    }
}