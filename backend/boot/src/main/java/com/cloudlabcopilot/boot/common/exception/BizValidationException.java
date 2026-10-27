package com.cloudlabcopilot.boot.common.exception;

public class BizValidationException extends BusinessException {

    public BizValidationException(String message) {
        super(400, message);
    }
}