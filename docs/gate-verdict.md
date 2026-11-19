# 阶段2 门禁裁决报告

> 生成时间：2026-11-18
> 阶段：阶段2「实验和状态」

## 门禁 A（开始测试前置）

| 检查项 | 结论 | 证据 |
|--------|------|------|
| Java 17 | ✅ PASS | 17.0.12 LTS |
| Maven 3.9.6 | ✅ PASS | D:/Meaven/apache-maven-3.9.6 |
| Node 22 | ✅ PASS | v22.16.0 |
| PostgreSQL 16 | ✅ PASS | 16.16，5432 LISTENING，scram-sha-256 |
| 后端构建 | ✅ PASS | `mvn package -DskipTests` BUILD SUCCESS（11 模块） |
| 后端启动 | ✅ PASS | `Started CloudLabApplication in 6.253 seconds` |
| 健康检查 | ✅ PASS | `GET /actuator/health` → 200 OK |
| Flyway 基线 | ✅ PASS | V1~V4 已应用，`now at version v4`（11 张表） |
| 前端构建 | ⚠️ PARTIAL | `esbuild.exe` 缺系统 DLL（VC++ 2015-2022 x64 未安装） |

**门禁 A 结论：PASS（前端 PARTIAL，已如实标注）**

## 门禁 B（测试门禁）

| 检查项 | 结论 | 证据 |
|--------|------|------|
| 单元测试 UT | ✅ PASS | 14 个通过（lab 9 + session 5），Failures/Errors 均为 0 |
| 集成测试 IT | ⚠️ PENDING | 代码就绪且 `mvn test-compile` BUILD SUCCESS；沙箱内 Maven 测试运行超时、surefire 报告未生成 |
| 契约测试 CT | ⚠️ PENDING | 未单独实现（可用 springdoc-openapi 生成 OpenAPI 后补契约校验） |
| 端到端 E2E | ⚠️ PENDING | 沙箱无法 HTTP 访问 localhost:8080；已用 JVM 内部方式验证 9 步闭环 |
| 覆盖率门禁 | ⚠️ PENDING | jacoco 已配置（LINE 80% / BRANCH 70%），未运行完整 `mvn verify` 报告 |

**门禁 B 结论：PARTIAL（UT 通过；IT/CT/E2E/覆盖率待用户本机补齐）**

## UT 明细

| 测试类 | 模块 | 用例数 | 结果 |
|--------|------|--------|------|
| LabCodeAndTagValidatorTest | lab | 5 | ✅ PASS |
| StepValidatorTest | lab | 4 | ✅ PASS |
| SessionStateMachineTest | session | 5 | ✅ PASS |
| **合计** | — | **14** | **✅ 全部通过** |

执行命令：
```bash
cd backend
mvn test -B
# Tests run: 9 (lab) + 5 (session), BUILD SUCCESS
```

## IT 说明

`LabSessionIntegrationTest`（`src/test/java/com/cloudlabcopilot/`）已编写并编译通过（`mvn test-compile` BUILD SUCCESS），覆盖：
- 全流程：创建实验 → 保存步骤 → 保存前置条件 → 发布 → 目录 → 详情 → 开始会话 → 推进 → 上下文
- 重复实验编码返回业务码 `409` + `LAB_CODE_CONFLICT`

用户本机运行方式：
```bash
cd backend
export TEST_DB_PASSWORD=aric2006
mvn test -Dtest=LabSessionIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```
沙箱内因 Maven 测试进程超时/日志不落地，未取得最终 surefire 报告，故如实记为 **PENDING**（不伪造通过）。

## E2E 说明

沙箱内 `curl`/PowerShell 无法访问 localhost:8080（沙箱网络限制）。已通过 `build/gate-a/verify-api.ps1` 在 JVM/进程内验证 9 步闭环，全部 `code=200`：
```
1_CREATE=OK 2_STEPS=OK 3_PRE=OK 4_PUBLISH=OK 5_CATALOG=OK(total=1)
6_DETAIL=OK(labId=1 versionNo=1 steps=2) 7_START=OK(status=IN_PROGRESS)
8_ADVANCE=OK(step=2) 9_CONTEXT=OK(currentStep=2 completed=1)
```

## 证据文件

- `build/gate-a/env-snapshot.txt`（工具链快照）
- `build/gate-a/startup-baseline.txt`（启动基线）
- `build/gate-a/api-results.txt`（9 步 API 闭环结果）
- `backend/target/surefire-reports/`（UT/IT 报告，单模块统一产物目录）

## 未完成项与原因

| 项 | 原因 | 补齐方式 |
|----|------|---------|
| 前端构建 | esbuild.exe 缺系统 DLL | 安装 VC++ 2015-2022 x64 Redistributable |
| IT 运行报告 | 沙箱 Maven 测试超时 | 用户本机运行（命令见上） |
| CT | 未单独实现 | 用 springdoc-openapi 生成契约后校验 |
| E2E | 沙箱无法 HTTP 访问 8080 | 用户本机浏览器/Postman |
| 覆盖率 | 未跑完整 verify | `mvn verify`（jacoco 已配置） |

## 总体结论

阶段2 核心业务（33 个端点 + dev Seeder + 前端页面 + 14 个 UT）已实现并跑通端到端业务闭环；**门禁 A PASS（前端 PARTIAL）、门禁 B PARTIAL**。剩余 IT/CT/E2E/覆盖率需在用户本机（具备完整网络与前端构建环境）补齐，本报告不伪造任何未验证项。