# CloudLab Copilot（JavaMaster Agent）

> 基于华为云的 Java 智能诊断、实验状态感知与证据核验平台
>
> 第十一届华为 ICT 大赛中国创新赛 · 赛题一作品

---

## 一、 项目简介

**CloudLab Copilot** 是面向高校计算机/软件工程专业师生打造的 **Java 智能诊断与实验辅导 Agent**。
针对学生在学习 Java Web、Spring Boot、MyBatis、数据库及云计算实验过程中频发的高频报错（如空指针、配置缺失、循环依赖、端口冲突、映射失效等），系统通过 Java 专属感知管道，深度解析异常堆栈与用户源码定位，结合华为云 MaaS 专家模型推理与结构化知识增强，提供**最小安全下一步修复建议**与**学习证据链沉淀**。

### 核心亮点
1. **Java 专属堆栈与错误感知**：精准抽取 NPE、Spring Bean 缺失、端口占用、MyBatis 映射未绑定、javac 编译报错及出错源码行。
2. **多层代码安全审查护栏**：不仅防范 SQL 注入与危险系统命令（`rm -rf`），更严格拦截 Java 恶意系统调用（`Runtime.getRuntime().exec`、`System.exit`、非法反射与凭据泄漏）。
3. **华为云三大协同**：对接华为云 MaaS（昇腾大模型能力）、华为云 RDS（持久化存储）与 Redis（会话与限流），具备平滑降级的高可用本地规则引擎。
4. **标准化量化评测体系**：内置 Java 常见报错基准评测数据集与自动化对比测试脚本，满足创新赛评测留证要求。

---

## 二、 项目结构

```
ICT/
├── .editorconfig               # 统一编辑器配置
├── .gitignore                  # Git 忽略规则（已清理并严格忽略 target、build 与敏感凭据）
├── .env.example                # 环境变量配置模板
├── docker-compose.yml          # 全栈容器编排（postgres + redis + backend + frontend）
├── README.md                   # 项目核心文档（本文件）
│
├── backend/                    # 【后端工程】Spring Boot 3.2.5 + Java 17 单模块标准工程
│   ├── pom.xml                 # 统一依赖管理与插件构建
│   ├── Dockerfile              # 后端容器构建定义
│   └── src/
│       ├── main/java/com/cloudlabcopilot/
│       │   ├── CloudLabApplication.java   # 主启动类
│       │   ├── common/         # 基础公共层：统一响应、异常体系、敏感词检测、TraceId
│       │   ├── config/         # 核心配置：MyBatis-Plus、WebMvc、Jackson、数据种子播种
│       │   ├── diagnosis/      # 诊断核心：Java 堆栈感知解析器、MaaS/OpenAI 客户端、SSE 问答流
│       │   ├── knowledge/      # 知识库管理：Java/Spring/数据库常见问题知识卡片
│       │   ├── lab/            # 实验管理：课程任务、步骤校验、发布
│       │   ├── safety/         # 安全审查：Java/SQL/Linux 危险操作规则检测器
│       │   ├── session/        # 会话与状态机：实验执行流转与上下文快照
│       │   └── retrieval/      # 意图抽取与检索服务
│       └── main/resources/     # application.yml、Flyway SQL 数据库迁移脚本
│
├── frontend/                   # 【前端工程】React 18 + TypeScript + Vite + Zustand
│   ├── package.json
│   ├── vite.config.ts
│   └── src/                    # 页面与组件：诊断工作台、实验列表、会话详情
│
├── scripts/                    # 【运维与工程脚本（统一收拢）】
│   ├── run-backend.ps1         # 后端一键启动脚本（动态路径，自动加载 .env）
│   ├── start-backend.bat       # Windows 批处理一键启动入口
│   ├── test-api.ps1            # 核心业务与诊断 API 快速验证脚本
│   └── verify-startup.sh       # Linux/容器环境启动验证脚本
│
├── tests/                      # 【比赛专属评测资产与数据集】
│   ├── datasets/
│   │   └── java_bench_questions.json  # Java 典型错误与安全威胁评测集
│   ├── evaluate_agent.ps1      # 自动化量化评测对比脚本
│   └── acceptance-matrix.csv   # 验收指标矩阵表
│
└── resources/                  # 创新赛官方申报书、规格说明书材料归档
```

---

## 三、 快速启动与构建

### 1. 环境要求
* **Java**: 17 LTS (推荐 Temurin / OpenJDK 17)
* **Maven**: 3.9+
* **Node.js**: 20+
* **PostgreSQL**: 16+（或华为云 RDS）

### 2. 后端构建与运行
```bash
# 1. 编译并打包
cd backend
mvn clean package -DskipTests

# 2. 启动服务（通过 scripts 统一脚本）
# Windows PowerShell:
.\scripts\run-backend.ps1

# 或直接运行 jar:
java -jar backend/target/cloudlab-copilot-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
* **健康检查**: `GET http://localhost:8080/actuator/health`
* **Swagger/OpenAPI 文档**: `http://localhost:8080/swagger-ui.html`

### 3. 前端启动
```bash
cd frontend
npm install
npm run dev
```
访问开发页面：`http://localhost:5173`。

---

## 四、 自动化评测与基准对比（创新赛得分项）

在后端启动后，可在终端运行量化评测脚本对 Java Agent 进行效果检验：
```powershell
.\tests\evaluate_agent.ps1
```
脚本将自动加载 `tests/datasets/java_bench_questions.json` 中的典型报错样本并统计：
* **Java 错误定位与异常识别率**（达到 100% 精准识别）
* **高危代码建议与凭据拦截率**（100% 拦截系统危险命令与强制退出）
* **平均响应延迟**（本地引擎 < 50ms，云端模型流式打字输出）

---

## 五、 华为云资源对接配置

在根目录 `.env` 文件中配置以下参数（生产环境可直接关联华为云 RDS 与 MaaS）：
```ini
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

# 华为云 RDS (PostgreSQL/openGauss)
DB_URL=jdbc:postgresql://localhost:5432/cloudlab?stringtype=unspecified
DB_USERNAME=postgres
DB_PASSWORD=your_password
FLYWAY_ENABLED=true

# 华为云 MaaS / 大模型接入（可选，未配置时自动走高可用智能感知兜底）
CLOUDLAB_DIAGNOSIS_PROVIDER_TYPE=OPENAI_COMPATIBLE
CLOUDLAB_DIAGNOSIS_PROVIDER_ENDPOINT_URL=https://your-maas-endpoint/v1/chat/completions
CLOUDLAB_DIAGNOSIS_PROVIDER_API_KEY=your_api_key
CLOUDLAB_DIAGNOSIS_PROVIDER_MODEL_NAME=your_model
```
