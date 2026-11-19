package com.cloudlabcopilot.config;

import com.cloudlabcopilot.knowledge.application.KnowledgeCardService;
import com.cloudlabcopilot.knowledge.domain.model.KnowledgeCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "cloudlab.demo.knowledge.enabled", havingValue = "true", matchIfMissing = true)
public class DemoKnowledgeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoKnowledgeSeeder.class);

    private final KnowledgeCardService service;

    public DemoKnowledgeSeeder(KnowledgeCardService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        int count = 0;
        count += seed("OPENGAUSS", "openGauss", openGauss());
        count += seed("MYSQL", "MySQL", mysql());
        count += seed("MYBATIS", "MyBatis", mybatis());
        log.info("dev 知识卡片初始化完成，共 {} 条", count);
    }

    private int seed(String tech, String label, List<Template> templates) {
        int inserted = 0;
        for (int i = 0; i < 20; i++) {
            Template t = templates.get(i % templates.size());
            String title = label + " 高频错误 #" + (i + 1) + "：" + t.name;
            try {
                KnowledgeCard card = KnowledgeCard.builder()
                        .techStack(tech)
                        .title(title)
                        .symptom(t.symptom)
                        .safeNextStep(t.nextStep)
                        .verificationMethod(t.verify)
                        .caseOrigin("DERIVED")
                        .sourceType("DEMO_DOC")
                        .sourceRef("demo/" + tech.toLowerCase() + "/" + (i + 1))
                        .applicableVersions(List.of("5.x", "3.x", "8.x"))
                        .errorPatterns(List.of(KnowledgeCard.ErrorPattern.builder()
                                .pattern(t.pattern).matchType("KEYWORD").sortNo(1).build()))
                        .causes(List.of(KnowledgeCard.Cause.builder()
                                .text(t.cause).confidence("MEDIUM").sortNo(1).build()))
                        .build();
                KnowledgeCard created = service.createCard(card);
                service.updateStatus(created.getId(), "PUBLISHED", "APPROVED");
                inserted++;
            } catch (Exception ignored) {
                // 幂等：已存在或校验失败则跳过
            }
        }
        return inserted;
    }

    private record Template(String name, String symptom, String nextStep, String verify,
            String pattern, String cause) {
    }

    private List<Template> openGauss() {
        return List.of(
                new Template("relation does not exist",
                        "执行 SQL 报 relation ... does not exist",
                        "请先确认当前连接的数据库与 schema，并核对表名大小写后重新查询",
                        "在 openGauss 中执行 \\dt 查看当前 schema 下的表",
                        "does not exist", "表名/ schema 或大小写不匹配"),
                new Template("connection refused",
                        "客户端连接 openGauss 报 connection refused",
                        "请核对监听地址与端口是否放通，并确认服务处于运行状态",
                        "使用 gs_ctl status 查看实例状态",
                        "connection refused", "服务未启动或端口未监听"),
                new Template("password authentication failed",
                        "登录报 password authentication failed",
                        "请核对用户名/密码与 pg_hba.conf 认证方式，勿在提问中粘贴真实口令",
                        "用正确的账户重新尝试连接",
                        "authentication failed", "凭据不匹配或认证方式限制"),
                new Template("permission denied",
                        "执行 DDL 报 permission denied",
                        "请确认当前角色是否具备该对象的操作权限",
                        "查询当前用户与角色授权情况",
                        "permission denied", "角色权限不足"),
                new Template("syntax error",
                        "SQL 报 syntax error",
                        "请检查语句分隔符、关键字拼写与括号配对，先执行最小语句验证",
                        "拆分语句逐段执行定位出错位置",
                        "syntax error", "语句拼写或结构错误"));
    }

    private List<Template> mysql() {
        return List.of(
                new Template("Access denied for user",
                        "连接 MySQL 报 Access denied for user",
                        "请核对用户名、主机来源与授权范围，避免在提问中粘贴真实密码",
                        "在 MySQL 中用 SHOW GRANTS 查看当前用户权限",
                        "Access denied", "账户密码或 host 授权不匹配"),
                new Template("Unknown column",
                        "查询报 Unknown column 'x' in 'field list'",
                        "请核对列名拼写与表结构，必要时先 DESC 表确认字段",
                        "执行 DESC 表名 查看列定义",
                        "Unknown column", "列名拼写错误或字段不存在"),
                new Template("Table doesn't exist",
                        "查询报 Table ... doesn't exist",
                        "请确认当前数据库与表名，并核对是否连错库",
                        "执行 SHOW TABLES 查看当前库中的表",
                        "doesn't exist", "连错库或表名错误"),
                new Template("Cannot add foreign key constraint",
                        "建表报 Cannot add foreign key constraint",
                        "请核对引用列类型、字符集与索引是否一致，先建父表再建子表",
                        "SHOW CREATE TABLE 对比父表与子表定义",
                        "foreign key", "外键列类型/字符集/索引不一致"),
                new Template("Duplicate entry",
                        "插入报 Duplicate entry for key",
                        "请先查询是否存在相同唯一键，再决定更新或使用不同值",
                        "按唯一键条件查询现有记录",
                        "Duplicate entry", "唯一键冲突"));
    }

    private List<Template> mybatis() {
        return List.of(
                new Template("Invalid bound statement",
                        "运行时报 Invalid bound statement (not found)",
                        "请核对 Mapper 接口与 XML 的 namespace、id 是否一致，并确认 XML 已被扫描",
                        "检查 mapper-locations 与 namespace 配置",
                        "Invalid bound statement", "Mapper 与 XML 映射不一致或未扫描"),
                new Template("BindingException",
                        "报 BindingException",
                        "请检查 Mapper 方法参数与 SQL 占位符是否对应，必要时使用 @Param 显式命名",
                        "对照 Mapper 方法与 XML 占位符",
                        "BindingException", "参数绑定名称不匹配"),
                new Template("TooManyResultsException",
                        "查询报 TooManyResultsException",
                        "请确认查询是否应返回单条，必要时使用 limit 或改为 List 接收",
                        "检查 SQL 结果条数与返回类型",
                        "TooManyResults", "返回多条却按单对象接收"),
                new Template("SQLSyntaxErrorException",
                        "MyBatis 执行报 SQLSyntaxErrorException",
                        "请先单独在数据库中验证该 SQL，再检查动态 SQL 标签拼接",
                        "打印最终 SQL 并在数据库中直接执行",
                        "SQLSyntaxError", "动态 SQL 拼接或方言不匹配"),
                new Template("CannotGetJdbcConnection",
                        "报 CannotGetJdbcConnectionException",
                        "请核对数据源 URL、账号与驱动类配置，勿在提问中粘贴连接串口令",
                        "检查 spring.datasource 配置与驱动版本",
                        "CannotGetJdbcConnection", "数据源配置错误或数据库不可达"));
    }
}