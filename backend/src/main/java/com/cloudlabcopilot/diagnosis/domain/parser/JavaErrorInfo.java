package com.cloudlabcopilot.diagnosis.domain.parser;

import java.util.List;

/**
 * Java 错误感知解析结果
 */
public record JavaErrorInfo(
        boolean detected,
        String errorCategory,
        String exceptionClass,
        String rootCause,
        String userCodeLocation,
        List<String> keywords,
        String heuristicHint) {

    public static JavaErrorInfo empty(String fallbackKeyword) {
        return new JavaErrorInfo(
                false,
                "GENERAL",
                null,
                null,
                null,
                fallbackKeyword != null && !fallbackKeyword.isBlank() ? List.of(fallbackKeyword) : List.of("Java"),
                "未识别出标准 Java 堆栈或编译报错，建议补充完整报错信息或代码片段。");
    }
}
