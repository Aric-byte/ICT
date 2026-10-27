package com.cloudlabcopilot.boot.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源未找到"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    PAYLOAD_TOO_LARGE(413, "请求体过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    SYSTEM_ERROR(500, "系统错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}