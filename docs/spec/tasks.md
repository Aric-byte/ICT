# CloudLab Copilot 安全审查修复 — 编码任务列表

> 本文档由 spec-task-agent 生成，对应需求规格：`docs/spec/requirements.md`，技术设计：`docs/spec/design.md`
> 覆盖 42 项问题（3 严重 / 7 高危 / 20 中危 / 12 低危），按 9 个实施阶段 P0–P9 拆解为可执行编码任务，并附集成验证、部署配置、代码审查三组收尾任务。
> 后端 Java 基础包路径：`backend/boot/src/main/java/com/cloudlabcopilot/boot/`（下文简称 `…/boot/`）
> 后端资源路径：`backend/boot/src/main/resources/`（下文简称 `…/resources/`）

---

## 1. 修复容器启动致命缺陷（P0 — 严重）

> 对应问题：C3（Redis healthcheck 无法通过）、C1（数据卷持久化缺失）、C2（CORS 任意来源携带凭据）
> 依赖关系：C3 为功能性缺陷，修复后 compose 方可正常启动；C1 依赖顶层 volumes 声明；C2 依赖新增 SecurityProperties（本组先建配置骨架）

### 1.1 修复 Redis healthcheck 带密码认证（C3）
- [ ] 修改 `docker-compose.yml` 中 redis 服务 healthcheck 的 `test` 为 `["CMD-SHELL", "redis-cli -a $$REDIS_PASSWORD ping | grep -q PONG"]`，使 healthcheck 携带认证通过
- [ ] 验证：执行 `docker compose up redis`，`docker inspect` 健康状态为 `healthy`，backend 能正常启动

### 1.2 为 PostgreSQL 与 Redis 添加命名数据卷持久化（C1）
- [ ] 在 `docker-compose.yml` 顶层声明 `volumes: pg-data:` 与 `redis-data:`
- [ ] 为 postgres 服务挂载 `pg-data:/var/lib/postgresql/data`，为 redis 服务挂载 `redis-data:/data`
- [ ] 验证：`docker compose down && docker compose up` 后 PG/Redis 存量数据不丢失

### 1.3 新增 SecurityProperties 配置属性绑定类（C2 前置）
- [ ] 在 `…/boot/config/` 新增 `SecurityProperties.java`，使用 `@ConfigurationProperties(prefix = "app.security")` + `@Validated`，内含 `Cors`（`allowedOrigins: List<String>` 标注 `@NotEmpty`、`allowCredentials: boolean`、`allowedMethods`、`allowedHeaders`、`maxAge`）与 `Multipart`（`maxFileSize`、`maxRequestSize` 标注 `@NotBlank`）两个 record 组件
- [ ] 在 `…/resources/application.yml` 新增 `app.security.cors.*` 与 `app.security.multipart.*` 配置项，dev 白名单为 `http://localhost:5173,http://localhost:8080`，multipart 限制 `50MB/100MB`
- [ ] 验证：启动时 `allowedOrigins` 为空则 fail-fast 报 `ConstraintViolationException`

### 1.4 收紧 CORS 白名单禁用通配来源（C2）
- [ ] 改造 `…/boot/config/WebMvcConfig.java`：注入 `SecurityProperties`，`addCorsMappings` 中用 `allowedOrigins(cors.allowedOrigins().toArray())` 替代 `allowedOriginPatterns("*")`，方法/头/maxAge 从配置读取，保持 `allowCredentials(true)`
- [ ] 在 `WebMvcConfig` 类上添加 `@EnableConfigurationProperties(SecurityProperties.class)`
- [ ] 验证：`curl -H "Origin: https://evil.com" -I http://localhost:8080/api/test` 响应不含 `Access-Control-Allow-Origin: https://evil.com`

---

## 2. 加固容器编排与镜像（P1 — 高/中危）

> 对应问题：H1, H2, H3, M16, M17, L6, L10, L11
> 依赖关系：均改 Dockerfile/compose，互不冲突，组内可并行

### 2.1 收敛数据库端口不暴露宿主机（H1）
- [ ] 修改 `docker-compose.yml`，删除 postgres 的 `5432:5432` 与 redis 的 `6379:6379` 端口映射，仅保留容器间网络互访；或改为 `127.0.0.1:5432:5432` 仅本地可达
- [ ] 验证：宿主机 `netstat` 不见 5432/6379 对外监听，backend 仍可经 compose 网络连接

### 2.2 后端容器以非 root 用户运行（H2）
- [ ] 修改 `backend/Dockerfile` 运行阶段：`RUN addgroup -S app && adduser -S -G app -h /app -u 1000 app`，`COPY --chown=app:app` 应用文件，末尾 `USER app`
- [ ] 验证：`docker exec <backend> id` 输出 `uid=1000(app)`，应用正常启动

### 2.3 新增 .dockerignore 排除敏感与构建产物（H3）
- [ ] 新增 `backend/.dockerignore`，排除 `target/`、`.idea/`、`*.iml`、`logs/`、`.env`、`.env.*`、`*.log`
- [ ] 新增 `frontend/.dockerignore`，排除 `node_modules/`、`dist/`、`.env`、`.env.local`、`*.log`
- [ ] 验证：`docker build` 构建上下文不含上述目录，镜像体积减小

### 2.4 补充容器 restart 策略、资源限制、网络隔离与日志轮转（M16）
- [ ] 在 `docker-compose.yml` 为所有服务添加 `restart: unless-stopped`
- [ ] 为 backend 添加 `mem_limit: 1g`、`cpus: "2"`；为 postgres/redis 添加 `mem_limit: 512m`
- [ ] 顶层声明自定义 `networks: appnet:`，各服务接入 `appnet`，移除默认网络
- [ ] 为各服务添加 `logging: driver: json-file options: max-size: "10m" max-file: "3"` 限制日志体积
- [ ] 验证：`docker compose up` 后 `docker inspect` 各服务含 restart 策略与资源限制

### 2.5 去掉 Dockerfile 中吞掉依赖下载错误的 `|| true`（M17）
- [ ] 修改 `backend/Dockerfile`，将 `RUN mvn dependency:go-offline -B || true` 改为 `RUN mvn dependency:go-offline -B`，依赖下载失败直接终止构建
- [ ] 验证：模拟依赖不可达时 `docker build` 失败退出

### 2.6 为后端镜像添加 OCI 规范 LABEL 元数据（L6）
- [ ] 在 `backend/Dockerfile` 添加 `LABEL org.opencontainers.image.title/source/description/version/created` 等元数据
- [ ] 验证：`docker inspect <image>` 含完整 OCI label

### 2.7 拆分前后端环境变量文件（L10）
- [ ] 将 `docker-compose.yml` 的 `env_file: .env` 拆分为 backend 引用 `backend/.env`、frontend 引用 `frontend/.env`，消除前后端变量混用
- [ ] 同步更新 `.env.example` 拆分为 `backend/.env.example` 与 `frontend/.env.example`
- [ ] 验证：各服务仅注入自身所需变量，`docker exec env` 无冗余变量

### 2.8 收敛 Dockerfile jar 通配符为明确文件名（L11）
- [ ] 修改 `backend/Dockerfile` 中 `COPY --from=builder /build/boot/target/*.jar app.jar` 为明确 `cloudlab-copilot-boot-*.jar` 或固定 `app.jar`，避免多 jar 误拷
- [ ] 验证：构建产物仅含预期 jar

---

## 3. 收紧后端安全配置（P2 — 高/中危）

> 对应问题：H6, M2, M1, M9, M4, L9
> 依赖关系：改 application*.yml + .env.example，组内按配置项串行

### 3.1 默认 Profile 改为 prod 防止开发模式误启动（H6）
- [ ] 修改 `.env.example`（拆分后为 `backend/.env.example`）中 `SPRING_PROFILES_ACTIVE` 默认值从 `dev` 改为 `prod`
- [ ] 在启动脚本 `scripts/verify-startup.sh` 中增加校验：若 `SPRING_PROFILES_ACTIVE=dev` 则警告或拒绝启动
- [ ] 验证：未设置 profile 时以 prod 启动，Actuator 不暴露详情

### 3.2 去掉数据库凭据占位符默认值实现 fail-fast（M2）
- [ ] 修改 `…/resources/application.yml` 中 `${DB_USERNAME:your-db-username-here}` 与 `${DB_PASSWORD:your-db-password-here}`，去掉默认值改为 `${DB_USERNAME}` / `${DB_PASSWORD}`，缺失环境变量时启动直接失败
- [ ] 同步检查 Redis、MaaS、OBS 等凭据占位符，一并去掉不安全默认值
- [ ] 验证：不设置 DB 凭据时启动报占位符解析失败

### 3.3 收紧 Actuator 健康详情暴露级别（M1）
- [ ] 修改 `…/resources/application-dev.yml` 中 `management.endpoint.health.show-details` 从 `always` 改为 `when_authorized`，避免暴露 DB/Redis 连接信息
- [ ] 验证：dev 环境 `GET /actuator/health` 未认证时不返回 DB/Redis 组件详情

### 3.4 修正 Jackson 全局日期格式为 ISO-8601（M9）
- [ ] 修改 `…/resources/application.yml` 中 `spring.jackson.date-format` 从 `yyyy-MM-dd HH:mm:ss` 改为 `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`，并确保 `time-zone` 配置正确
- [ ] 验证：API 返回的日期字段含时区与毫秒精度

### 3.5 生产环境降级 MyBatis-Plus SQL 日志级别（M4）
- [ ] 修改 `…/resources/application-prod.yml`，将 MyBatis-Plus `log-impl` 关闭或日志级别降为 `WARN`，避免生产环境输出完整 SQL 泄露敏感数据
- [ ] 验证：prod profile 下 SQL 日志不输出参数值

### 3.6 配置 Tomcat 与 multipart 上传限制（L9）
- [ ] 在 `…/resources/application.yml` 添加 `server.tomcat.max-threads`、`max-connections` 等线程池配置
- [ ] 添加 `spring.servlet.multipart.max-file-size: 50MB` 与 `max-request-size: 100MB`（引用 `app.security.multipart.*`），为证据链上传设限
- [ ] 验证：上传超过 50MB 文件返回 `PAYLOAD_TOO_LARGE` 业务码

---

## 4. 增强后端骨架代码（P3 — 中危）

> 对应问题：M7, L2, M5, M6, M3, L1, L3, M10
> 依赖关系：M7（TraceIdFilter）先于 L2（ApiResponse 读 MDC）；L1 改名需全局替换引用；M10 新增 V2 迁移

### 4.1 新增 TraceIdFilter 注入 MDC 链路标识（M7）
- [ ] 在 `…/boot/common/filter/` 新增 `TraceIdFilter.java`，继承 `OncePerRequestFilter`：优先读请求头 `X-Trace-Id`，校验格式 `[a-fA-F0-9-]{8,36}`，非法或缺失则生成 UUID；`MDC.put("traceId", traceId)`，`finally` 块 `MDC.clear()`
- [ ] 在配置类中注册 `FilterRegistrationBean<TraceIdFilter>`，`setOrder(Ordered.HIGHEST_PRECEDENCE)`，`addUrlPatterns("/*")`
- [ ] 在 `…/boot/common/constant/AppConstants.java` 新增 `TRACD_ID_MDC_KEY = "traceId"` 常量
- [ ] 验证：请求任意 API，响应头 `X-Trace-Id` 非空，日志含 `[traceId=xxx]`，线程池复用无串流

### 4.2 改造 ApiResponse 工厂方法从 MDC 读取 traceId（L2）
- [ ] 修改 `…/boot/common/response/ApiResponse.java` 的 `success` / `error` 静态工厂方法，traceId 从 `MDC.get(AppConstants.TRACD_ID_MDC_KEY)` 读取替代硬编码 `null`
- [ ] 新增私有 `currentTraceId()` 方法封装 MDC 读取逻辑
- [ ] 验证：API 响应 body `traceId` 字段非 null 且与响应头一致（依赖 4.1 完成）

### 4.3 扩展 GlobalExceptionHandler 补全 5 类异常映射（M5）
- [ ] 在 `…/boot/common/response/ResultCode.java` 新增 `METHOD_NOT_ALLOWED(405)`、`PAYLOAD_TOO_LARGE(413)`、`UNSUPPORTED_MEDIA_TYPE(415)` 三个枚举值
- [ ] 在 `…/boot/common/exception/GlobalExceptionHandler.java` 新增 5 个 `@ExceptionHandler` 方法：`ConstraintViolationException`→`PARAM_ERROR`、`MissingServletRequestParameterException`→`PARAM_ERROR`、`MethodArgumentTypeMismatchException`→`PARAM_ERROR`、`HttpRequestMethodNotSupportedException`→`METHOD_NOT_ALLOWED`、`MaxUploadSizeExceededException`→`PAYLOAD_TOO_LARGE`，日志级别 `warn`，返回 `ApiResponse` 保持既有约定
- [ ] 验证：发送缺参/类型不匹配/错误方法/超大上传请求，返回精确业务码 400/405/413

### 4.4 脱敏 HttpMessageNotReadableException 日志（M6）
- [ ] 修改 `…/boot/common/exception/GlobalExceptionHandler.java` 中 `HttpMessageNotReadableException` handler，将 `log.warn("请求体不可读: {}", e.getMessage())` 改为 `log.warn("请求体不可读: {}", e.getClass().getSimpleName())`，不记录可能含敏感请求体的 message
- [ ] 验证：发送含敏感字段的非法 JSON body，日志仅记异常类型名不含请求体内容

### 4.5 为 MyBatis-Plus 追加防全表操作与乐观锁拦截器（M3）
- [ ] 修改 `…/boot/config/MyBatisPlusConfig.java` 的 `mybatisPlusInterceptor()` Bean，在 `PaginationInnerInterceptor` **之前**添加 `BlockAttackInnerInterceptor`（阻止无 where 的 update/delete），之后添加 `OptimisticLockerInnerInterceptor`（基于 `@Version` 字段乐观锁）
- [ ] 验证：执行无 where 的 update/delete 被 `BlockAttackInnerInterceptor` 拦截报错

### 4.6 重命名 ValidationException 为 BizValidationException 消除同名冲突（L1）
- [ ] 将 `…/boot/common/exception/ValidationException.java` 重命名为 `BizValidationException.java`，保持继承 `BusinessException` 与构造契约不变
- [ ] 全局搜索替换所有引用 `ValidationException` 为 `BizValidationException`（排除 `jakarta.validation.ValidationException`）
- [ ] 验证：编译通过，无 import 歧义

### 4.7 收敛 app_config.config_value 字段长度（L3）
- [ ] 修改 `…/resources/db/migration/V1__init.sql` 中 `config_value TEXT` 改为 `config_value VARCHAR(4096)`，或保留 TEXT 但在应用层加长度校验
- [ ] 验证：插入超过 4096 字符的 config_value 被拒绝

### 4.8 新增 V2 迁移修复 app_config 审计字段（M10）
- [ ] 在 `…/resources/db/migration/` 新增 `V2__audit_columns.sql`：`ALTER TABLE app_config ADD COLUMN IF NOT EXISTS created_at TIMESTAMP`；回填 `created_at = updated_at`；`CREATE OR REPLACE FUNCTION set_updated_at()` 返回 `NEW.updated_at := CURRENT_TIMESTAMP`；`DROP TRIGGER IF EXISTS trg_app_config_updated_at` 后 `CREATE TRIGGER ... BEFORE UPDATE`
- [ ] 验证：更新 `app_config` 行后 `updated_at` 自动刷新为当前时间，`created_at` 保持不变；迁移可重复执行不报错

---

## 5. 完善日志与可观测性（P4 — 中/低危）

> 对应问题：M8, L8
> 依赖关系：改 logback-spring.xml

### 5.1 修复 logback prod JSON 格式未转义问题（M8）
- [ ] 修改 `…/resources/logback-spring.xml`，prod profile 下引入 `logstash-logback-encoder` 的 `LogstashEncoder` 输出结构化 JSON，或对 `%msg` 做转义防止日志注入
- [ ] 验证：prod 环境日志为合法 JSON，特殊字符不破坏格式

### 5.2 评估并明确 logback 文件滚动 appender 策略（L8）
- [ ] 评估容器化场景下是否需要文件滚动 appender；若不需要在 `logback-spring.xml` 注释说明"容器化仅输出 stdout，由 Docker logging driver 收集"
- [ ] 若需要则添加 `RollingFileAppender` 配置 `maxFileSize` 与 `maxBackupIndex`
- [ ] 验证：日志输出策略与文档说明一致

---

## 6. 加固前端构建与运行时（P5 — 中/低危）

> 对应问题：M12, M14, M15, M13, M11, L7
> 依赖关系：M15（ErrorBoundary）先于 M14（包裹路由）；M13 依赖 toast 库引入

### 6.1 修正 vite.config.ts alias 路径解析（M12）
- [ ] 修改 `frontend/vite.config.ts`，将 alias `@` 路径用 `fileURLToPath(new URL('./src', import.meta.url))` 替代可能不一致的相对路径写法
- [ ] 验证：`import '@/...'` 解析正确，`vite build` 无路径告警

### 6.2 新增 ErrorBoundary 组件捕获渲染异常（M15）
- [ ] 在 `frontend/src/components/` 新增 `ErrorBoundary.tsx`，类组件实现 `getDerivedStateFromError` + `componentDidCatch`，支持 `fallback` props 显示降级 UI，`onError` 回调可上报后端
- [ ] 在 `frontend/src/App.tsx` 用 `<ErrorBoundary fallback={<ErrorFallback />}>` 包裹 `RouterProvider`
- [ ] 验证：触发子组件渲染异常显示降级 UI 而非白屏

### 6.3 改造前端路由添加 404 兜底与懒加载（M14）
- [ ] 修改 `frontend/src/router/index.tsx`，用 `React.lazy(() => import(...))` 懒加载页面组件，用 `Suspense` 包裹提供 fallback
- [ ] 在 `frontend/src/pages/` 新增 `NotFound.tsx` 404 兜底页，路由数组追加 `{ path: '*', element: <NotFound /> }`
- [ ] 验证：访问不存在路径显示 404 页，页面按需加载减小首屏 bundle（依赖 6.2 ErrorBoundary 完成）

### 6.4 扩展 axios 响应拦截器统一处理业务错误码（M13）
- [ ] 修改 `frontend/src/api/request.ts`，response interceptor 中读取 `response.data.code`，非 200 时 `message.error` 提示并 `reject(new BizError(code, message))`
- [ ] 新增 error interceptor 处理 HTTP 状态码：401 跳转登录、403 提示无权限、≥500 提示服务异常，不暴露技术细节
- [ ] 在 `frontend/src/types/` 新增 `BizError` 类型（`Error` 子类携带 `code: number`）
- [ ] 验证：后端返回业务错误码时自动 toast，网络错误统一提示

### 6.5 构建期校验前端 .env.production 占位符（M11）
- [ ] 在 `frontend/` 新增构建前置校验脚本（或 vite 插件），检查 `.env.production` 中 `VITE_API_BASE_URL` 等变量非占位符值，未替换则 `vite build` 失败
- [ ] 验证：保留占位符时构建报错退出

### 6.6 显式管理 npm 镜像源配置（L7）
- [ ] 修改 `frontend/.npmrc`，将固定 npmmirror 镜像改为 CI 环境变量注入或在 CI 脚本中显式 `npm config set registry`，避免本地配置污染 CI
- [ ] 验证：CI 构建使用预期 registry

---

## 7. 加固 Nginx 反向代理（P6 — 高/中危）

> 对应问题：H5, H7, M19
> 依赖关系：改 nginx.conf，需配合证书/域名

### 7.1 添加安全响应头与 TLS 配置（H5）
- [ ] 修改 `frontend/nginx.conf`，主 server 块添加 `server_tokens off`、`add_header X-Frame-Options DENY`、`add_header X-Content-Type-Options nosniff`、`add_header Strict-Transport-Security "max-age=31536000; includeSubDomains"`、`add_header Content-Security-Policy "default-src 'self'"`
- [ ] 新增 443 server 块配置 `ssl` + `ssl_protocols TLSv1.2 TLSv1.3`（可选自托管方案），80 server 块 `return 301 https://$host$request_uri` 跳转；默认方案由外置 ALB/Ingress 终止 TLS，nginx 仅监听 80
- [ ] 验证：`curl -I https://localhost` 响应含全部 4 个安全响应头

### 7.2 收紧 server_name 拒绝任意 Host 头（H7）
- [ ] 修改 `frontend/nginx.conf`，将 `server_name _` 改为具体域名 `cloudlab.example.edu.cn`
- [ ] 新增默认 server 块 `listen 80 default_server` + `return 444` 拦截非匹配 Host
- [ ] 验证：非匹配 Host 请求被拒绝，匹配 Host 正常响应

### 7.3 配置代理后端超时与 SSE 友好选项（M19）
- [ ] 修改 `frontend/nginx.conf` 的 `location /api/` 块，添加 `proxy_read_timeout 300s`、`proxy_send_timeout 300s`、`proxy_buffering off`（SSE 流式响应不被缓冲）、`client_max_body_size 100m`
- [ ] 验证：SSE 接口流式输出不超时，大文件上传不被 1m 默认限制拒绝

---

## 8. 升级依赖修复已知 CVE（P7 — 高危）

> 对应问题：H4
> 依赖关系：patch/minor 级升级后跑全量构建验证；ESLint 9 major 延后

### 8.1 升级后端 Maven 依赖修复 CVE（H4）
- [ ] 修改 `backend/pom.xml` 中 `spring-boot.version` 从 `3.2.5` 升至 `3.2.12`，修复 CVE-2024-22243/22259/22262
- [ ] 升级 PostgreSQL JDBC 从 `42.7.3` 至 `42.7.4+`，MyBatis-Plus 从 `3.5.5` 至 `3.5.9+`
- [ ] 执行 `mvn dependency:tree` 确认无版本冲突，`mvn clean compile` 编译通过
- [ ] 验证：启动 + Actuator health + 现有骨架编译正常，分页与防全表拦截器单测通过

### 8.2 升级前端 npm 依赖修复 CVE（H4）
- [ ] 修改 `frontend/package.json`，axios 从 `1.7.7` 升至 `≥1.7.8`（修复 CVE-2024-39338 SSRF），Vite 从 `5.4.3` 升至 `≥5.4.10`（修复 CVE-2024-45810 preview XSS）
- [ ] 执行 `npm install` + `npm audit` 确认无已知 CVE
- [ ] 验证：前端 `npm run dev` 与 `npm run build` 正常，request.ts 拦截器功能不变

### 8.3 评估 ESLint 9 迁移并标注延后风险（L12 / H4）
- [ ] 在 `frontend/.eslintrc.cjs` 顶部注释标注 ESLint 8.57.0 已 EOL，待前端业务代码落地时迁移至 9.x flat config（`eslint.config.js`）
- [ ] 本次不执行 major 版本升级，记录延后理由与迁移路径
- [ ] 验证：当前 `npm run lint` 仍通过，延后决策已文档化

---

## 9. 引入测试框架与工程规范（P8 — 低危）

> 对应问题：L4, L5, L12
> 依赖关系：新增依赖与测试目录

### 9.1 引入后端 JUnit5 测试骨架与示例（L4）
- [ ] 在 `backend/boot/src/test/java/com/cloudlabcopilot/boot/` 新增测试目录与基类，编写 `ApiResponseTest`、`GlobalExceptionHandlerTest` 示例验证 traceId 与异常映射
- [ ] 确认 `backend/boot/pom.xml` 已含 `spring-boot-starter-test`（含 JUnit5），若无则添加
- [ ] 验证：`mvn test` 执行通过，测试报告生成

### 9.2 引入前端 vitest 测试框架与示例（L4）
- [ ] 在 `frontend/package.json` 添加 `vitest` 与 `@testing-library/react` devDependencies，新增 `vitest.config.ts`
- [ ] 在 `frontend/src/` 新增 `ErrorBoundary.test.tsx` 示例测试
- [ ] 验证：`npm run test` 执行通过

### 9.3 引入 JaCoCo 覆盖率插件（L5）
- [ ] 在 `backend/boot/pom.xml` 添加 `jacoco-maven-plugin`，配置 `prepare-agent` 与 `report` goal
- [ ] 验证：`mvn test` 后 `target/site/jacoco/index.html` 覆盖率报告生成

---

## 10. 修复脚本与清理敏感信息（P9 — 中危）

> 对应问题：M18, M20
> 依赖关系：M20 涉及 git 历史，需人工确认

### 10.1 修复 verify-startup.sh 中 npm ci 静默 fallback（M18）
- [ ] 修改 `scripts/verify-startup.sh`，将 `npm ci` 失败的静默 fallback 改为直接 `exit 1` 终止脚本，确保依赖安装失败不被吞掉
- [ ] 验证：模拟 `npm ci` 失败时脚本立即退出非零

### 10.2 确认并清理 resources/ 下竞赛敏感信息（M20）
- [ ] 审查 `resources/` 下 .docx 文件，确认文件名含"学校-队长姓名-手机号"等 PII 是否应公开
- [ ] 若不应公开：从工作区移除文件，必要时用 `git filter-repo` 清理历史（需人工确认后执行）
- [ ] 验证：`git log --all --full-history -- resources/` 确认敏感信息已清除或确认可公开

---

## 11. 集成验证与回归测试

> 确保各阶段修复在集成环境下协同生效，覆盖跨模块调用与端到端路径

### 11.1 容器编排全量启动验证
- [ ] 执行 `docker compose up --build`，确认 postgres/redis/backend/frontend 全部 `service_healthy`
- [ ] 验证 Redis healthcheck 带认证通过（C3）、数据卷持久化生效（C1）、backend 非 root 运行（H2）、端口未暴露宿主（H1）

### 11.2 后端安全基线端到端验证
- [ ] 验证 CORS 拒绝非白名单 Origin（C2）、Actuator 未认证不暴露详情（M1）、缺失凭据 fail-fast（M2）
- [ ] 验证 traceId 响应头与 body 一致且日志含 traceId（M7）、5 类异常返回精确业务码（M5）、日志不含敏感请求体（M6）
- [ ] 验证无 where 的 update/delete 被拦截（M3）、`app_config.updated_at` 触发器自动刷新（M10）

### 11.3 前端运行时端到端验证
- [ ] 验证 404 路由显示兜底页（M14）、ErrorBoundary 捕获渲染异常显示降级 UI（M15）
- [ ] 验证 axios 拦截器业务错误码 toast（M13）、`.env.production` 占位符构建失败（M11）

### 11.4 Nginx 与依赖回归验证
- [ ] 验证 nginx 安全响应头完整（H5）、非匹配 Host 被拒绝（H7）、SSE 代理不超时（M19）
- [ ] 执行 `mvn dependency:tree` + `npm audit` 确认无已知 CVE（H4），全量构建通过

### 11.5 全量启动脚本回归
- [ ] 执行 `scripts/verify-startup.sh --profile prod`，健康检查全部 UP
- [ ] 验证脚本中 `npm ci` 失败不再静默（M18），默认 profile 为 prod（H6）

---

## 12. 部署配置与上线保障

> 确保修复可安全上线，含环境配置、数据迁移、监控与文档

### 12.1 同步环境配置模板
- [ ] 更新 `backend/.env.example` 与 `frontend/.env.example`，补充 `app.security.cors.allowed-origins`、multipart 限制等新增配置项说明
- [ ] 确保所有去掉占位符默认值的凭据（M2）在 `.env.example` 中有明确文档
- [ ] 验证：按 `.env.example` 填写真实值后应用可启动

### 12.2 数据库迁移上线方案
- [ ] 编写 `V2__audit_columns.sql` 上线说明：prod 首次部署时 `FLYWAY_ENABLED=true` 执行迁移，后续关闭
- [ ] 确认迁移幂等：`ADD COLUMN IF NOT EXISTS` + `CREATE OR REPLACE FUNCTION` + `DROP TRIGGER IF EXISTS` 可重复执行
- [ ] 验证：在含存量数据的 PG 实例上执行 V2，`created_at` 回填正确，触发器生效

### 12.3 监控埋点与告警配置
- [ ] 确认 logback prod JSON 格式（M8）可被日志收集系统（如 Loki/ELK）解析，traceId 字段可检索
- [ ] 配置容器 `logging` 限制（M16）与 restart 策略，确保异常重启有告警
- [ ] 验证：日志可按 traceId 关联检索，容器 OOM/异常退出有告警通知

### 12.4 更新接口与运维文档
- [ ] 更新 `README.md` 或新增 `docs/deployment.md`，说明新增配置项、TLS 终止方案、证书挂载方式
- [ ] 更新 `docs/spec/` 下需求与设计文档状态为"已实施"
- [ ] 验证：文档与实际配置一致，部署步骤可复现

---

## 13. 代码审查与最终确认

> 最终验证确保交付质量，每项需有负责人与记录

### 13.1 关键代码审查
- [ ] 审查 `TraceIdFilter.java`：确认 MDC 清理在 finally 块、Filter 优先级最高、traceId 合法性校验防注入
- [ ] 审查 `GlobalExceptionHandler.java`：确认 5 类新增 handler 返回 ApiResponse 保持约定、M6 脱敏无请求体泄漏
- [ ] 审查 `SecurityProperties.java` + `WebMvcConfig.java`：确认 CORS 禁用 `*`+credentials、配置非空校验 fail-fast
- [ ] 审查 `docker-compose.yml`：确认端口不暴露宿主、数据卷挂载、非 root 用户、网络隔离、资源限制

### 13.2 设计与实现一致性核对
- [ ] 逐项核对 `docs/spec/design.md` 中 8 个技术分组 G1–G8 的设计决策与实际实现一致
- [ ] 核对 42 项问题（C1–C3、H1–H7、M1–M20、L1–L12）全部有对应任务且已实施
- [ ] 核对延后项（ESLint 9 / jacoco 评测模块 / M20 历史清理）有明确记录与后续计划

### 13.3 变更范围最终确认
- [ ] 执行 `git diff --stat` 确认变更文件范围与 `design.md` 3.3 影响范围矩阵一致，无意外文件改动
- [ ] 确认新增文件（`.dockerignore`、`TraceIdFilter.java`、`SecurityProperties.java`、`ErrorBoundary.tsx`、`NotFound.tsx`、`V2__audit_columns.sql`、`BizValidationException.java`）均已创建
- [ ] 确认删除文件（`ValidationException.java`）已移除且引用已替换
- [ ] 记录未通过项与跟进措施，确认无遗留风险