# Docker Registry 端口规范化统一 (5001) 与远程服务器迁移割接方案

## 1. 变更背景与缺陷诊断

在 `docker-compose.yml` 中，私有镜像仓库服务 `volur-registry` 的外部端口映射原配置为：
```yaml
ports:
  - "${REGISTRY_PORT:-5000}:5000"
```
而后端主服务 `volur-app` 注入的环境变量配置为：
```yaml
- VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT=${REGISTRY_PORT:-5001}
```

### 1.1 缺陷危害
1. **环境变量缺省时产生“端口裂脑”（Split-Brain Mismatch）**：
   - 当部署环境未显式定义 `REGISTRY_PORT`（如未提供 `.env` 或缺少该变量）时，`volur-registry` 在宿主机实际监听 `5000` 端口，而 `volur-app` 内部服务却使用 `5001` 端口拼装镜像推送/拉取标签（`<publicHost>:5001/image:tag`）；
   - 这导致外部 Docker 守护进程、K3s 节点及远程工作节点在拉取/推送镜像时出现 `Connection Refused`，靶机容器拉取与题目生命周期调度全链路阻断。
2. **破坏全局单一真实源 (SST)**：
   - 项目中 `.env.example`（`REGISTRY_PORT=5001`）、`application.yml` 与 `application-dev.yml`（`public-port: 5001`）均已统一收敛至 `5001`，该配置破坏了全局一致性。
3. **宿主机系统级端口冲突隐患**：
   - macOS（Monterey 及更高版本）系统的 AirPlay 隔空播放接收器（ControlCenter）默认强制占用宿主机 `0.0.0.0:5000` 端口，导致 `volur-registry` 无法绑定端口，并级联卡死 `volur-app` 的启动前置依赖检查。

---

## 2. 代码变更明细

修改 `docker-compose.yml`：
- 将 `volur-registry` 宿主机端口映射默认值从 `5000` 统一修正为 `5001`：
  ```yaml
  ports:
    - "${REGISTRY_PORT:-5001}:5000"
  ```
- 保持容器内部网络通信 `http://volur-registry:5000` 不变（容器内部服务监听标准端口 5000）。

---

## 3. 远程服务器已绑定 5000 端口的平滑迁移与割接方案

针对已在生产或测试环境远程服务器上运行、且已绑定在 5000 端口的实例，割接至规范 5001 端口的标准执行步骤如下：

### 步骤 1：更新远程配置与编排文件
1. 检查远程服务器工作目录下的 `.env` 文件：
   - 若存在 `REGISTRY_PORT=5000`，修改为 `REGISTRY_PORT=5001`；
   - 若不存在该行，追加写入 `REGISTRY_PORT=5001`。
2. 同步更新 `docker-compose.yml` 文件中的默认端口映射声明：
   ```bash
   git pull origin main
   ```

### 步骤 2：云服务商安全组与宿主机防火墙更新
1. **云平台安全组（Security Group）**：
   - 在云服务器控制台（如阿里云、腾讯云、AWS 等）为实例入方向新增规则：放行 TCP `5001` 端口；
   - 确认各 K3s Node 节点或主控节点能正常访问 `5001` 端口后，废弃并删除原 `5000` 端口的入方向规则。
2. **宿主机本地防火墙**：
   - 若使用 `ufw`：
     ```bash
     sudo ufw allow 5001/tcp
     sudo ufw delete allow 5000/tcp
     sudo ufw reload
     ```
   - 若使用 `firewalld`：
     ```bash
     sudo firewall-cmd --permanent --add-port=5001/tcp
     sudo firewall-cmd --permanent --remove-port=5000/tcp
     sudo firewall-cmd --reload
     ```

### 步骤 3：更新 Docker Daemon 与 K3s 镜像仓库信任配置
私有 Registry 通常采用 HTTP 协议（非 TLS），各节点均配置了非安全仓库地址：
1. **Docker Daemon (`/etc/docker/daemon.json`)**：
   - 将原 `insecure-registries` 中的 `5000` 端口调整为 `5001`：
     ```json
     {
       "insecure-registries": ["127.0.0.1:5001", "<REMOTE_SERVER_IP>:5001"]
     }
     ```
   - 重载配置并重启 Docker 服务：
     ```bash
     sudo systemctl daemon-reload
     sudo systemctl restart docker
     ```
2. **K3s / Containerd 配置 (`/etc/rancher/k3s/registries.yaml`)**（若集群使用 K3s）：
   - 将 endpoint 与 mirror 配置由 `5000` 调整为 `5001`：
     ```yaml
     mirrors:
       "<REMOTE_SERVER_IP>:5001":
         endpoint:
           - "http://<REMOTE_SERVER_IP>:5001"
     ```
   - 重启 K3s 服务：
     ```bash
     sudo systemctl restart k3s
     # 或工作节点: sudo systemctl restart k3s-agent
     ```

### 步骤 4：宿主机历史镜像标签迁移（可选）
持久化存储 `./data/registry` 中的底层分层数据不受外部端口变化影响。但宿主机 Docker 缓存中若存在打了旧端口标签的镜像，可通过脚本快速重新打标，避免重新推送或重新拉取：
```bash
docker images --format '{{.Repository}}:{{.Tag}}' | grep ':5000/' | while read -r img; do
    new_img=$(echo "$img" | sed 's/:5000\//:5001\//')
    docker tag "$img" "$new_img"
done
```

### 步骤 5：重启容器完成端口割接
重启 `volur-registry` 与 `volur-app` 容器使端口映射和环境变量生效：
```bash
docker compose up -d --force-recreate volur-registry volur-app
```

### 步骤 6：连通性验证
1. 检查宿主机监听端口：
   ```bash
   ss -tulpn | grep 5001
   ```
2. 验证 Registry API 响应：
   ```bash
   curl -I http://127.0.0.1:5001/v2/
   # 预期返回 HTTP/1.1 200 OK
   ```
3. 验证平台后端健康探测与镜像 Catalog 接口：
   - 登录 Volur Lite 管理端，进入镜像管理或靶机管理，验证 Catalog 检索与容器实例启动是否正常。
