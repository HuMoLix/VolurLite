# K3s kubeconfig 宿主机 Docker 映射管理、连通性探测重构与全策略沙箱仿真模式移除施工文档

## 1. 变更背景与目标

1. **宿主机 K3s Kubeconfig 映射与单一凭据约束**：
   - 平台运行在 Docker 容器（`volur-app`）内，为了能够调度和管理宿主机上的 K3s 集群，必须在 `docker-compose.yml` 中挂载宿主机的 `/etc/rancher/k3s/k3s.yaml:ro`。
   - 彻底废除以往在无 Kubeconfig 文件时通过 Master URL 直连自签证书的后备（Fallback）逻辑，保证 Kubeconfig 成为 K3s 集群身份认证与通信配置的**单一真实来源（Single Source of Truth）**。
2. **容器与宿主机网络边界自适应（连通性预检与超时熔断）**：
   - 宿主机 K3s 自动生成的 `k3s.yaml` 默认将 API Server Server URL 设置为 `https://127.0.0.1:6443` 或 `https://localhost:6443`。
   - 平台作为 Docker 容器运行时，容器内的 `127.0.0.1` 仅指向容器自身的 Network Namespace，无法直接与宿主机的 6443 端口通信。
   - 需引入网络自适应探测机制（包括 `host.docker.internal`、容器默认网关 IP、平台公网 Host 等优先级探测），同时将 `extra_hosts` 注入至 Compose 服务，并开启 TLS 证书信任避免 SAN 域名校验拦截。
3. **彻底删除沙箱仿真模式（Simulation / Mock Mode）**：
   - 清理所有在底层集群不可用、命令异常或容器不存在时无感伪造 `RUNNING` 状态或 mock 终端返回的逻辑，杜绝开发与生产环境下假状态导致的难以排查的暗坑，全面转为严格抛出 `BusinessException` 与标准非零退出码错误输出。

---

## 2. 详细改动点与实施清单

### 2.1 `docker-compose.yml` 部署编排升级
- **文件**：[`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)
- **改动**：
  1. 在 `volur-app` 服务下挂载只读卷：`- /etc/rancher/k3s/k3s.yaml:/etc/rancher/k3s/k3s.yaml:ro`；
  2. 在 `volur-app` 服务下增加主机映射：
     ```yaml
     extra_hosts:
       - "host.docker.internal:host-gateway"
     ```
     确保容器内可解析 `host.docker.internal` 直达宿主机网关端口。

### 2.2 K3s 运行时策略重构 (`K3sRuntimeStrategy.java`)
- **文件**：[`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
- **改动**：
  1. **废除 Master URL 后备直连与平铺默认配置**：
     - 移除 `volur.k3s.master-url` 依赖注入及相关未持有效凭据的备选逻辑；
     - `getClient()` 与 `testConnection()` 严格要求指定的 kubeconfig 文件存在且有效，若不存在则直接返回不可用或报错。
  2. **容器宿主机 API Server 动态端点解析与 TLS 信任适配**：
     - 新增 `buildEffectiveConfig(kubeconfigContent)` 方法：从 kubeconfig 解析原始 Server URL；
     - 新增 `resolveReachableHost(rawHost, port)` 方法：按优先级依序探测 `rawHost`、`host.docker.internal`、Linux 容器默认网关（从 `/proc/net/route` 解析）以及 `publicHost`；
     - 利用 `ConfigBuilder(baseConfig).withMasterUrl(effectiveMasterUrl).withTrustCerts(true)` 重建 Fabric8 配置，解决跨容器访问时自签证书未包含 `host.docker.internal` SAN 域名的证书校验阻断问题；
     - 新增 `resetClient()` 方法，在测试成功或集群配置修改时自动重置缓存客户端，即刻生效。
  3. **连通性预检与超时熔断防抖优化**：
     - 文件存在性阻断检查：快速判定 kubeconfig 挂载情况；
     - 基于低延迟（500ms~1000ms）TCP Socket 探针完成宿主机网络快速核验，杜绝底层离线时客户端阻塞 30+ 秒；
     - 基于 `CompletableFuture` 限制整体握手探针在 4000ms 内熔断。
  4. **彻底删除 K3s 沙箱仿真模式**：
     - 移除 `startContainer` 与 `startManualContainer` 中的 `mock-` 资源生成与仿真兜底，调度失败或客户端为空时直接抛出 `BusinessException(HttpStatus.INTERNAL_SERVER_ERROR)`；
     - 移除 `execCommand` 中的仿真终端伪造；移除 `simulateExecResponse` 辅助方法，命令异常时返回 `exitCode = 1` 及真实错误说明；
     - 优化 `getTerminalInfo`：Pod 不存在或未处于就绪状态时严格返回 `ready: false`。

### 2.3 Docker Swarm 运行时策略清理 (`DockerSwarmRuntimeStrategy.java`)
- **文件**：[`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java)
- **改动**：
  1. **移除 startContainer 容错仿真**：
     - 当 `docker service create` 执行未就绪或出现异常时，不再返回假 `RUNNING` 状态的 `ContainerRunResultDTO`，全面统一抛出 `BusinessException` 供上层统一事务与审计拦截。
  2. **移除 testConnection Socket 文件伪就绪检查**：
     - 移除单凭 `/var/run/docker.sock` 本地文件存在即盲目返回 `connected: true` 的代码，必须以 `docker info` 真实返回的 Engine 版本与 Swarm 状态为准。
  3. **移除 execCommand 沙箱终端仿真**：
     - 移除 `simulateExecResponse` 辅助方法；
     - 当 Swarm 任务容器不存在或底层执行异常时，返回明确的报错与退出码 1，杜绝返回 mock 数据。

### 2.4 集群管理服务联动 (`AdminClusterServiceImpl.java`)
- **文件**：[`AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
- **改动**：
  - 在 `updateSettings()` 保存集群基础参数后，显式调用 `k3sStrategy.resetClient()`，保证管理员更新 kubeconfig 路径或相关参数后，新设置即刻生效，无需重启服务。

---

## 3. 安全隐患修复与影响范围评估

1. **安全性提升**：
   - 彻底关闭了因没有合法证书凭证而尝试明文或无校验直连 Master URL 的潜在安全漏洞；
   - 杜绝了靶机沙箱仿真模式伪造 Flag（`volur{...}`）或虚拟返回成功导致的选手作弊、误判或比赛运行事故。
2. **影响范围**：
   - 仅影响容器编排调度底层模块 [`volur-container`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container) 与部署编排文件 [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)；
   - 业务层接口签名与响应契约未变动；全平台模块编译（`mvn test-compile`）全部一次性通过。
