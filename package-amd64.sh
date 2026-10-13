#!/usr/bin/env bash
# ==============================================================================
# Volur Lite 一键交付打包脚本 (针对 Linux amd64 平台)
# 支持在 macOS (Apple Silicon ARM64) 或 Linux x86_64 上直接交叉编译与导出
# 生成一键解包即可运行的 volur-lite-amd64.tar.gz 交付压缩包
# ==============================================================================

set -euo pipefail

# 颜色输出定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

info() { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# 脚本所在目录 (项目根目录)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# 默认参数
MODE="slim"                     # [默认] slim: 业务精简包 (仅自有服务 volur-app + volur-nginx); full: 全量离线包 (含 mysql + redis)
SKIP_BUILD=false                # 是否跳过源码构建直接打包现有镜像
OUTPUT_FILE="volur-lite-amd64.tar.gz"
TARGET_PLATFORM="linux/amd64"

# 参数解析
show_help() {
    cat <<EOF
Volur Lite Linux amd64 一键交付打包脚本

用法:
  ./package-amd64.sh [选项]

选项:
  --slim            [默认] 仅打包 volur-app 与 volur-nginx 核心业务镜像 (~130MB)，目标机器拉起时自动获取 MySQL/Redis/Registry
  --full            全量离线交付包，包含全套 5 个 amd64 镜像 (volur-app, volur-nginx, mysql, redis, registry)
  --skip-build      跳过前后端源码镜像构建，直接导出本地已存在的 amd64 镜像
  -o, --output <名> 指定输出的 tar.gz 文件名 (默认: volur-lite-amd64.tar.gz)
  -h, --help        显示帮助信息

示例:
  ./package-amd64.sh                    # 默认仅打包 volur-app 与 volur-nginx 业务镜像
  ./package-amd64.sh --skip-build       # 跳过构建，直接导出当前本地镜像
  ./package-amd64.sh --full             # 打包全量含数据库的离线包
  ./package-amd64.sh -o my-release.tar.gz
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --full)
            MODE="full"
            shift
            ;;
        --slim)
            MODE="slim"
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

echo "=================================================================="
echo -e "${CYAN}             Volur Lite Linux amd64 一键交付打包工具              ${NC}"
echo "=================================================================="
info "项目根目录:   $PROJECT_DIR"
info "目标架构平台: $TARGET_PLATFORM"
info "打包模式:     $MODE $([ "$MODE" == "full" ] && echo "(全量离线包，含全部 4 个镜像)" || echo "(轻量业务包)")"
info "输出目标文件: $OUTPUT_FILE"
info "是否跳过构建: $SKIP_BUILD"
echo "------------------------------------------------------------------"

# 1. 环境依赖核验
if ! command -v docker &>/dev/null; then
    error "未找到 docker 命令，请确保已安装 Docker 并加入环境变量 PATH"
    exit 1
fi

if ! docker info &>/dev/null; then
    error "Docker 守护进程未启动或无法连通，请先启动 Docker Desktop 或 Docker 服务"
    exit 1
fi

# 检查压缩工具
COMPRESS_CMD="gzip"
if command -v pigz &>/dev/null; then
    COMPRESS_CMD="pigz"
    info "检测到多线程压缩工具 pigz，将启用多核极速压缩"
fi

# 临时工作目录
STAGING_DIR="$(mktemp -d -t volur-pkg-XXXXXX)"
PKG_ROOT="$STAGING_DIR/volur-lite"
mkdir -p "$PKG_ROOT"

cleanup() {
    rm -rf "$STAGING_DIR"
}
trap cleanup EXIT

# 2. 编译 / 准备 amd64 镜像
if [ "$SKIP_BUILD" = false ]; then
    info "步骤 1/4: 正在并行交叉编译后端与前端镜像 [$TARGET_PLATFORM]..."
    docker build \
        --platform "$TARGET_PLATFORM" \
        -t volur-app:latest \
        -f "$PROJECT_DIR/src/volur-lite-platform/Dockerfile" \
        "$PROJECT_DIR/src/volur-lite-platform" &
    PID_APP=$!

    docker build \
        --platform "$TARGET_PLATFORM" \
        -t volur-nginx:latest \
        -f "$PROJECT_DIR/src/volur-lite-view-NEXT/Dockerfile" \
        "$PROJECT_DIR/src/volur-lite-view-NEXT" &
    PID_NGINX=$!

    wait "$PID_APP" || { error "后端镜像编译失败"; exit 1; }
    wait "$PID_NGINX" || { error "前端镜像编译失败"; exit 1; }
    success "前后端镜像交叉编译完成！"
else
    warn "已指定 --skip-build，跳过源码镜像构建阶段"
fi

# 3. 准备公共基础设施镜像 (Full 模式)
IMAGES_TO_SAVE=("volur-app:latest" "volur-nginx:latest")

if [ "$MODE" == "full" ]; then
    info "步骤 2/4: 正在拉取 / 核验基础设施镜像 (amd64 架构)..."
    info "拉取 mysql:9.6.0 ($TARGET_PLATFORM)..."
    docker pull --platform "$TARGET_PLATFORM" mysql:9.6.0
    info "拉取 redis:8.6.2-alpine ($TARGET_PLATFORM)..."
    docker pull --platform "$TARGET_PLATFORM" redis:8.6.2-alpine
    info "拉取 registry:2 ($TARGET_PLATFORM)..."
    docker pull --platform "$TARGET_PLATFORM" registry:2
    IMAGES_TO_SAVE+=("mysql:9.6.0" "redis:8.6.2-alpine" "registry:2")
else
    info "步骤 2/4: Slim 模式下不打包基础组件，目标机拉起时将由 Docker 自动按需获取"
fi

# 4. 导出 Docker 镜像
info "步骤 3/4: 正在导出 Docker 镜像为 volur-images.tar..."
info "打包镜像清单: ${IMAGES_TO_SAVE[*]}"

docker save "${IMAGES_TO_SAVE[@]}" -o "$PKG_ROOT/volur-images.tar"
IMAGE_TAR_SIZE=$(du -h "$PKG_ROOT/volur-images.tar" | cut -f1)
success "Docker 镜像包导出完成，未压缩体积: $IMAGE_TAR_SIZE"

# 5. 生成生产纯净版部署套件
info "步骤 4/4: 正在装配独立部署运行套件..."

# 5.1 生产级 docker-compose.yml (移除 build: 代码目录依赖，直接挂载镜像与卷)
cat <<'EOF' > "$PKG_ROOT/docker-compose.yml"
services:
  volur-mysql:
    image: mysql:9.6.0
    container_name: volur-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}
      MYSQL_DATABASE: volur_db
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00
    volumes:
      - volur-mysql-data:/var/lib/mysql
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}" ]
      interval: 5s
      timeout: 5s
      retries: 10
    networks:
      - volur-network
    restart: always

  volur-redis:
    image: redis:8.6.2-alpine
    container_name: volur-redis
    command: [ "redis-server", "--requirepass", "${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}", "--appendonly", "yes" ]
    healthcheck:
      test: [ "CMD", "redis-cli", "-a", "${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}", "ping" ]
      interval: 5s
      timeout: 3s
      retries: 5
    volumes:
      - volur-redis-data:/data
    networks:
      - volur-network
    restart: always

  volur-registry:
    image: registry:2
    container_name: volur-registry
    environment:
      REGISTRY_STORAGE_DELETE_ENABLED: "true"
    ports:
      - "${REGISTRY_PORT:-5000}:5000"
    volumes:
      - ./data/registry:/var/lib/registry
    networks:
      - volur-network
    restart: always

  volur-app:
    image: volur-app:latest
    container_name: volur-app
    depends_on:
      volur-mysql:
        condition: service_healthy
      volur-redis:
        condition: service_healthy
      volur-registry:
        condition: service_started
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
      - SERVER_PORT=8080
      - VOLUR_MYSQL_HOST=volur-mysql
      - VOLUR_MYSQL_USER=${VOLUR_MYSQL_USER:-root}
      - VOLUR_MYSQL_PASSWORD=${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}
      - VOLUR_REDIS_HOST=volur-redis
      - VOLUR_REDIS_PASSWORD=${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}
      - VOLUR_JWT_SECRET=${VOLUR_JWT_SECRET:-VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!}
      - VOLUR_CONTAINER_DOCKER_HOST=unix:///var/run/docker.sock
      - VOLUR_CONTAINER_REGISTRY_INTERNAL_URL=http://volur-registry:5000
      - VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT=${REGISTRY_PORT:-5000}
      - VOLUR_CONTAINER_REGISTRY_ENABLED=true
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./uploads:/app/uploads
      - /etc/rancher/k3s/k3s.yaml:/etc/rancher/k3s/k3s.yaml:ro
    networks:
      - volur-network
    restart: always

  volur-nginx:
    image: volur-nginx:latest
    container_name: volur-nginx
    depends_on:
      - volur-app
    ports:
      - "${NGINX_HTTP_PORT:-80}:80"
    networks:
      - volur-network
    restart: always

networks:
  volur-network:
    driver: bridge

volumes:
  volur-mysql-data:
  volur-redis-data:
EOF

# 5.2 生产 .env 模板文件
if [ -f "$PROJECT_DIR/.env" ]; then
    cp "$PROJECT_DIR/.env" "$PKG_ROOT/.env.example"
elif [ -f "$PROJECT_DIR/.env.example" ]; then
    cp "$PROJECT_DIR/.env.example" "$PKG_ROOT/.env.example"
else
    cat <<'EOF' > "$PKG_ROOT/.env.example"
NGINX_HTTP_PORT=80
REGISTRY_PORT=5000
VOLUR_MYSQL_USER=root
VOLUR_MYSQL_PASSWORD=volur_mysql_secret_2026
VOLUR_REDIS_PASSWORD=volur_redis_secret_2026
VOLUR_JWT_SECRET=VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!
EOF
fi
cp "$PKG_ROOT/.env.example" "$PKG_ROOT/.env"

# 5.3 数据库升级与增量迁移脚本
if [ -f "$PROJECT_DIR/scripts/upgrade/upgrade_to_latest.sql" ]; then
    cp "$PROJECT_DIR/scripts/upgrade/upgrade_to_latest.sql" "$PKG_ROOT/upgrade_to_latest.sql"
fi

# 5.4 数据持久化目录与权限占位
mkdir -p "$PKG_ROOT/uploads" "$PKG_ROOT/data/registry"

# 5.4 一键启动脚本 start.sh
cat <<'EOF' > "$PKG_ROOT/start.sh"
#!/usr/bin/env bash
# ==============================================================================
# Volur Lite 一键启动脚本
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "              Volur Lite 一键部署拉起                     "
echo "=========================================================="

# 1. 检查 Docker 环境
if ! command -v docker &>/dev/null; then
    echo "[错误] 未检测到 docker，请先安装 Docker (或 Docker Engine)" >&2
    exit 1
fi

if ! docker info &>/dev/null; then
    echo "[错误] Docker 服务未运行，请先启动 Docker 服务 (sudo systemctl start docker)" >&2
    exit 1
fi

# 2. 导入镜像离线包 (若存在)
if [ -f "volur-images.tar" ]; then
    echo "==> 正在导入离线 Docker 镜像包 (volur-images.tar)..."
    docker load -i volur-images.tar
    echo "==> 镜像导入完成！"
elif [ -f "volur-images.tar.gz" ]; then
    echo "==> 正在导入离线 Docker 镜像包 (volur-images.tar.gz)..."
    gzip -dc volur-images.tar.gz | docker load
    echo "==> 镜像导入完成！"
fi

# 3. 配置文件检查
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "==> 自动从 .env.example 生成 .env 配置文件..."
    cp .env.example .env
fi

# 4. 创建必要目录
mkdir -p uploads data/registry

# 5. 针对 K3s 挂载文件的友好处理 (若无 K3s 挂载文件，避免 Docker 自动将其创建为目录)
if [ ! -e "/etc/rancher/k3s/k3s.yaml" ]; then
    echo "==> 提示: 宿主机当前未发现 /etc/rancher/k3s/k3s.yaml"
    echo "==> 若您计划使用 K3s 模式，请在安装 K3s 后挂载；若使用 Docker Swarm 模式则无需理会"
    sudo mkdir -p /etc/rancher/k3s 2>/dev/null || true
    sudo touch /etc/rancher/k3s/k3s.yaml 2>/dev/null || true
fi

# 6. 拉起服务
echo "==> 正在启动 Volur Lite 容器集群..."
docker compose up -d

echo ""
echo "=========================================================="
echo "   🎉 Volur Lite 平台服务启动成功！"
echo "   服务访问端口: ${NGINX_HTTP_PORT:-80}"
echo "   默认访问地址: http://<您的服务器IP>:${NGINX_HTTP_PORT:-80}"
echo "   停止服务命令: ./stop.sh"
echo "=========================================================="
EOF
chmod +x "$PKG_ROOT/start.sh"

# 5.5 一键停止脚本 stop.sh
cat <<'EOF' > "$PKG_ROOT/stop.sh"
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> 正在停止 Volur Lite 服务容器..."
docker compose down
echo "==> Volur Lite 服务已成功停止。"
EOF
chmod +x "$PKG_ROOT/stop.sh"

# 5.6 部署使用说明 README.md
cat <<'EOF' > "$PKG_ROOT/README.md"
# Volur Lite 交付运行说明 (Linux amd64)

本安装包是开箱即用的独立运行套件，包含已预编译完成的 Linux amd64 镜像及运行时配置。

## 一、快速运行

在目标服务器上解压后，直接执行一键启动脚本：

```bash
# 1. 启动服务 (自动导入镜像并运行)
chmod +x start.sh stop.sh
./start.sh
```

启动完成后，打开浏览器访问：`http://<您的服务器IP>` 即可进入平台。

## 二、常用维护命令

- **停止服务**：`./stop.sh`
- **查看运行日志**：`docker compose logs -f`
- **查看容器状态**：`docker compose ps`
- **重启后端应用**：`docker compose restart volur-app`

## 三、目录结构

- `volur-images.tar`：已打包的 amd64 Docker 镜像归档
- `docker-compose.yml`：轻量级容器编排文件
- `.env`：系统环境变量配置文件（可修改端口、数据库密码等）
- `start.sh`：一键导入镜像并拉起系统
- `stop.sh`：一键关闭系统
- `uploads/`：用户上传文件、题目附件与静态资源持久化目录
- `data/registry/`：题目容器私有镜像持久化目录
EOF

# 6. 生成最终交付压缩包
info "正在生成最终发布压缩包: $OUTPUT_FILE ..."
if [ "$COMPRESS_CMD" == "pigz" ]; then
    tar -cf - -C "$STAGING_DIR" volur-lite | pigz > "$OUTPUT_FILE"
else
    tar -czf "$OUTPUT_FILE" -C "$STAGING_DIR" volur-lite
fi

FINAL_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
echo "------------------------------------------------------------------"
success "打包成功！"
success "最终生成文件: $PROJECT_DIR/$OUTPUT_FILE"
success "最终文件大小: $FINAL_SIZE"
echo ""
echo -e "${GREEN}使用指引:${NC}"
echo "1. 将 $OUTPUT_FILE 复制或 scp 至目标 Linux x86_64 / amd64 服务器:"
echo "   scp $OUTPUT_FILE user@<目标服务器IP>:~/"
echo "2. 在目标服务器上解压并运行:"
echo "   tar -zxvf $OUTPUT_FILE"
echo "   cd volur-lite"
echo "   ./start.sh"
echo "=================================================================="
