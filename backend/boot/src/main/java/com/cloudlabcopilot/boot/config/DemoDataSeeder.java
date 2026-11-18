package com.cloudlabcopilot.boot.config;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.lab.application.LabCommandService;
import com.cloudlabcopilot.lab.domain.model.Difficulty;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.model.PreconditionLevel;
import com.cloudlabcopilot.lab.domain.model.PreconditionType;
import com.cloudlabcopilot.lab.domain.model.TechStack;
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
@ConditionalOnProperty(name = "cloudlab.demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String OPERATOR = "demo-system";

    private final LabCommandService labCommandService;

    public DemoDataSeeder(LabCommandService labCommandService) {
        this.labCommandService = labCommandService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("DEMO-OPENGAUSS-01", "openGauss 入门实验", TechStack.OPEN_GAUSS, Difficulty.BEGINNER,
                "掌握 openGauss 基本连接与建表",
                List.of("openGauss", "入门"),
                List.of(
                        step(1, "连接数据库", "使用 gsql 连接 openGauss", "成功连接数据库", "gsql -d postgres -p 5432"),
                        step(2, "创建表", "CREATE TABLE students(id int, name varchar(50))", "表创建成功", "CREATE TABLE"),
                        step(3, "插入数据", "INSERT INTO students VALUES(1,'Alice')", "插入成功", "INSERT"),
                        step(4, "查询验证", "SELECT * FROM students", "查询到数据", "SELECT")),
                List.of(
                        pre(1, PreconditionType.ENV, PreconditionLevel.REQUIRED, "已安装 openGauss 客户端"),
                        pre(2, PreconditionType.KNOWLEDGE, PreconditionLevel.RECOMMENDED, "了解 SQL 基础")));

        seed("DEMO-MYSQL-01", "MySQL 入门实验", TechStack.MYSQL, Difficulty.BEGINNER,
                "掌握 MySQL 基本连接与建表",
                List.of("MySQL", "入门"),
                List.of(
                        step(1, "连接数据库", "mysql -u root -p", "成功连接", "mysql -u root -p"),
                        step(2, "创建数据库", "CREATE DATABASE demo", "数据库创建成功", "CREATE DATABASE"),
                        step(3, "创建表", "CREATE TABLE t(id int)", "表创建成功", "CREATE TABLE")),
                List.of(
                        pre(1, PreconditionType.ENV, PreconditionLevel.REQUIRED, "已安装 MySQL 客户端")));

        seed("DEMO-MYBATIS-01", "MyBatis 入门实验", TechStack.MYBATIS, Difficulty.INTERMEDIATE,
                "掌握 MyBatis 映射与查询",
                List.of("MyBatis", "进阶"),
                List.of(
                        step(1, "配置数据源", "配置 mybatis-config.xml", "数据源可用", "jdbc.url"),
                        step(2, "编写 Mapper", "编写 UserMapper.xml", "映射生效", "namespace"),
                        step(3, "执行查询", "调用 selectById", "查询成功", "select")),
                List.of(
                        pre(1, PreconditionType.ENV, PreconditionLevel.REQUIRED, "已完成 JDBC 实验"),
                        pre(2, PreconditionType.RESOURCE, PreconditionLevel.RECOMMENDED, "准备示例数据库")));

        seedDraft();
        log.info("dev 演示数据初始化完成");
    }

    private void seedDraft() {
        try {
            LabDefinition input = LabDefinition.builder()
                    .labCode("DEMO-DRAFT-01")
                    .name("草稿实验（未发布）")
                    .techStack(TechStack.OPEN_GAUSS)
                    .difficulty(Difficulty.ADVANCED)
                    .objective("演示草稿状态，学生目录不可见")
                    .estimatedMinutes(45)
                    .tags(List.of("草稿"))
                    .build();
            labCommandService.createLab(input, OPERATOR);
            log.info("演示草稿实验已注入: DEMO-DRAFT-01");
        } catch (BusinessException e) {
            log.info("演示草稿实验已存在或跳过: {}", e.getMessage());
        }
    }

    private void seed(
            String labCode,
            String name,
            TechStack tech,
            Difficulty difficulty,
            String objective,
            List<String> tags,
            List<LabStep> steps,
            List<Precondition> preconditions) {
        try {
            LabDefinition input = LabDefinition.builder()
                    .labCode(labCode)
                    .name(name)
                    .techStack(tech)
                    .difficulty(difficulty)
                    .objective(objective)
                    .estimatedMinutes(30)
                    .tags(tags)
                    .build();
            labCommandService.createLab(input, OPERATOR);
            labCommandService.saveSteps(labCode, steps, OPERATOR);
            labCommandService.savePreconditions(labCode, preconditions, OPERATOR);
            labCommandService.publish(labCode, OPERATOR);
            log.info("演示实验已注入并发布: {}", labCode);
        } catch (BusinessException e) {
            log.info("演示实验已存在或跳过: {} ({})", labCode, e.getMessage());
        }
    }

    private LabStep step(int no, String title, String instruction, String criteria, String hint) {
        return LabStep.builder()
                .stepNo(no)
                .title(title)
                .instruction(instruction)
                .completionCriteria(criteria)
                .hint(hint)
                .build();
    }

    private Precondition pre(int sortNo, PreconditionType type, PreconditionLevel level, String description) {
        return Precondition.builder()
                .sortNo(sortNo)
                .type(type)
                .level(level)
                .description(description)
                .checkMode("SELF_CONFIRM")
                .build();
    }
}