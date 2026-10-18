#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

info() { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

MODE="slim"
SKIP_BUILD=false
TARGET_PLATFORM="linux/amd64"
OUTPUT_FILE=""
DO_PUSH=false
REGISTRY_PREFIX=""
IMAGE_TAG="latest"

normalize_platform() {
    local plat="$1"
    case "$plat" in
        amd64|x86_64) echo "linux/amd64" ;;
        arm64|aarch64) echo "linux/arm64" ;;
        *) echo "$plat" ;;
    esac
}

show_help() {
    cat <<EOF
Volur Lite 交付打包与推送工具

用法:
  ./package.sh [选项]

选项:
  -p, --platform <架构>   目标平台架构 (默认: linux/amd64，支持 amd64, arm64, linux/amd64, linux/arm64 等)
  --slim                 [默认] 仅打包 volur-app 与 volur-nginx 核心业务镜像
  --full                 全量离线交付包，包含 mysql, redis, registry
  --skip-build           跳过前后端源码镜像构建，直接导出本地现有镜像
  -o, --output <文件名>   指定输出的 tar.gz 文件名 (默认: volur-lite-<arch>.tar.gz)
  --push                 构建并推送镜像至远程 Registry (需指定 -r/--registry)
  -r, --registry <前缀>  远程镜像仓库前缀 (如 docker.io/myorg 或 harbor.example.com/volur)
  -t, --tag <标签>       镜像版本标签 (默认: latest)
  -h, --help             显示帮助信息

示例:
  ./package.sh                                   # 打包默认 Linux amd64 业务镜像
  ./package.sh -p arm64                          # 打包 Linux arm64 业务镜像
  ./package.sh -p amd64 --full                   # 打包包含数据库的全量 Linux amd64 离线包
  ./package.sh -p linux/amd64,linux/arm64 --push -r registry.example.com/volur
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -p|--platform)
            TARGET_PLATFORM="$(normalize_platform "$2")"
            shift 2
            ;;
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
        --push)
            DO_PUSH=true
            shift
            ;;
        -r|--registry)
            REGISTRY_PREFIX="${2%/}"
            shift 2
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
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

SHORT_ARCH=$(echo "$TARGET_PLATFORM" | sed 's|^linux/||' | tr '/' '-' | tr ',' '_')
if [ -z "$OUTPUT_FILE" ]; then
    OUTPUT_FILE="volur-lite-${SHORT_ARCH}.tar.gz"
fi

echo "=================================================================="
echo -e "${CYAN}             Volur Lite 镜像交付打包与发布工具              ${NC}"
echo "=================================================================="
info "项目根目录:   $PROJECT_DIR"
info "目标架构平台: $TARGET_PLATFORM"
info "镜像版本标签: $IMAGE_TAG"
if [ "$DO_PUSH" = true ]; then
    info "操作模式:     远程 Registry 推送模式"
    info "仓库前缀:     $REGISTRY_PREFIX"
else
    info "操作模式:     离线归档模式 ($MODE)"
    info "输出目标文件: $OUTPUT_FILE"
    info "是否跳过构建: $SKIP_BUILD"
fi
echo "------------------------------------------------------------------"

if ! command -v docker &>/dev/null; then
    error "未找到 docker 命令，请确保已安装 Docker 并加入环境变量 PATH"
    exit 1
fi

if ! docker info &>/dev/null; then
    error "Docker 守护进程未启动或无法连通，请先启动 Docker 服务"
    exit 1
fi

if [ "$DO_PUSH" = true ]; then
    if [ -z "$REGISTRY_PREFIX" ]; then
        error "启用 --push 时必须通过 -r 或 --registry 指定远程仓库前缀"
        exit 1
    fi

    info "正在并行交叉编译并推送前后端镜像至 [$REGISTRY_PREFIX]..."
    
    docker buildx build \
        --platform "$TARGET_PLATFORM" \
        -t "$REGISTRY_PREFIX/volur-app:$IMAGE_TAG" \
        -f "$PROJECT_DIR/src/volur-lite-platform/Dockerfile" \
        "$PROJECT_DIR/src/volur-lite-platform" \
        --push &
    PID_APP=$!

    docker buildx build \
        --platform "$TARGET_PLATFORM" \
        -t "$REGISTRY_PREFIX/volur-nginx:$IMAGE_TAG" \
        -f "$PROJECT_DIR/src/volur-lite-view-NEXT/Dockerfile" \
        "$PROJECT_DIR/src/volur-lite-view-NEXT" \
        --push &
    PID_NGINX=$!

    wait "$PID_APP" || { error "后端镜像构建并推送失败"; exit 1; }
    wait "$PID_NGINX" || { error "前端镜像构建并推送失败"; exit 1; }

    success "多架构镜像构建并推送成功！"
    success "镜像地址: $REGISTRY_PREFIX/volur-app:$IMAGE_TAG"
    success "镜像地址: $REGISTRY_PREFIX/volur-nginx:$IMAGE_TAG"
    exit 0
fi

COMPRESS_CMD="gzip"
if command -v pigz &>/dev/null; then
    COMPRESS_CMD="pigz"
    info "检测到多线程压缩工具 pigz，将启用多核极速压缩"
fi

STAGING_DIR="$(mktemp -d -t volur-pkg-XXXXXX)"
PKG_ROOT="$STAGING_DIR/volur-lite"
mkdir -p "$PKG_ROOT"

cleanup() {
    rm -rf "$STAGING_DIR"
}
trap cleanup EXIT

HOST_ARCH="$(uname -m)"
LOCAL_APP_ID=$(docker image inspect volur-app:latest --format '{{.Id}}' 2>/dev/null || true)
LOCAL_NGINX_ID=$(docker image inspect volur-nginx:latest --format '{{.Id}}' 2>/dev/null || true)
LOCAL_REDIS_ID=$(docker image inspect redis:8.6.2-alpine --format '{{.Id}}' 2>/dev/null || true)
LOCAL_REGISTRY_ID=$(docker image inspect registry:2 --format '{{.Id}}' 2>/dev/null || true)
LOCAL_MYSQL_ID=$(docker image inspect mysql:9.6.0 --format '{{.Id}}' 2>/dev/null || true)

restore_local_tags() {
    if [[ "$HOST_ARCH" =~ ^(arm64|aarch64)$ ]] && [ "$TARGET_PLATFORM" != "linux/arm64" ]; then
        info "正在恢复宿主机本地原生 ARM64 镜像标签引用..."
        [ -n "$LOCAL_APP_ID" ] && docker tag "$LOCAL_APP_ID" volur-app:latest 2>/dev/null || true
        [ -n "$LOCAL_NGINX_ID" ] && docker tag "$LOCAL_NGINX_ID" volur-nginx:latest 2>/dev/null || true
        [ -n "$LOCAL_REDIS_ID" ] && docker tag "$LOCAL_REDIS_ID" redis:8.6.2-alpine 2>/dev/null || true
        [ -n "$LOCAL_REGISTRY_ID" ] && docker tag "$LOCAL_REGISTRY_ID" registry:2 2>/dev/null || true
        [ -n "$LOCAL_MYSQL_ID" ] && docker tag "$LOCAL_MYSQL_ID" mysql:9.6.0 2>/dev/null || true
    fi
}

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

    wait "$PID_APP" || { error "后端镜像编译失败"; restore_local_tags; exit 1; }
    wait "$PID_NGINX" || { error "前端镜像编译失败"; restore_local_tags; exit 1; }
    success "前后端镜像交叉编译完成！"
else
    warn "已指定 --skip-build，跳过源码镜像构建阶段"
fi

IMAGES_TO_SAVE=("volur-app:latest" "volur-nginx:latest")

if [ "$MODE" == "full" ]; then
    info "步骤 2/4: 正在拉取 / 核验基础设施镜像 [$TARGET_PLATFORM]..."
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

info "步骤 3/4: 正在导出 Docker 镜像为 volur-images.tar..."
info "打包镜像清单: ${IMAGES_TO_SAVE[*]}"

docker save "${IMAGES_TO_SAVE[@]}" -o "$PKG_ROOT/volur-images.tar"
IMAGE_TAR_SIZE=$(du -h "$PKG_ROOT/volur-images.tar" | cut -f1)
success "Docker 镜像包导出完成，未压缩体积: $IMAGE_TAR_SIZE"

restore_local_tags

info "步骤 4/4: 正在装配独立部署运行套件..."

cat <<'EOF' > "$PKG_ROOT/docker-compose.yml"
services:
  volur-mysql:
    image: mysql:9.6.0
    container_name: volur-mysql
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - CHOWN
      - SETUID
      - SETGID
      - DAC_OVERRIDE
    read_only: true
    tmpfs:
      - /tmp:rw,noexec,nosuid,size=1024m
      - /var/run/mysqld:rw,noexec,nosuid,size=32m
    environment:
      MYSQL_ROOT_PASSWORD: ${VOLUR_MYSQL_ROOT_PASSWORD:-${VOLUR_MYSQL_PASSWORD}}
      MYSQL_PASSWORD: ${VOLUR_MYSQL_PASSWORD}
      MYSQL_DATABASE: volur_db
      MYSQL_USER: volur
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00
      - --local-infile=0
      - --secure-file-priv=/var/lib/mysql-files
      - --max_connections=500
      - --max_connect_errors=100000
      - --back_log=500
      - --innodb_buffer_pool_size=1024M
      - --innodb_buffer_pool_instances=4
      - --innodb_log_buffer_size=16M
      - --innodb_redo_log_capacity=256M
      - --innodb_read_io_threads=4
      - --innodb_write_io_threads=4
      - --thread_cache_size=64
      - --table_open_cache=2000
      - --table_definition_cache=1000
      - --tmp_table_size=64M
      - --max_heap_table_size=64M
      - --innodb_flush_log_at_trx_commit=2
    volumes:
      - volur-mysql-data:/var/lib/mysql
    ulimits:
      core: 0
      nofile:
        soft: 65535
        hard: 65535
    deploy:
      resources:
        limits:
          cpus: '4.0'
          memory: 4096M
        reservations:
          cpus: '1.0'
          memory: 1024M
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "volur", "-p${VOLUR_MYSQL_PASSWORD}" ]
      interval: 5s
      timeout: 5s
      retries: 10
    networks:
      - volur-network
    restart: always

  volur-redis:
    image: redis:8.6.2-alpine
    container_name: volur-redis
    user: "999:999"
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    read_only: true
    tmpfs:
      - /tmp:rw,noexec,nosuid,size=256m
    command:
      - redis-server
      - --appendonly
      - "yes"
      - --user
      - default
      - "off"
      - --user
      - volur
      - "on"
      - ">${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}"
      - "~*"
      - "&*"
      - "+@all"
      - "-@admin"
      - "-@dangerous"
      - --rename-command
      - MODULE
      - ""
      - --rename-command
      - DEBUG
      - ""
      - --maxclients
      - "10000"
      - --tcp-backlog
      - "511"
      - --timeout
      - "0"
      - --tcp-keepalive
      - "300"
      - --io-threads
      - "4"
      - --io-threads-do-reads
      - "yes"
      - --maxmemory
      - "1536mb"
      - --maxmemory-policy
      - "allkeys-lru"
    ulimits:
      core: 0
      nofile:
        soft: 65535
        hard: 65535
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2048M
        reservations:
          cpus: '0.5'
          memory: 512M
    healthcheck:
      test: [ "CMD", "redis-cli", "--user", "volur", "-a", "${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}", "ping" ]
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
      - VOLUR_MYSQL_PASSWORD=${VOLUR_MYSQL_PASSWORD}
      - VOLUR_REDIS_HOST=volur-redis
      - VOLUR_REDIS_PASSWORD=${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}
      - VOLUR_JWT_SECRET=${VOLUR_JWT_SECRET:-VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!}
      - VOLUR_CONTAINER_DOCKER_HOST=unix:///var/run/docker.sock
      - VOLUR_CONTAINER_REGISTRY_INTERNAL_URL=http://volur-registry:5000
      - VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT=${REGISTRY_PORT:-5000}
      - VOLUR_CONTAINER_REGISTRY_ENABLED=true
    ulimits:
      nofile:
        soft: 65535
        hard: 65535
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

if [ -f "$PROJECT_DIR/.env" ]; then
    cp "$PROJECT_DIR/.env" "$PKG_ROOT/.env.example"
elif [ -f "$PROJECT_DIR/.env.example" ]; then
    cp "$PROJECT_DIR/.env.example" "$PKG_ROOT/.env.example"
else
    cat <<'EOF' > "$PKG_ROOT/.env.example"
NGINX_HTTP_PORT=80
REGISTRY_PORT=5000
VOLUR_MYSQL_PASSWORD=volur_mysql_secret_2026
VOLUR_REDIS_PASSWORD=volur_redis_secret_2026
VOLUR_JWT_SECRET=VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!
EOF
fi
cp "$PKG_ROOT/.env.example" "$PKG_ROOT/.env"

if [ -f "$PROJECT_DIR/scripts/upgrade/upgrade_to_latest.sql" ]; then
    cp "$PROJECT_DIR/scripts/upgrade/upgrade_to_latest.sql" "$PKG_ROOT/upgrade_to_latest.sql"
fi

mkdir -p "$PKG_ROOT/uploads" "$PKG_ROOT/data/registry"

cat <<'EOF' > "$PKG_ROOT/start.sh"
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "              Volur Lite 一键部署拉起                     "
echo "=========================================================="

if ! command -v docker &>/dev/null; then
    echo "[错误] 未检测到 docker，请先安装 Docker (或 Docker Engine)" >&2
    exit 1
fi

if ! docker info &>/dev/null; then
    echo "[错误] Docker 服务未运行，请先启动 Docker 服务 (sudo systemctl start docker)" >&2
    exit 1
fi

if [ -f "volur-images.tar" ]; then
    echo "==> 正在导入离线 Docker 镜像包 (volur-images.tar)..."
    docker load -i volur-images.tar
    echo "==> 镜像导入完成！"
elif [ -f "volur-images.tar.gz" ]; then
    echo "==> 正在导入离线 Docker 镜像包 (volur-images.tar.gz)..."
    gzip -dc volur-images.tar.gz | docker load
    echo "==> 镜像导入完成！"
fi

if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "==> 自动从 .env.example 生成 .env 配置文件..."
    cp .env.example .env
fi

mkdir -p uploads data/registry

if [ ! -e "/etc/rancher/k3s/k3s.yaml" ]; then
    echo "==> 提示: 宿主机当前未发现 /etc/rancher/k3s/k3s.yaml"
    echo "==> 若您计划使用 K3s 模式，请在安装 K3s 后挂载；若使用 Docker Swarm 模式则无需理会"
    sudo mkdir -p /etc/rancher/k3s 2>/dev/null || true
    sudo touch /etc/rancher/k3s/k3s.yaml 2>/dev/null || true
fi

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

cat <<EOF > "$PKG_ROOT/README.md"
# Volur Lite 交付运行说明 ($TARGET_PLATFORM)

本安装包是开箱即用的独立运行套件，包含已预编译完成的 $TARGET_PLATFORM 镜像及运行时配置。

## 一、快速运行

在目标服务器上解压后，直接执行一键启动脚本：

\`\`\`bash
chmod +x start.sh stop.sh
./start.sh
\`\`\`

启动完成后，打开浏览器访问：\`http://<您的服务器IP>\` 即可进入平台。

## 二、常用维护命令

- **停止服务**：\`./stop.sh\`
- **查看运行日志**：\`docker compose logs -f\`
- **查看容器状态**：\`docker compose ps\`
- **重启后端应用**：\`docker compose restart volur-app\`

## 三、目录结构

- \`volur-images.tar\`：已打包的 $TARGET_PLATFORM Docker 镜像归档
- \`docker-compose.yml\`：轻量级容器编排文件
- \`.env\`：系统环境变量配置文件（可修改端口、数据库密码等）
- \`start.sh\`：一键导入镜像并拉起系统
- \`stop.sh\`：一键关闭系统
- \`uploads/\`：用户上传文件、题目附件与静态资源持久化目录
- \`data/registry/\`：题目容器私有镜像持久化目录
EOF

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
echo "1. 将 $OUTPUT_FILE 复制或 scp 至目标服务器:"
echo "   scp $OUTPUT_FILE user@<目标服务器IP>:~/"
echo "2. 在目标服务器上解压并运行:"
echo "   tar -zxvf $OUTPUT_FILE"
echo "   cd volur-lite"
echo "   ./start.sh"
echo "=================================================================="
