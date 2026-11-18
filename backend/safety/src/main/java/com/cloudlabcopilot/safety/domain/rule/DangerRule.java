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
                        "先导出结构与数据备份，评估影响后再操作；学生环境建议避免直接 DROP"),
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
                        "系统级危险操作",
                        "禁止在学生实验环境执行；改为检查服务状态等只读动作"));
    }
}