package com.cloudlabcopilot.safety.domain.rule;

import java.util.List;
import java.util.regex.Pattern;

public record DangerRule(
        String id,
        String category,
        String riskLevel,
        Pattern pattern,
        String description,
        String safeAlternative) {

    public static List<DangerRule> defaultRules() {
        return List.of(
                new DangerRule(
                        "R001",
                        "DELETE",
                        "HIGH",
                        Pattern.compile("(?i)\\bDELETE\\s+FROM\\b(?![\\s\\S]*\\bWHERE\\b)"),
                        "整表删除（无 WHERE 条件）",
                        "先备份数据，并用 SELECT 确认目标记录后再考虑带 WHERE 的删除"),
                new DangerRule(
                        "R002",
                        "DROP",
                        "HIGH",
                        Pattern.compile("(?i)\\bDROP\\s+(TABLE|DATABASE|SCHEMA)\\b"),
                        "删除表/库/模式",
                        "先导出结构与数据备份，评估影响后再操作；建议避免直接 DROP"),
                new DangerRule(
                        "R003",
                        "TRUNCATE",
                        "HIGH",
                        Pattern.compile("(?i)\\bTRUNCATE\\b"),
                        "清空整表",
                        "改用带 WHERE 的 DELETE，或先备份"),
                new DangerRule(
                        "R004",
                        "UPDATE",
                        "MEDIUM",
                        Pattern.compile("(?i)\\bUPDATE\\s+\\w+\\s+SET\\b(?![\\s\\S]*\\bWHERE\\b)"),
                        "全表更新（无 WHERE 条件）",
                        "先用 SELECT 确认影响范围，并补充 WHERE 条件"),
                new DangerRule(
                        "R005",
                        "PRIVILEGE",
                        "HIGH",
                        Pattern.compile("(?i)\\bGRANT\\s+ALL\\b|\\bALTER\\s+USER\\b|\\bsuperuser\\b"),
                        "提权/修改用户权限",
                        "仅在必要时由管理员在受控环境操作"),
                new DangerRule(
                        "R006",
                        "SYSTEM",
                        "HIGH",
                        Pattern.compile("(?i)\\b(shutdown|reboot|rm\\s+-rf|format\\s+[a-z]:)\\b"),
                        "系统级危险命令",
                        "禁止在学生/实验环境执行破坏性系统命令；改为只读检查"),
                new DangerRule(
                        "R007",
                        "JAVA_EXEC",
                        "HIGH",
                        Pattern.compile("(?i)(Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder|new\\s+ProcessBuilder)"),
                        "Java 进程级外部系统命令执行",
                        "避免在应用中直接执行宿主机外部命令，建议优先使用安全的 Java 原生 API 实现功能"),
                new DangerRule(
                        "R008",
                        "JAVA_EXIT",
                        "HIGH",
                        Pattern.compile("(?i)(System\\.exit|Runtime\\.getRuntime\\(\\)\\.exit|Runtime\\.getRuntime\\(\\)\\.halt)"),
                        "JVM 进程强制终止",
                        "禁止在代码中随意终止 JVM 虚拟机，应抛出可捕获的业务异常或通过 Spring 容器上下文优雅关闭"),
                new DangerRule(
                        "R009",
                        "JAVA_UNSAFE",
                        "MEDIUM",
                        Pattern.compile("(?i)(sun\\.misc\\.Unsafe|\\.setAccessible\\s*\\(\\s*true\\s*\\))"),
                        "绕过 JVM 安全访问限制或使用底层非受控内存",
                        "遵循标准 Java 封装规范，优先使用受控的反射 API 或依赖注入机制")
        );
    }
}
