package com.cloudlabcopilot.diagnosis.domain.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java 专属感知解析器：负责从提问与日志文本中抽取 Java 异常类、根本原因、用户代码出错行及核心关键词
 */
@Component
public class JavaErrorParser {

    // 匹配 Java 异常类名称与消息，如 java.lang.NullPointerException: Cannot invoke ...
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*\\.)*([a-zA-Z_][a-zA-Z0-9_]*(?:Exception|Error))(?::\\s*([^\r\n]+))?");

    // 匹配堆栈帧: at com.example.service.UserServiceImpl.getUser(UserServiceImpl.java:42)
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "at\\s+([a-zA-Z0-9_$.]+)\\.([a-zA-Z0-9_$]+)\\((?:Native Method|Unknown Source|([a-zA-Z0-9_$]+\\.java):(\\d+))\\)");

    // 匹配 javac 编译报错: [ERROR] /path/to/Main.java:[15,22] cannot find symbol
    private static final Pattern JAVAC_ERROR_PATTERN = Pattern.compile(
            "(?:\\[ERROR\\]\\s+)?(?:[^:\r\n]+):(?:\\[(\\d+),(\\d+)\\]|(\\d+):)\\s*(cannot find symbol|package [\\w.]+ does not exist|incompatible types|unreported exception[^\r\n]+)");

    // 匹配 Spring 端口被占用
    private static final Pattern PORT_IN_USE_PATTERN = Pattern.compile(
            "(?i)Port\\s+(\\d+)\\s+was already in use");

    // 匹配 Spring Bean 缺失
    private static final Pattern NO_SUCH_BEAN_PATTERN = Pattern.compile(
            "(?i)No qualifying bean of type ['\"]?([\\w.]+)['\"]? available");

    // 匹配 MyBatis Invalid bound statement
    private static final Pattern MYBATIS_BOUND_PATTERN = Pattern.compile(
            "(?i)Invalid bound statement \\(not found\\):\\s*([\\w.]+)");

    public JavaErrorInfo parse(String text) {
        if (text == null || text.isBlank()) {
            return JavaErrorInfo.empty(null);
        }

        // 1. 检查 Spring Boot 常见启动错误
        Matcher portMatcher = PORT_IN_USE_PATTERN.matcher(text);
        if (portMatcher.find()) {
            String port = portMatcher.group(1);
            return new JavaErrorInfo(
                    true,
                    "SPRING_FRAMEWORK",
                    "PortInUseException",
                    "端口 " + port + " 已被占用，服务启动失败",
                    null,
                    List.of("PortInUseException", "端口占用", port),
                    "请使用 netstat/lsof 检查端口占用进程，或在 application.yml 中修改 server.port。");
        }

        Matcher beanMatcher = NO_SUCH_BEAN_PATTERN.matcher(text);
        if (beanMatcher.find()) {
            String beanClass = beanMatcher.group(1);
            return new JavaErrorInfo(
                    true,
                    "SPRING_FRAMEWORK",
                    "NoSuchBeanDefinitionException",
                    "Spring 容器中未找到符合类型的 Bean: " + beanClass,
                    null,
                    List.of("NoSuchBeanDefinitionException", beanClass, "Spring Bean"),
                    "请检查目标类是否添加了 @Component/@Service/@Repository 注解，以及是否在 @ComponentScan 扫描路径内。");
        }

        // 2. 检查 MyBatis 映射绑定错误
        Matcher mybatisMatcher = MYBATIS_BOUND_PATTERN.matcher(text);
        if (mybatisMatcher.find()) {
            String statement = mybatisMatcher.group(1);
            return new JavaErrorInfo(
                    true,
                    "MYBATIS_DATABASE",
                    "BindingException",
                    "MyBatis 映射语句未找到: " + statement,
                    null,
                    List.of("BindingException", "Invalid bound statement", statement),
                    "请检查 Mapper 接口全限定名与 XML namespace 是否一致，以及 XML 中的 id 是否与方法名一致，并确认 XML 已被打包加载。");
        }

        // 3. 检查 Java 编译错误
        Matcher javacMatcher = JAVAC_ERROR_PATTERN.matcher(text);
        if (javacMatcher.find()) {
            String line = javacMatcher.group(1) != null ? javacMatcher.group(1) : javacMatcher.group(3);
            String issue = javacMatcher.group(4);
            return new JavaErrorInfo(
                    true,
                    "COMPILATION_BUILD",
                    "JavaCompilerError",
                    issue != null ? issue : "Java 编译错误",
                    line != null ? "Line " + line : null,
                    List.of("编译错误", issue != null ? issue : "javac"),
                    "请检查类或符号是否正确 import、类路径依赖 pom.xml 是否完整引入，或是否存在拼写错误。");
        }

        // 4. 检查标准 Java 异常堆栈
        Matcher exMatcher = EXCEPTION_PATTERN.matcher(text);
        String foundExceptionClass = null;
        String foundMessage = null;
        while (exMatcher.find()) {
            String fullOrShortEx = exMatcher.group(2);
            // 避免匹配一些普通带 Exception 单词的英文句子
            if (fullOrShortEx != null && fullOrShortEx.endsWith("Exception") || fullOrShortEx.endsWith("Error")) {
                foundExceptionClass = fullOrShortEx;
                foundMessage = exMatcher.group(3);
                break;
            }
        }

        // 提取用户代码调用栈位置（过滤掉 jdk/spring 等内部框架栈）
        String userCodeLocation = null;
        Matcher stackMatcher = STACK_FRAME_PATTERN.matcher(text);
        while (stackMatcher.find()) {
            String className = stackMatcher.group(1);
            String method = stackMatcher.group(2);
            String file = stackMatcher.group(3);
            String line = stackMatcher.group(4);
            if (className != null && !isFrameworkClass(className)) {
                userCodeLocation = className + "." + method + "(" + (file != null ? file : "Unknown") + (line != null ? ":" + line : "") + ")";
                break;
            }
        }

        if (foundExceptionClass != null) {
            String category = resolveCategory(foundExceptionClass);
            List<String> keywords = new ArrayList<>();
            keywords.add(foundExceptionClass);
            if (foundMessage != null && !foundMessage.isBlank()) {
                String cleanMsg = foundMessage.replaceAll("[\"'`]", "").trim();
                if (cleanMsg.length() > 40) {
                    cleanMsg = cleanMsg.substring(0, 40);
                }
                keywords.add(cleanMsg);
            }
            return new JavaErrorInfo(
                    true,
                    category,
                    foundExceptionClass,
                    foundMessage != null ? foundMessage.trim() : foundExceptionClass,
                    userCodeLocation,
                    keywords,
                    generateHint(foundExceptionClass, userCodeLocation));
        }

        // 兜底：未能匹配到标准格式
        return JavaErrorInfo.empty(extractGenericKeywords(text));
    }

    private boolean isFrameworkClass(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("jdk.")
                || className.startsWith("org.springframework.")
                || className.startsWith("org.apache.")
                || className.startsWith("com.baomidou.")
                || className.startsWith("org.mybatis.");
    }

    private String resolveCategory(String exClass) {
        if (exClass.contains("Bean") || exClass.contains("Spring") || exClass.contains("Context")) {
            return "SPRING_FRAMEWORK";
        }
        if (exClass.contains("Sql") || exClass.contains("MyBatis") || exClass.contains("Jdbc") || exClass.contains("Persistence")) {
            return "MYBATIS_DATABASE";
        }
        if (exClass.contains("IO") || exClass.contains("File") || exClass.contains("Socket")) {
            return "IO_NETWORK";
        }
        return "RUNTIME_EXCEPTION";
    }

    private String generateHint(String exClass, String userLocation) {
        StringBuilder sb = new StringBuilder();
        if ("NullPointerException".equals(exClass)) {
            sb.append("典型的空指针异常。请检查变量是否未初始化或未被正确赋值");
        } else if ("ClassNotFoundException".equals(exClass) || "NoClassDefFoundError".equals(exClass)) {
            sb.append("类未找到。请检查 pom.xml 是否遗漏依赖，或构建打包时是否缺失相关 jar");
        } else if ("ClassCastException".equals(exClass)) {
            sb.append("类型转换异常。对象实际类型与强制转换的目标类型不兼容");
        } else if ("IndexOutOfBoundsException".equals(exClass) || "ArrayIndexOutOfBoundsException".equals(exClass)) {
            sb.append("集合或数组越界。访问索引超出了当前数据容量");
        } else {
            sb.append("发生 ").append(exClass).append("。");
        }
        if (userLocation != null) {
            sb.append("；优先检查你的源码位置: ").append(userLocation);
        }
        return sb.toString();
    }

    private String extractGenericKeywords(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("spring")) return "Spring";
        if (lower.contains("mybatis")) return "MyBatis";
        if (lower.contains("maven")) return "Maven";
        if (lower.contains("java")) return "Java";
        return text.length() > 20 ? text.substring(0, 20).trim() : text.trim();
    }
}
