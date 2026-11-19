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
        count += seed("JAVA", "Java Core", javaCore());
        count += seed("SPRING", "Spring Boot", springBoot());
        count += seed("MYBATIS", "MyBatis", mybatis());
        count += seed("OPENGAUSS", "openGauss", openGauss());
        count += seed("MYSQL", "MySQL", mysql());
        log.info("dev 知识卡片初始化完成，共 {} 条（已覆盖 Java/Spring/MyBatis 等核心技术栈）", count);
    }

    private int seed(String tech, String label, List<Template> templates) {
        int inserted = 0;
        for (int i = 0; i < templates.size(); i++) {
            Template t = templates.get(i);
            String title = label + " 常见问题 #" + (i + 1) + "：" + t.name;
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
                        .applicableVersions(List.of("17", "21", "3.x"))
                        .errorPatterns(List.of(KnowledgeCard.ErrorPattern.builder()
                                .pattern(t.pattern).matchType("KEYWORD").sortNo(1).build()))
                        .causes(List.of(KnowledgeCard.Cause.builder()
                                .text(t.cause).confidence("HIGH").sortNo(1).build()))
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

    private List<Template> javaCore() {
        return List.of(
                new Template("NullPointerException",
                        "运行时抛出 java.lang.NullPointerException 空指针异常",
                        "在调用对象方法前，使用 Objects.requireNonNull() 或 Optional 进行判空防守，确认对象已完成实例化",
                        "使用单元测试覆盖入参为 null 场景，观察是否正常返回或抛出受检异常",
                        "NullPointerException", "引用对象为 null 却尝试访问其成员方法或属性"),
                new Template("ClassNotFoundException / NoClassDefFoundError",
                        "类加载失败，抛出 ClassNotFoundException 或 NoClassDefFoundError",
                        "检查 pom.xml 对应依赖坐标是否引入，并排查 dependency scope 是否被错误设置为 provided 或 test",
                        "在终端执行 mvn dependency:tree 确认 jar 包是否已被引入类路径",
                        "ClassNotFoundException", "编译期或运行时缺少目标类的字节码依赖"),
                new Template("ConcurrentModificationException",
                        "遍历集合时抛出 java.util.ConcurrentModificationException",
                        "避免在 foreach 循环中直接调用 list.remove()；改用 Iterator.remove() 或 List.removeIf()，多线程场景使用 CopyOnWriteArrayList",
                        "编写并发或循环删除测试用例验证迭代安全性",
                        "ConcurrentModificationException", "集合在迭代遍历过程中被并发或非法结构修改"),
                new Template("OutOfMemoryError: Java heap space",
                        "JVM 抛出 java.lang.OutOfMemoryError: Java heap space",
                        "检查是否存在大集合未释放、死循环创建对象；可通过 -Xmx 调整堆内存大小，并配合 jmap 导出 hprof 堆转储文件分析内存泄漏",
                        "使用 JDK 自带的 VisualVM 或 jstat 观察 GC 与堆内存占用趋势",
                        "OutOfMemoryError", "堆内存耗尽或存在大对象未被垃圾回收释放"),
                new Template("ClassCastException",
                        "类型转换失败，抛出 java.lang.ClassCastException",
                        "强转类型前使用 instanceof 进行类型检查，或在泛型声明处明确类型边界",
                        "通过断点单步调试观察转换前对象的实际 Runtime Class",
                        "ClassCastException", "向下转型时对象的实际类型与目标类型不兼容"));
    }

    private List<Template> springBoot() {
        return List.of(
                new Template("NoSuchBeanDefinitionException",
                        "Spring 容器初始化失败：No qualifying bean of type available",
                        "检查目标实现类是否标记 @Component/@Service/@Repository 注解，并确认其包路径位于启动类的 @ComponentScan 扫描范围内",
                        "在启动日志中检查 Bean 扫描注册列表，或通过 ApplicationContext.getBeanDefinitionNames() 确认",
                        "NoSuchBeanDefinitionException", "Spring 容器未找到匹配的 Bean 定义"),
                new Template("PortInUseException",
                        "Spring Boot 启动报错: Port was already in use",
                        "使用命令 netstat -ano | findstr 8080 排查占用进程并关闭，或在 application.yml 中修改 server.port 指定空闲端口",
                        "启动后访问 http://localhost:PORT/actuator/health 验证服务端口连通",
                        "PortInUseException", "配置的 HTTP 服务端口已被其他进程占用"),
                new Template("BeanCreationException / CircularDependency",
                        "Spring Boot 启动报错: Circular reference between beans (循环依赖)",
                        "重构设计解除循环依赖关系，优先使用构造器注入配合设计模式解耦，或在依赖字段上临时添加 @Lazy 注解延时加载",
                        "重新执行 mvn spring-boot:run 确认依赖注入链正常构建",
                        "BeanCreationException", "两个或多个 Spring Bean 之间存在闭环循环依赖"),
                new Template("HttpMessageNotReadableException",
                        "前端请求后端接口报 400 且提示 HttpMessageNotReadableException",
                        "检查客户端发送的 JSON 格式与 Controller @RequestBody DTO 实体类的字段类型是否匹配（如时间格式、数字溢出）",
                        "在 DTO 属性上添加 @JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\") 进行格式化约束",
                        "HttpMessageNotReadableException", "请求体 JSON 无法正常反序列化为 Java 目标对象"));
    }

    private List<Template> mybatis() {
        return List.of(
                new Template("BindingException: Invalid bound statement",
                        "执行持久层调用报错：Invalid bound statement (not found)",
                        "检查 Mapper XML 文件中的 namespace 是否与 Java Mapper 接口全限定名完全一致，并确认 XML 中的 select/insert id 与方法名对应",
                        "检查 target/classes 目录下是否成功打包生成了 mapper XML 文件",
                        "Invalid bound statement", "Mapper 接口方法未能与 XML SQL 语句完成绑定映射"),
                new Template("BadSqlGrammarException",
                        "执行 MyBatis-Plus 报错: BadSqlGrammarException",
                        "检查 SQL 语法是否匹配当前数据库引擎（如 openGauss/PostgreSQL/MySQL），注意关键字转义、字段名大小写与类型转换",
                        "开启 MyBatis 的 SQL 日志输出 (logging.level.com.baomidou=DEBUG) 观察打印的完整 SQL",
                        "BadSqlGrammarException", "生成的 SQL 语句存在语法错误或字段/表名不存在"),
                new Template("CommunicationsException / CannotGetJdbcConnection",
                        "数据库连接失败: Cannot get Jedis connection / Communications link failure",
                        "核对 application.yml 中的 spring.datasource.url、账号密码以及数据库服务的白名单与端口监听状态",
                        "使用 telnet 或 nc 命令测试数据库端口是否畅通",
                        "CannotGetJdbcConnection", "数据源配置有误、网络不通或数据库连接池已满"));
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
                        "connection refused", "服务未启动或端口未监听"));
    }

    private List<Template> mysql() {
        return List.of(
                new Template("Access denied for user",
                        "连接 MySQL 报 Access denied for user",
                        "核对数据库用户名、密码及主机访问权限（% 或 localhost）",
                        "使用 mysql -u root -p 本地命令行验证口令",
                        "Access denied", "用户名密码不正确或主机无连接权限"),
                new Template("Unknown column in field list",
                        "执行 SQL 报 Unknown column ... in field list",
                        "检查实体类属性名与数据表列名映射，核对驼峰转下划线规则配置",
                        "执行 DESC table_name 查看数据表实际结构",
                        "Unknown column", "实体属性对应的数据库列不存在"));
    }
}
