package com.cloudlabcopilot.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {

    FIELD_REQUIRED(400, "必填字段缺失"),
    INVALID_PARAMETER(400, "参数不合法"),
    INVALID_ENUM_VALUE(400, "枚举取值不合法"),
    INVALID_PAGE_SIZE(400, "分页参数越界"),
    INVALID_TIME_RANGE(400, "时间范围倒置"),

    LAB_CODE_CONFLICT(409, "实验编码已存在"),
    LAB_NOT_FOUND(404, "实验不存在"),
    LAB_NOT_DRAFT(400, "仅草稿可编辑"),
    LAB_PUBLISH_NO_STEP(400, "发布需至少一个步骤"),
    LAB_STEP_SEQUENCE_INVALID(400, "步骤序号不连续"),
    LAB_STEP_CRITERIA_MISSING(400, "步骤完成判据缺失"),
    LAB_OBJECTIVE_REQUIRED(400, "实验目标不能为空"),
    LAB_PRECONDITION_REQUIRED_MISSING(400, "发布需至少一条必需前置条件"),
    LAB_PRECONDITION_INVALID(400, "前置条件不合法"),
    LAB_VERSION_CONFLICT(409, "版本发布冲突"),
    LAB_NOT_STARTABLE(400, "实验当前不可开始"),

    VERSION_CONTENT_IMMUTABLE(400, "已发布版本内容不可修改"),
    VERSION_NOT_FOUND(404, "版本不存在"),
    VERSION_ARCHIVE_NOT_EFFECTIVE(400, "归档版本不可设为生效版本"),
    VERSION_SNAPSHOT_UNAVAILABLE(500, "绑定版本快照暂不可用"),
    CONCURRENT_MODIFICATION(409, "内容已被更新，请刷新后重试"),

    SESSION_NOT_FOUND(404, "会话不存在"),
    ACTIVE_SESSION_EXISTS(409, "已存在活跃会话"),
    PRECONDITION_NOT_CONFIRMED(400, "前置条件未确认"),
    TERMINAL_SESSION_IMMUTABLE(400, "会话已结束，如需继续请重新开始"),
    SESSION_PAUSED_CANNOT_ADVANCE(400, "会话已暂停，无法推进"),
    STEP_EXPECTATION_AHEAD(400, "期望步骤超前于当前步骤"),
    STEP_OUT_OF_RANGE(400, "步骤超出范围"),
    STEP_AT_FIRST(400, "当前已是第一步，无法回退"),
    ABANDON_REASON_REQUIRED(400, "放弃原因长度不足"),
    SESSION_LOCK_TIMEOUT(409, "会话并发冲突，请稍后重试"),

    ENV_ITEM_LIMIT_REACHED(400, "环境信息条目已达上限"),
    ENV_KEY_SENSITIVE(400, "禁止登记凭据类环境键"),
    ENV_KEY_INVALID(400, "环境键不合法"),
    TEXT_TOO_LONG(400, "文本超出长度上限"),
    SENSITIVE_CONTENT_REJECTED(400, "检测到疑似凭据/敏感信息，请脱敏后重新提交"),

    OPERATOR_IDENTITY_REQUIRED(400, "缺少操作者身份"),
    FORBIDDEN_OPERATION(403, "当前角色无权限执行该操作"),
    IDEMPOTENCY_KEY_REUSED(400, "幂等键被复用但请求内容不一致"),

    SYSTEM_ERROR(500, "系统错误");

    private final int httpCode;
    private final String message;

    ErrorCode(int httpCode, String message) {
        this.httpCode = httpCode;
        this.message = message;
    }
}