package com.cloudlabcopilot.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    FORBIDDEN(403, "角色不允许"),
    NOT_FOUND(404, "资源未找到"),
    CONFLICT(409, "并发冲突"),
    SYSTEM_ERROR(500, "系统错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}