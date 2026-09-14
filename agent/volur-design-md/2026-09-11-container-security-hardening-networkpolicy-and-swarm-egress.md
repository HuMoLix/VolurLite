# 容器安全加固、K3s 命名空间网络策略隔离与 Docker Swarm 出网控制施工文档

## 1. 变更背景与安全隐患分析

在 CTF / 动态靶场对抗演练场景中，平台启动的赛题容器属于**不可信执行环境（Untrusted Workload）**。选手或恶意攻击者在获取靶机容器的 Shell 权限（RCE / 逃逸尝试）后，通常会尝试以下攻击路径：
1. **利用 Kubernetes ServiceAccount 凭据进行集群提权**：
   - 默认情况下，Kubernetes 会将默认 ServiceAccount 的 JWT Token 自动挂载至容器内部的 `/var/run/secrets/kubernetes.io/serviceaccount/token`。
   - 若靶机存在 SSRF 或命令执行，攻击者可直接读取该 Token 并向 API Server 发起请求，尝试查询其他资源或在集群内提权。
2. **内网横向移动与核心数据源探测 (SSRF / Lateral Movement)**：
   - 攻击者在靶机容器内利用扫描工具探测宿主机私有网段（`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`）、云厂商实例元数据接口（`169.254.169.254`）或其他选手的靶机容器。
   - 若 MySQL、Redis 等核心中间件暴露了宿主机端口，或未在网络层进行严格边界阻断，攻击者可直接穿透内网探测攻击平台基础设施。
3. **数据外带与反弹 Shell (Data Exfiltration / Reverse Shell)**：
   - 在某些内网盲打或纯静态/无出网需求的赛题中，容器若默认具备出网（Egress）能力，攻击者可通过 DNS/ICMP/HTTP 隐蔽信道将 Flag 数据外带至公网，或向外部 VPS 建立反向 Shell，降低解题门槛并扩大攻击面。

针对上述安全风险，本项目实施全面的容器安全加固与多集群网络隔离架构改造。

---

## 2. 核心实施方案与详细改动点

### 2.1 基础中间件宿主机网络隔离确认
- **核验对象**：[`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)
- **实现机制**：
  - 确认 `volur-mysql` (3306) 与 `volur-redis` (6379) 均未在 Compose 中配置 `ports:` 端口映射；
  - 核心数据库与缓存完全闭环在 Docker 内置桥接网络 `volur-network` 中，宿主机外部网络及外部网络接口无法直接访问 3306 / 6379 端口；
  - 平台唯一暴露至宿主机的入口仅为 `volur-nginx` 代理的 HTTP 服务端口（`${NGINX_HTTP_PORT:-80}:80`）。

### 2.2 K3s 靶机禁用 ServiceAccount Token 自动挂载
- **核心文件**：[`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
- **改动点**：
  1. 在动态靶机启动方法 `startContainer(...)` 中，为 `PodSpecBuilder` 显式声明：
     ```java
     var podSpecBuilder = new PodSpecBuilder()
             .withRestartPolicy("Never")
             .withAutomountServiceAccountToken(false)
             .addNewContainer()
             ...
     ```
  2. 在管理端手动启动容器方法 `startManualContainer(...)` 中，同步添加 `.withAutomountServiceAccountToken(false)`；
  3. **安全效益**：从根本上杜绝 K3s 在赛题 Pod 中挂载 API Server 访问凭证，彻底关闭通过 ServiceAccount 进行集群渗透的路径。

### 2.3 K3s 命名空间级别 NetworkPolicy 隔离策略下发
- **核心文件**：[`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
- **改动点**：
  - 抽取并实现 `ensureNamespaceAndNetworkPolicy(KubernetesClient client, String namespace)` 方法：
    1. **Namespace 状态确保**：在客户端初始化（`getClient()`）以及每次调度 Pod 之前，自动检查并创建目标靶机命名空间（默认 `volur-challenges`）；
    2. **NetworkPolicy 自动化部署**：向该命名空间下发并持续维持名为 `volur-challenge-isolation` 的 `NetworkPolicy` 规则：
       - **Pod 匹配范围**：`podSelector: {}`（全量覆盖命名空间下所有靶机 Pod）；
       - **PolicyTypes**：同时启用 `Ingress` 与 `Egress` 控制；
       - **Ingress 规则**：允许入站流量（外部选手可通过 NodePort 转发或路由 Mesh 正常访问赛题暴露的 Web/Pwn 端口）；
       - **Egress 规则 1 (DNS 基础服务)**：仅允许出站至 UDP 53 和 TCP 53 端口，确保基础域名解析（CoreDNS 位于 `kube-system`）不被阻断；
       - **Egress 规则 2 (公网可达但私网全量阻断)**：出网 CIDR 声明为 `0.0.0.0/0`，并通过 `except` 严格排除所有内网网段与敏感链路：
         - `10.0.0.0/8`：私网 Class A，覆盖 K3s Pod CIDR (`10.42.0.0/16`) 与 Service CIDR (`10.43.0.0/16`)，阻断靶机间横向移动与 API Server 探测；
         - `172.16.0.0/12`：私网 Class B，覆盖 Docker 宿主机网络与平台 Compose 桥接网络；
         - `192.168.0.0/16`：私网 Class C，覆盖企业与机房局域网；
         - `169.254.0.0/16`：链路本地地址及云元数据（严格阻断针对 AWS / GCP / 阿里云 `169.254.169.254` 的 SSRF 攻击）；
         - `127.0.0.0/8`：本地回环网段；
         - `100.64.0.0/10`：运营商级 NAT (CGNAT)；
         - `224.0.0.0/4`：组播网络。
  - 使用 Fabric8 的 `.resource(policy).createOrReplace()` 进行幂等式部署与更新。

### 2.4 Docker Swarm 模式容器出网控制选项与内部隔离网络
- **核心文件**：
  - [`SystemSettingKeys.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)
  - [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)
  - [`AdminClusterSettingsDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterSettingsDTO.java)
  - [`AdminClusterSettingsVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterSettingsVO.java)
  - [`AdminContainerManualStartDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminContainerManualStartDTO.java)
  - [`AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
  - [`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java)
- **改动点**：
  1. **系统动态配置键与持久化**：
     - 新增系统配置键：`SystemSettingKeys.CONTAINER_DOCKER_SWARM_OUTBOUND_ENABLED = "system.container.docker_swarm.outbound_enabled"`；
     - 在 `SystemSettingInitializer` 中注册默认值：`"false"`（默认禁止出网以保证最大安全隔离）；
     - 更新 `AdminClusterSettingsDTO` 与 `AdminClusterSettingsVO` 暴露 `dockerSwarmOutboundEnabled` 属性；
     - 在 `AdminClusterServiceImpl` 的 `getSettings()` 与 `updateSettings()` 中实现读写联动。
  2. **手工启动容器个性化出网参数**：
     - 在 `AdminContainerManualStartDTO` 中添加 `outboundEnabled` 字段，允许管理员在临时调试或测试手工启动单个容器时动态指定是否放行出网。
  3. **Swarm Overlay 内部网络自动分流架构**：
     - 在 `DockerSwarmRuntimeStrategy` 中实现 `resolveEffectiveSwarmNetwork(baseNetwork, outboundEnabled)`：
       - 当允许出网时，容器挂载至标准覆盖网络（如 `volur-swarm-network`）；
       - 当禁止出网时，自动路由至隔离网络：`${baseNetwork}-internal`（如 `volur-swarm-network-internal`）；
     - 升级 `ensureOverlayNetwork(String network, String swarmApiUrl, boolean internal)`：
       - 若 `internal == true`，自动在 `docker network create` 指令中追加 `--internal` 参数；
       - 基于 Docker Swarm Routing Mesh 机制，`--publish` 发布的外部访问端口依托独立的 Ingress 网络转发，外部选手能够正常访问题目，而题目容器内部彻底失去外部网络路由网关，无法向外发起任何主动网络连接。

### 2.5 前端管控台视图与国际化对齐
- **核心文件**：
  - [`src/volur-lite-view/src/types/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/cluster/index.ts)
  - [`src/volur-lite-view/src/types/container/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/container/index.ts)
  - [`src/volur-lite-view/src/locales/zh_CN/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/clusters.json)
  - [`src/volur-lite-view/src/locales/en_US/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/clusters.json)
  - [`src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue)
- **改动点**：
  1. 在 TS 实体定义中同步补全 `dockerSwarmOutboundEnabled?: boolean` 与 `outboundEnabled?: boolean`；
  2. 在中英文语言包中增加 `swarm_outbound` 与 `swarm_outbound_hint` 键值；
  3. 在 `AdminClusterSettingsView.vue` 的 Docker Swarm 配置区中添加 `<n-switch>` 开关表单项，并与 `settings`、`form`、`fetchSettings` 及保存接口完整双向绑定。

---

## 3. 验证与测试记录

1. **后端全量模块编译验证**：
   - 执行：`mvn test-compile -DskipTests`（于 `src/volur-lite-platform` 根工程）；
   - 结果：**12 个模块全部 `BUILD SUCCESS`**，Total time: 10.581s，包含 Fabric8 NetworkPolicy 依赖构建、DTO/VO 字段扩展、系统配置项与策略类均编译通过。
2. **前端类型检查与生产构建验证**：
   - 执行：`npx vue-tsc --noEmit`；
   - 结果：零类型报错，通过。
   - 执行：`npm run build`；
   - 结果：`vite build` 成功完成打包产物输出，4276 个模块转换成功。
3. **架构与规范合规自检**：
   - 依赖注入统一使用 Lombok `@RequiredArgsConstructor` 与 `private final`，无字段注入；
   - 领域模型收敛在 `domain.dto` 与 `domain.vo` 包下，严格遵守驼峰命名；
   - 系统设置采用标准小写点分层级键名 `system.container.docker_swarm.outbound_enabled`；
   - 前后端类型与 i18n 语言配置 1:1 严格对齐。
