package com.cloudlabcopilot.diagnosis.domain.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Java 错误感知解析器测试")
class JavaErrorParserTest {

    private final JavaErrorParser parser = new JavaErrorParser();

    @Test
    @DisplayName("精准识别 NPE 与用户代码行号")
    void parseNullPointerException() {
        String log = """
                java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
                    at com.example.demo.service.UserServiceImpl.getUser(UserServiceImpl.java:42)
                    at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:880)
                """;
        JavaErrorInfo info = parser.parse(log);
        assertThat(info.detected()).isTrue();
        assertThat(info.exceptionClass()).isEqualTo("NullPointerException");
        assertThat(info.userCodeLocation()).isEqualTo("com.example.demo.service.UserServiceImpl.getUser(UserServiceImpl.java:42)");
        assertThat(info.errorCategory()).isEqualTo("RUNTIME_EXCEPTION");
    }

    @Test
    @DisplayName("精准识别 Spring 端口被占用")
    void parsePortInUse() {
        String log = "Web server failed to start. Port 8080 was already in use.";
        JavaErrorInfo info = parser.parse(log);
        assertThat(info.detected()).isTrue();
        assertThat(info.exceptionClass()).isEqualTo("PortInUseException");
        assertThat(info.errorCategory()).isEqualTo("SPRING_FRAMEWORK");
        assertThat(info.keywords()).contains("8080");
    }

    @Test
    @DisplayName("精准识别 Spring 缺少 Bean 定义")
    void parseNoSuchBean() {
        String log = "No qualifying bean of type 'com.example.repository.UserRepo' available: expected at least 1 bean";
        JavaErrorInfo info = parser.parse(log);
        assertThat(info.detected()).isTrue();
        assertThat(info.exceptionClass()).isEqualTo("NoSuchBeanDefinitionException");
        assertThat(info.errorCategory()).isEqualTo("SPRING_FRAMEWORK");
        assertThat(info.keywords()).contains("com.example.repository.UserRepo");
    }

    @Test
    @DisplayName("精准识别 MyBatis 绑定失效")
    void parseMyBatisBinding() {
        String log = "org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): com.example.mapper.UserMapper.selectById";
        JavaErrorInfo info = parser.parse(log);
        assertThat(info.detected()).isTrue();
        assertThat(info.exceptionClass()).isEqualTo("BindingException");
        assertThat(info.errorCategory()).isEqualTo("MYBATIS_DATABASE");
    }

    @Test
    @DisplayName("精准识别 javac 编译报错")
    void parseCompilationError() {
        String log = "[ERROR] /D:/workspace/Main.java:[15,22] cannot find symbol class ObjectMapper";
        JavaErrorInfo info = parser.parse(log);
        assertThat(info.detected()).isTrue();
        assertThat(info.exceptionClass()).isEqualTo("JavaCompilerError");
        assertThat(info.errorCategory()).isEqualTo("COMPILATION_BUILD");
    }

    @Test
    @DisplayName("普通 Java 提问兜底处理")
    void parseGeneralQuestion() {
        String q = "请问 Java 中 HashMap 和 ConcurrentHashMap 有什么区别？";
        JavaErrorInfo info = parser.parse(q);
        assertThat(info.detected()).isFalse();
        assertThat(info.errorCategory()).isEqualTo("GENERAL");
    }
}
