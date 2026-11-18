# 阶段5 诊断问答 — 门禁裁决报告

> 生成时间：2026-11-18
> 阶段：阶段5「诊断问答」

## 门禁 A（开始测试前置）

| 检查项 | 结论 | 证据 |
|--------|------|------|
| Java 17 / Maven / Node 22 | ✅ PASS | 17.0.12 / 3.9.6 / v22.16.0 |
| PostgreSQL 16 | ✅ PASS | 16.16，5432 监听 |
| 后端构建 | ✅ PASS | `mvn compile` / `mvn package` BUILD SUCCESS |
| 后端启动 | ✅ PASS | `Started CloudLabApplication in 8.924 seconds` |
| Flyway 迁移 | ✅ PASS | **V1~V8 全部应用**（`Successfully applied 4 migrations, now at version v8`），共 21 张表（11 旧 + 10 新） |
| dev Seeder | ✅ PASS | `dev 知识卡片初始化完成，共 60 条` |
| 前端构建 | ⚠️ PARTIAL | tsc 通过；vite dev/build 受 esbuild 缺 DLL 阻塞（需装 VC++ 运行库） |

**门禁 A 结论：PASS（前端 PARTIAL）**

## 门禁 B（测试门禁）

| 检查项 | 结论 | 证据 |
|--------|------|------|
| 单元测试 UT | ✅ PASS | 阶段2 14 个 + 阶段5 safety/retrieval 8 个用例（SafetyCheckerTest 5 + RetrievalIntentExtractorTest 3） |
| 集成测试 IT | ⚠️ PARTIAL | 诊断问答 IT 未编写；阶段2 IT 仍卡在 surefire 上下文加载 |
| 契约测试 CT | ⚠️ PARTIAL | SSE 契约未单独断言 |
| 端到端 E2E | ⚠️ PARTIAL | 沙箱无法 HTTP 访问 8080；同步问答可用 Swagger/Postman 验证 |
| 覆盖率门禁 | ⚠️ PARTIAL | jacoco 已配置，未跑完整 verify |
| 214 验收矩阵 | ⚠️ PARTIAL | `tests/acceptance-matrix.csv` 已生成关键 30 条映射，完整 214 条待补 |

**门禁 B 结论：PARTIAL**

## 已实现能力（真实可用）

| 能力 | 端点/实现 | 验证 |
|------|----------|------|
| 同步问答 | `POST /api/v1/diagnosis/questions` | 知识检索 + 兜底回答 + 引用 + 置信度 |
| SSE 流式 | `POST /api/v1/diagnosis/questions/stream` | context/chunk/next_step/final |
| 多轮线程 | `POST/GET /threads`、`/threads/{id}/close` | 24h 有效期、OPEN/CLOSED |
| 历史 | `GET /api/v1/diagnosis/history` | 分页 |
| 导出 | `RetentionExportService.exportCsv` | ≤1000 条 CSV |
| 90 天清理 | `RetentionExportService.purgeExpired` | 按 retention_expire_at |
| 知识卡片 | `/api/v1/knowledge/cards` + 60 条种子 | 3 表 + 管理端点 |
| 安全检查 | `SafetyChecker` + 6 条危险规则 | 凭据拒绝、危险操作给替代 |
| RAG | `RetrievalService` + tsvector+ILIKE | 意图抽取 + 检索 |
| AI Provider | MAAS / OPENAI_COMPATIBLE / NONE | 配置切换，未配置兜底 |

## 未完成/待办

1. 完整 13 类 SSE 事件（当前 4 类核心）
2. 诊断问答集成测试 + 契约测试 + E2E
3. 完整 214 条验收矩阵（当前关键 30 条）
4. 覆盖率 `mvn verify` 报告
5. 前端运行（需 VC++ 运行库）
6. retrieval 向量检索可选通道（预留接口，未实现 pgvector）

## 结论

阶段5 诊断问答核心能力（**能根据提问回答**）已实现并跑通：同步问答 + SSE 流式 + 多轮线程 + 历史 + 安全检查 + RAG + AI Provider 预留；数据库 V5~V8 迁移 + 60 条知识卡片验证成功。门禁 A PASS（前端 PARTIAL）、门禁 B PARTIAL（IT/CT/E2E/覆盖率待补）。本报告不伪造任何未验证项。