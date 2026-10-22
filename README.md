# CloudLab Copilot

> 基于华为云的 ICT 实验状态感知诊断与证据核验平台
>
> 第十一届华为 ICT 大赛中国创新赛 · 赛题一

## 项目简介

CloudLab Copilot 是面向高校学生和教师的 ICT 实验辅助平台。通过分析学生提交的命令/输出/报错文本，结合实验目标、当前步骤和历史执行记录，提供结构化诊断建议和最小安全下一步，同时自动构建学习证据链。

**核心能力**：
- 实验状态感知诊断（不只看单条报错，结合上下文）
- 最小安全下一步（一次只给一条建议，先过危险命令校验）
- 学习证据链（命令/输出/诊断/验证/截图自动记录）

## 项目结构

```
ICT/
├── .editorconfig               # 统一编辑器配置
├── .gitignore                  # Git 忽略规则
├── .env.example                # 环境变量模板（占位符，禁止填真实凭据）
├── docker-compose.yml          # 全栈编排（postgres + redis + backend + frontend）
├── README.md                   # 项目说明（本文件）
│
├── backend/                    # 后端 Spring Boot 多模块工程
│   ├── pom.xml                 # 父 POM（统一版本管理）
│   ├── Dockerfile              # 后端多阶段构建
│   ├── boot/                   # 启动聚合模块
│   ├── lab/                    # 实验管理模块
│   ├── session/                # 会话状态模块
│   ├── knowledge/              # 结构化知识模块
│   ├── retrieval/              # 检索模块
│   ├── diagnosis/              # 诊断模块
│   ├── safety/                 # 安全审查模块
│   ├── evidence/               # 证据链模块
│   ├── report/                 # 报告模块
│   └── evaluation/             # 评测模块
│
├── frontend/                   # 前端 React + Vite 工程
│   ├── package.json            # 依赖与脚本
│   ├── vite.config.ts          # Vite 配置（含开发代理）
│   ├── Dockerfile              # 前端多阶段构建
│   ├── nginx.conf              # Nginx SPA 回退配置
│   └── src/                    # 源代码
│       ├── main.tsx            # 应用入口
│       ├── App.tsx             # 根组件（挂载路由）
│       ├── router/             # React Router 路由配置
│       ├── store/              # Zustand 状态管理
│       ├── api/                # axios 请求层
│       ├── pages/              # 页面组件
│       ├── components/         # 通用组件
│       ├── types/              # TypeScript 类型定义
│       └── utils/              # 工具函数
│
└── scripts/
    └── verify-startup.sh       # 一键启动验证脚本
```

## 后端模块说明

| 模块 | artifactId | 说明 |
|------|-----------|------|
| **boot** | cloudlab-copilot-boot | 启动聚合模块，聚合九大业务模块，提供主启动类、全局配置、统一响应体、全局异常处理 |
| **lab** | cloudlab-copilot-lab | 实验管理：实验定义、步骤、前置条件 |
| **session** | cloudlab-copilot-session | 会话状态：实验执行会话、状态感知、历史记录 |
| **knowledge** | cloudlab-copilot-knowledge | 结构化知识：知识卡片、版本匹配、元数据管理 |
| **retrieval** | cloudlab-copilot-retrieval | 检索：关键词检索、向量 RAG、版本匹配资料召回 |
| **diagnosis** | cloudlab-copilot-diagnosis | 诊断：MaaS 错误解释、结构化下一步、状态感知诊断 |
| **safety** | cloudlab-copilot-safety | 安全审查：危险命令规则校验、最小安全下一步 |
| **evidence** | cloudlab-copilot-evidence | 证据链：命令/输出/诊断/验证/截图记录与缺项发现 |
| **report** | cloudlab-copilot-report | 报告：实验报告生成、缺项检查、摘要 |
| **evaluation** | cloudlab-copilot-evaluation | 评测：评测集管理、指标计算、真实评测产出 |

### boot 模块核心类

| 文件 | 说明 |
|------|------|
| `CloudLabApplication.java` | 主启动类，`@SpringBootApplication`，扫描 `com.cloudlabcopilot` |
| `config/ActuatorConfig.java` | Actuator 配置，仅暴露 `/actuator/health` |
| `config/MyBatisPlusConfig.java` | MyBatis-Plus 分页插件配置（PostgreSQL） |
| `config/WebMvcConfig.java` | Web MVC CORS 配置 |
| `common/response/ApiResponse.java` | 统一响应体 `ApiResponse<T>`，含 success/error 工厂方法 |
| `common/response/ResultCode.java` | 响应码枚举（200/400/404/500） |
| `common/exception/GlobalExceptionHandler.java` | 全局异常处理，拦截 5 类异常统一转 ApiResponse |
| `common/exception/BusinessException.java` | 业务异常基类 |
| `common/exception/ValidationException.java` | 校验异常 |
| `common/constant/AppConstants.java` | 公共常量 |

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 后端语言 | Java | 17 LTS |
| 后端框架 | Spring Boot | 3.2.5 |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 数据库迁移 | Flyway | 10.10.0 |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis | 7 |
| 前端语言 | TypeScript | 5.x |
| 前端框架 | React | 18.x |
| 前端构建 | Vite | 5.x |
| 前端路由 | React Router | 6.x |
| 前端状态 | Zustand | 4.x |
| 前端请求 | axios | 1.x |
| 代码风格 | Spotless + ESLint + Prettier | - |
| 容器化 | Docker + docker-compose | - |

## 构建与运行

### 前置条件

- Java 17 LTS
- Maven 3.9+
- Node.js 22+
- PostgreSQL 16+（运行时）

### 后端构建

```bash
cd backend
mvn clean package -DskipTests
java -jar boot/target/cloudlab-copilot-boot-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

健康检查：`GET http://localhost:8080/actuator/health`

### 前端构建

```bash
cd frontend
npm install
npm run build
```

开发模式：`npm run dev`（端口 5173，自动代理 `/api` 到后端 8080）

### Docker 一键启动

```bash
cp .env.example .env  # 填入真实配置
docker-compose up -d
```

### 一键验证

```bash
bash scripts/verify-startup.sh --profile dev
```

## 环境变量

参见 `.env.example`，主要配置项：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring Profile | dev |
| `SERVER_PORT` | 后端端口 | 8080 |
| `DB_URL` | 数据库连接串 | 占位符 |
| `DB_USERNAME` | 数据库用户 | 占位符 |
| `DB_PASSWORD` | 数据库密码 | 占位符 |
| `FLYWAY_ENABLED` | 是否启用迁移 | true |
| `REDIS_HOST` | Redis 主机 | 占位符 |
| `VITE_API_BASE_URL` | 前端 API 基址 | 占位符 |

## 开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| 0 | 仓库检查（只读） | ✅ 完成 |
| 1 | 项目骨架 | ✅ 完成 |
| 2 | 实验和状态 | ⏳ 待开发 |
| 3 | 结构化知识 | ⏳ 待开发 |
| 4 | RAG 检索 | ⏳ 待开发 |
| 5 | MaaS 诊断 | ⏳ 待开发 |
| 6 | 安全和报告 | ⏳ 待开发 |
| 7 | RDS/Redis/Skills 部署 | ⏳ 待开发 |
| 8 | 评测和发布 | ⏳ 待开发 |

## 安全约束

- `.env.example` 仅含占位符，禁止填入真实凭据
- `.gitignore` 排除 `.env`、`node_modules`、`target` 等敏感/构建文件
- Actuator 仅暴露 `health` 端点，生产环境隐藏详情
- 全局异常处理不向客户端暴露完整堆栈