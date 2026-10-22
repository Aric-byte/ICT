#!/usr/bin/env bash
set -euo pipefail

PROFILE="dev"
TIMEOUT=30
SKIP_FRONTEND=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_PID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile) PROFILE="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --skip-frontend) SKIP_FRONTEND=true; shift ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

log() { echo "[verify-startup] $*"; }
fail() { echo "[verify-startup] 失败: $*" >&2; cleanup; exit 1; }
cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    log "关闭后端进程 PID=$BACKEND_PID"
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

log "===== 步骤 0: 前置检查 ====="
java -version 2>&1 | head -1 || fail "Java 未安装，请安装 JDK 17"
JAVA_VER=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')
[[ "$JAVA_VER" == 17* ]] || fail "需要 Java 17，当前: $JAVA_VER"

mvn -version 2>&1 | head -1 || fail "Maven 未安装"
log "Java: $JAVA_VER, Maven: $(mvn -version 2>&1 | head -1)"

if [[ "$SKIP_FRONTEND" == false ]]; then
  node --version 2>&1 || fail "Node 未安装，请安装 Node 22"
  NODE_VER=$(node --version)
  [[ "$NODE_VER" == v22* ]] || fail "需要 Node 22，当前: $NODE_VER"
  log "Node: $NODE_VER"
fi

log "===== 步骤 1: 后端构建 ====="
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests -B -q || fail "后端构建失败"
JAR_PATH=$(ls boot/target/*.jar 2>/dev/null | head -1)
[[ -n "$JAR_PATH" ]] || fail "未找到后端 JAR 产物"
log "后端构建成功: $JAR_PATH"

if [[ "$SKIP_FRONTEND" == false ]]; then
  log "===== 步骤 2: 前端构建 ====="
  cd "$PROJECT_ROOT/frontend"
  npm ci --silent 2>/dev/null || npm install --silent || fail "前端依赖安装失败"
  npm run build --silent || fail "前端构建失败"
  log "前端构建成功: dist/ 已生成"
fi

log "===== 步骤 3: 启动后端 (profile=$PROFILE) ====="
cd "$PROJECT_ROOT/backend"
java -jar "$JAR_PATH" --spring.profiles.active="$PROFILE" &
BACKEND_PID=$!
log "后端已启动 PID=$BACKEND_PID"

log "===== 步骤 4: 健康检查轮询 (超时 ${TIMEOUT}s) ====="
HEALTH_URL="http://localhost:8080/actuator/health"
ELAPSED=0
INTERVAL=2
while [[ $ELAPSED -lt $TIMEOUT ]]; do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    fail "后端进程已退出"
  fi
  RESPONSE=$(curl -sf "$HEALTH_URL" 2>/dev/null || echo "")
  if [[ -n "$RESPONSE" ]]; then
    STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [[ "$STATUS" == "UP" ]]; then
      log "===== 验证通过 ====="
      log "健康检查响应: $RESPONSE"
      log "耗时: ${ELAPSED}s"
      exit 0
    fi
  fi
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
  log "等待中... ${ELAPSED}s / ${TIMEOUT}s"
done

fail "健康检查在 ${TIMEOUT}s 内未通过"