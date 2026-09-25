#!/bin/bash
# 微服务电商智能导购平台 - 一键启动脚本 (Bash版)
# 用法: 在项目根目录执行 bash start.sh
#
# 可选环境变量：
#   JAVA_HOME          JDK 安装目录，未设置时使用 PATH 中的 java
#   DEEPSEEK_API_KEY   DeepSeek API Key，AI 导购对话功能必需（未设置时该功能不可用）
#   DB_PASSWORD        覆盖默认数据库密码（默认 123456）

set -e

# 项目根目录 = 本脚本所在目录，克隆到任意路径都可用，无需手动修改
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# JAVA_HOME 未设置或无效时回退到 PATH 中的 java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi

echo "============================================"
echo "  微服务电商智能导购平台 - 一键启动"
echo "  项目目录: $PROJECT_DIR"
echo "============================================"
echo ""

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

check_port() {
  curl -s -o /dev/null -w "%{http_code}" --max-time 1 "http://localhost:$1/" 2>/dev/null
}

ok()   { echo -e "  ${GREEN}✅${NC} $1"; }
waitf() { echo -e "  ${YELLOW}⏳${NC} $1"; }
warn() { echo -e "  ${RED}⚠️${NC}  $1"; }

# 检查 AI 功能所需的 Key
if [ -z "$DEEPSEEK_API_KEY" ]; then
  warn "未设置 DEEPSEEK_API_KEY，AI 导购对话将不可用（其余功能不受影响）"
fi

# ===== 1. 启动 Nacos =====
echo "[1/4] 检查 Nacos..."
if curl -s -o /dev/null --max-time 2 http://localhost:8848/nacos/ 2>/dev/null; then
  ok "Nacos 已在运行"
elif [ -x "$PROJECT_DIR/work/nacos/bin/startup.sh" ]; then
  waitf "启动 Nacos..."
  nohup "$PROJECT_DIR/work/nacos/bin/startup.sh" -m standalone > /dev/null 2>&1 &
  sleep 15
  ok "Nacos 已启动"
else
  warn "未找到 Nacos（预期路径: work/nacos/）"
  echo "      Nacos 未随仓库提供，请自行下载并解压到 work/nacos/："
  echo "      https://github.com/alibaba/nacos/releases  (推荐 2.3.x)"
  echo "      或先手动启动 Nacos 后重新执行本脚本。"
  exit 1
fi

# ===== 2. 检查基础服务 =====
echo "[2/4] 检查 MySQL & Redis..."
redis-cli ping 2>/dev/null | grep -q PONG && ok "Redis 已在运行" || waitf "Redis 未运行，请检查"
# MySQL check
mysql -u root -p"${DB_PASSWORD:-123456}" -e "SELECT 1" 2>/dev/null && ok "MySQL 已在运行" || waitf "MySQL 未运行，请检查"

# ===== 3. 启动后端微服务 =====
echo "[3/4] 启动后端微服务..."
# 日志输出目录
mkdir -p "$PROJECT_DIR/logs/backend"
cd "$PROJECT_DIR/springcloud-eshop"

SERVICES=(
  "eshop-gateway:8086"
  "eshop-user-service:8081"
  "eshop-product-service:8082"
  "eshop-cart-service:8083"
  "eshop-order-service:8084"
  "eshop-ai-guide-service:8085"
)

for svc in "${SERVICES[@]}"; do
  name="${svc%%:*}"
  port="${svc##*:}"

  code=$(check_port $port)
  if [ "$code" != "000" ]; then
    ok "$name (:$port) 已在运行"
  else
    waitf "启动 $name (:$port)..."
    # gateway 的胖 JAR 名称特殊
    if [ "$name" = "eshop-gateway" ]; then
      jar_file="$name/target/eshop-gateway-app.jar"
    else
      jar_file="$name/target/$name-1.0.0.jar"
    fi
    if [ ! -f "$jar_file" ]; then
      warn "$name 的构建产物不存在，请先执行: mvn clean package -DskipTests"
      continue
    fi
    nohup $JAVA -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar "$jar_file" \
      --server.port=$port \
      --spring.cloud.nacos.discovery.ip=127.0.0.1 > "$PROJECT_DIR/logs/backend/$name.log" 2>&1 &
    sleep 8
    ok "$name (:$port) 已启动"
  fi
done

# ===== 4. 启动前端 =====
echo "[4/4] 启动前端..."
cd "$PROJECT_DIR/frontend"

if [ ! -d node_modules ]; then
  waitf "首次运行，安装前端依赖..."
  npm install
fi

# 检查 3000 是否被占用
if curl -s -o /dev/null --max-time 1 http://localhost:3000/ 2>/dev/null; then
  FRONTEND_PORT=3001
  echo "  ⚠️  端口 3000 被占用，使用 3001"
else
  FRONTEND_PORT=3000
fi

NODE_OPTIONS="--max-old-space-size=4096" nohup npm run dev -- --port $FRONTEND_PORT > "$PROJECT_DIR/logs/backend/frontend.log" 2>&1 &
ok "前端已启动 (http://localhost:$FRONTEND_PORT)"

echo ""
echo "============================================"
echo -e "  ${GREEN}启动完成！${NC}"
echo ""
echo "  前端地址：    http://localhost:$FRONTEND_PORT"
echo "  后端网关：    http://localhost:8086"
echo "  Nacos 控制台：http://localhost:8848/nacos"
echo "  服务日志：    logs/backend/"
echo "============================================"
