# CloudLab Copilot 安全审查与修复需求规格文档

> 本文档由 spec-requirement-agent 生成，对应需求：「研究一下这个项目，找一下漏洞和可以修复的地方」
> 项目处于骨架阶段（业务模块多为 Placeholder），问题集中在**基础设施配置、安全基线、骨架代码缺陷**三个层面。

---

## 🔴 严重（Critical）— 必须立即修复

### C1. docker-compose 中 PostgreSQL / Redis 没有数据卷持久化
**文件**：`docker-compose.yml`
**问题**：postgres 和 redis 服务都没有挂载 `volumes`，容器一旦重启或重建，**数据库和缓存数据全部丢失**。对于一个会话/证据链平台这是灾难性的。
**修复**：
```yaml
services:
  postgres:
    volumes:
      - pg-data:/var/lib/postgresql/data
  redis:
    volumes:
      - redis-data:/data
volumes:
  pg-data:
  redis-data:
```

### C2. CORS 配置允许任意来源携带凭据
**文件**：`backend/boot/src/main/java/com/cloudlabcopilot/boot/config/WebMvcConfig.java:13-16`
**问题**：`allowedOriginPatterns("*")` + `allowCredentials(true)` 是经典 CORS 漏洞。Spring 用 `allowedOriginPatterns` 绕过了浏览器"禁止 `*` + credentials"的限制，会**回显任意 Origin 头**，等价于"任何恶意网站都能携带 Cookie/Authorization 跨域调用本系统 API"，存在 CSRF 与敏感数据外泄风险。
**修复**：白名单显式域名，或开发期用 profile 区分：
```java
registry.addMapping("/api/**")
        .allowedOrigins("https://cloudlab.example.edu.cn")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
```

### C3. Redis healthcheck 未带密码，容器永远无法 healthy
**文件**：`docker-compose.yml:20-24`
**问题**：Redis 启动时设了 `--requirepass ${REDIS_PASSWORD}`，但 healthcheck 用 `redis-cli ping` 没有认证，会返回 `NOAUTH Authentication required`，**healthcheck 永远不通过**，导致 `backend`（`depends_on: redis: condition: service_healthy`）永远不启动。
**修复**：
```yaml
redis:
  command: redis-server --requirepass ${REDIS_PASSWORD}
  healthcheck:
    test: ["CMD-SHELL", "redis-cli -a $$REDIS_PASSWORD ping | grep -q PONG"]
```

---

## 🟠 高危（High）

### H1. PostgreSQL / Redis 端口直接暴露到宿主机
**文件**：`docker-compose.yml:4-5, 18-19`
**问题**：`5432:5432` 和 `6379:6379` 把数据库和 Redis 直接映射到宿主机所有网卡。
**修复**：仅暴露 backend 需要的端口，DB/Redis 不映射宿主机端口（容器间通过 compose 网络互访），或改为 `127.0.0.1:5432:5432`。

### H2. 后端容器以 root 运行
**文件**：`backend/Dockerfile:18-23`
**问题**：`eclipse-temurin:17-jre-alpine` 默认以 root 启动 java 进程，违反最小权限原则。
**修复**：创建非 root 用户 `app` 并 `USER app`。

### H3. 缺少 `.dockerignore`，`COPY . .` 可能泄露敏感文件
**文件**：`backend/Dockerfile:15`、`frontend/Dockerfile:5`
**问题**：没有 `.dockerignore`，`COPY . .` 会把 `target/`、`.idea/`、`logs/`、本地 `.env`、`*.iml` 等全部复制进构建上下文。
**修复**：新增 `backend/.dockerignore` 和 `frontend/.dockerignore`，排除构建产物与本地凭据。

### H4. 依赖存在已知 CVE（建议核实并升级）
| 依赖 | 当前版本 | 风险 | 建议 |
|------|---------|------|------|
| Spring Boot | 3.2.5 | CVE-2024-22243/22259/22262 | 升级到 3.2.12+ 或 3.3.x |
| axios | 1.7.7 | CVE-2024-39338（SSRF/凭证泄漏） | 升级到 ≥1.7.8 |
| Vite | 5.4.3 | CVE-2024-45810（preview server DOM XSS） | 升级到 ≥5.4.10 |
| PostgreSQL JDBC | 42.7.3 | 多个 bug 修复 | 升级到 42.7.4+ |
| MyBatis-Plus | 3.5.5 | SQL 注入相关修复 | 升级到 3.5.9+ |
| ESLint | 8.57.0 | 已 EOL | 迁移到 9.x |

### H5. Nginx 缺少安全响应头且明文 HTTP
**文件**：`frontend/nginx.conf`
**问题**：没有 `X-Frame-Options`、`X-Content-Type-Options`、`Strict-Transport-Security`、`Content-Security-Policy`，且仅监听 80 明文。
**修复**：添加安全响应头，生产配 443 + TLS，80 跳转，`server_tokens off`，禁止 dotfiles。

### H6. 默认 Profile 为 dev，生产易误启动为开发模式
**文件**：`.env.example:7`、`application.yml:5`
**问题**：`SPRING_PROFILES_ACTIVE=dev` 是默认值，生产部署若忘记设置会以 dev 启动，暴露 Actuator 详情与 DEBUG 日志。
**修复**：`.env.example` 默认改为 `prod`，启动脚本强制校验非 dev。

### H7. `nginx.conf` 中 `server_name _` 接受任意 Host 头
**文件**：`frontend/nginx.conf:3`
**问题**：`server_name _` 会响应任意 Host，易受 Host 头注入 / 缓存投毒。
**修复**：配置为具体域名，并加默认 server 拦截非匹配 Host。

---

## 🟡 中危（Medium）

### M1. Actuator dev 暴露健康详情
`application-dev.yml:14` — `show-details: always` 暴露 DB/Redis 连接信息。建议改为 `when_authorized`。

### M2. `application.yml` 默认占位符凭据
`application.yml:9-10` — `${DB_USERNAME:your-db-username-here}`。建议去掉默认值，缺失时直接启动失败。

### M3. MyBatis-Plus 缺少防全表操作插件
`MyBatisPlusConfig.java:15` — 缺 `BlockAttackInnerInterceptor` 和 `OptimisticLockerInnerInterceptor`。

### M4. MyBatis-Plus SQL 日志可能泄露敏感数据
prod profile 应降级为 `WARN` 或关闭 `log-impl`。

### M5. GlobalExceptionHandler 缺少多种异常映射
缺少 `ConstraintViolationException`、`MissingServletRequestParameterException`、`MethodArgumentTypeMismatchException`、`HttpRequestMethodNotSupportedException`、`MaxUploadSizeExceededException`。

### M6. `HttpMessageNotReadableException` 日志可能泄露敏感请求体
`GlobalExceptionHandler.java:37` — 只记日志类型，不记 message。

### M7. traceId 功能形同虚设
`ApiResponse.traceId` 永远为 `null`；无 `TraceIdFilter` 注入 MDC。建议新增 `OncePerRequestFilter`。

### M8. logback prod JSON 格式未转义
`logback-spring.xml:19` — 用 `logstash-logback-encoder` 或对 `%msg` 转义。

### M9. Jackson 全局日期格式丢失精度
`application.yml:25-26` — 改用 ISO-8601：`yyyy-MM-dd'T'HH:mm:ss.SSSXXX`。

### M10. `V1__init.sql` 中 `updated_at` 不会自动更新
`V1__init.sql:11` — PostgreSQL 不支持 `ON UPDATE`，需加触发器或应用层维护。同时缺少 `created_at` 字段。

### M11. 前端 `.env.production` 占位符未校验
`frontend/.env.production` — 构建时校验非占位符。

### M12. `vite.config.ts` alias 路径不一致
`vite.config.ts:16-18` — 用 `fileURLToPath(new URL('./src', import.meta.url))`。

### M13. 前端响应拦截器未处理业务错误码
`frontend/src/api/request.ts:18-25` — 统一处理业务码并 toast。

### M14. 前端 router 无 404 路由、无懒加载
`frontend/src/router/index.tsx` — 加 `path: '*'` 兜底，用 `lazy()` 懒加载。

### M15. 前端无 ErrorBoundary
新增 ErrorBoundary 组件包裹路由。

### M16. docker-compose 缺少 restart 策略、资源限制、网络隔离、日志限制
加 `restart: unless-stopped`、`mem_limit`/`cpus`、自定义 `networks`、`logging` 限制。

### M17. backend Dockerfile `|| true` 吞掉依赖下载错误
`backend/Dockerfile:14` — 去掉 `|| true`。

### M18. `verify-startup.sh` 中 `npm ci` 静默 fallback
`scripts/verify-startup.sh:56` — `npm ci` 失败直接 fail。

### M19. nginx 代理后端无超时/SSE 配置
`frontend/nginx.conf:11-17` — 加 `proxy_read_timeout`/`proxy_buffering off`/`client_max_body_size`。

### M20. `resources/` 下 .docx 含竞赛敏感信息
文件名含"学校-队长姓名-手机号"，可能包含 PII。建议确认是否应公开，必要时从历史清除。

---

## 🟢 低危（Low）/ 代码质量

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| L1 | `ValidationException.java` | 类名与 `jakarta.validation.ValidationException` 同名易混淆 | 改为 `BizValidationException` |
| L2 | `ApiResponse.java` | `traceId` 永远 null | 配合 M7 修复 |
| L3 | `V1__init.sql` | `config_value TEXT` 无长度上限 | 加 `VARCHAR(4096)` 或应用层校验 |
| L4 | 全项目 | 无单元测试框架 | 引入 JUnit5 + vitest |
| L5 | 全项目 | 无 jacoco 覆盖率 | 评测模块需要时引入 |
| L6 | `backend/Dockerfile` | 缺少 OCI LABEL | 加 `LABEL org.opencontainers.image.*` |
| L7 | `frontend/.npmrc` | 固定 npmmirror 镜像 | CI 中显式覆盖 |
| L8 | `logback-spring.xml` | 无文件滚动 appender | 容器化可接受，明确说明 |
| L9 | `application.yml` | 未配置 `server.tomcat.*` 和 `spring.servlet.multipart.*` | 证据链上传需配置 multipart 限制 |
| L10 | `docker-compose.yml:31` | `env_file: .env` 前后端变量混用 | 拆分前后端 env 文件 |
| L11 | `backend/pom.xml` | spring-boot-maven-plugin repackage 后 `*.jar` 通配 | 改为明确 `cloudlab-copilot-boot-*.jar` |
| L12 | `frontend/.eslintrc.cjs` | ESLint 8 + .cjs | 后续迁移 flat config |

---

## 📋 修复优先级建议

1. **立即修**：C1（数据持久化）、C2（CORS）、C3（Redis healthcheck）
2. **本周修**：H1–H7 — 容器安全、依赖 CVE、Nginx 安全头、默认 profile
3. **迭代修**：M1–M20 — 异常处理、日志、前端健壮性、docker-compose 加固
4. **opportunistically**：L1–L12 — 代码质量与工程规范

---

## ℹ️ 说明

- 项目处于**骨架阶段**（README 标注阶段 1 完成，阶段 2–8 待开发），业务模块均为空 `Placeholder` 接口，本次审查聚焦**基础设施、配置、骨架代码**。后续业务代码落地时建议再做一轮针对 SQL 注入、命令注入（safety 模块）、SSRF（MaaS 调用）、认证鉴权的专项审查。
- 依赖 CVE 部分基于版本号推断，建议用 `mvn dependency:tree` + OWASP Dependency-Check 插件、`npm audit` 做自动化扫描确认。