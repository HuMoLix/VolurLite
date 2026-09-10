# 集群扩展节点 addNode 业务逻辑重构与主机环境信息探测审计记录

## 1. 变更背景与需求概述

针对平台集群节点扩展（`addNode`）流程中的节点角色归属、网络探测、身份认证与系统环境探测逻辑，进行了全流程分析与重构优化：
1. **统一 Worker 与 Resource 节点语义**：
   - 确立集群拓扑原则：当集群中仅存在 Master 节点时，Master 节点同时承担 Control Node（控制面）与 Resource Node（运行赛题容器与镜像挂载）；
   - 当集群中存在 Worker 节点时，Master 节点仅作为纯 Control Node，所有的赛题容器与镜像调度全部放置在 Worker 节点上，即 **Worker 节点等价于 Resource 节点**；
   - 梳理并明确了无需在 DTO 与前端界面保留两个独立决策判断符号的架构理念。
2. **前置网络可达性探测**：
   - 在解析 `sshPort` 之前，强制对目标节点 IP 进行网络可达性探测，避免因目标 IP 离线或网关不可达导致长时间阻塞。
3. **严格凭据约束与 SSH 前置握手核验**：
   - 在 `AdminClusterNodeCreateDTO` 中将 `sshPort` 与 `sshUser` 设为必填字段；
   - 明确认证方式为密码认证或私钥认证（二选一）；
   - 在初始化 `osInfo` 之前，先建立 SSH 连接并完成凭据认证核验。
4. **深度系统与环境信息探测收集**：
   - SSH 连接建立后，主动采集目标主机的操作系统版本（`/etc/os-release`）、系统内核信息（`uname -r`）及硬件架构（`uname -m`），并组装规范的 `osInfo`；
   - 收集硬件资源规格（CPU 核心数与物理内存总量）与 Docker 容器引擎就绪状态。

---

## 2. 核心架构设计与详细改动

### 2.1 Worker 节点与 Resource 节点决策模型收敛分析

#### 2.1.1 业务架构模型分析
在原设计中，`request.getRole()`（"MASTER" / "WORKER"）与 `request.getIsResourceNode()`（Boolean）作为两个独立输入参数存在，容易导致以下问题：
- 允许管理员在前端勾选一个非资源节点的 Worker 节点，或者在具备 Worker 节点的情况下仍然让 Master 节点调度容器，破坏了平台“控制面与计算资源隔离”的核心原则。

根据最新架构设计：
- **场景 A（单节点 / 纯 Master 集群）**：集群仅有 Master 节点，此时 Master 兼任控制与资源计算，`isResourceNode = true`。
- **场景 B（多节点 / 包含 Worker 集群）**：集群中一旦存在 Worker 节点，所有容器必须调度至 Worker，Master 节点无条件退回纯控制面（`isResourceNode = false`），而新加入的 Worker 节点无条件为资源节点（`isResourceNode = true`）。

#### 2.1.2 结论与重构落地
- **无需存在 `isWorker` 与 `isResource` 两个独立的判断符号**：
  在 `addNode` 逻辑中，节点的资源属性完全由其角色与集群拓扑推导：
  ```java
  boolean isWorker = "WORKER".equalsIgnoreCase(request.getRole());
  boolean isResource = isWorker || !clusterNodeRepository.existsByRole("WORKER");
  ```
  - 当添加 Worker 节点时，自动遍历将集群中所有 Master 节点的 `isResourceNode` 设为 `false`；
  - 实体表 `ClusterNode` 的 `is_resource_node` 列继续保留，作为底层调度器快速筛选可用计算节点的单一真实来源；
  - 前端界面中在新增 Worker 节点时锁定资源节点开关，避免歧义输入。

---

### 2.2 目标 IP 网络连通性探测实现 (`checkIpReachable`)

在进入 SSH 端口与凭据解析之前，必须验证平台能否连接目标 IP。为避免单一检测手段在特殊网络环境（如云主机安全组禁 ping、非 root 容器无 raw socket 权限等）下的误判，设计了四级渐进式探测策略：
1. **本地回环/本机直通判定**：检测 `127.0.0.1`、`localhost` 或本机接口，直接判定可达；
2. **Java 原生 `isReachable(2000)`**：尝试 ICMP Echo Request；
3. **系统 ping 命令探测**：调用底层 OS `ping -c 1 -W 2 <ip>` 执行单次探测；
4. **TCP 路由探测（降级容灾）**：针对禁止 ICMP ping 的云网络环境，向目标主机的候选端口（SSH 端口或 22）发起轻量级 Socket 连接：
   - 若 TCP 握手成功，证明 IP 完全可达；
   - 若捕获到 `ConnectException`（收到对端返回的 TCP RST 报文），证明网络路由可达且目标主机存活在线；
   - 若全部探测超时或路由不可达，抛出友好业务异常 `BusinessException(HttpStatus.BAD_REQUEST, "平台无法连接到目标节点 IP ...")`。

---

### 2.3 SSH 必填约束与凭据二选一核验 (`verifySshConnectivity`)

1. **DTO 字段级校验 (`AdminClusterNodeCreateDTO.java`)**：
   - `sshPort`: `@NotNull(message = "SSH端口不能为空")`, `@Min(1)`, `@Max(65535)`；
   - `sshUser`: `@NotBlank(message = "SSH登录用户名不能为空")`, `@Size(min = 1, max = 64)`；
   - `sshPassword` / `sshKey`: 明确支持密码或私钥二选一。
2. **业务级二选一约束 (`AdminClusterServiceImpl.java`)**：
   - 校验必须至少提供密码或私钥凭据之一，否则直接拦截抛出 `400 Bad Request`。
3. **前置 SSH 连通与身份认证核验**：
   - 在声明 `String osInfo = "Linux (x86_64)";` 之前，调用 `verifySshConnectivity` 建立 JSch 会话；
   - 针对认证失败（`Auth fail`）、端口连接被拒绝（`Connection refused`）、连接超时或私钥格式错误（`invalid privatekey`）进行精准语义捕获并向管理员反馈具体失败原因。

---

### 2.4 系统环境信息采集与装配

在 SSH 验证通过后，按序执行指令采集主机运行环境：
1. **系统内核与硬件架构**：
   - 执行 `uname -r && uname -m`，精准提取内核版本（如 `6.6.0-43-generic`）与硬件架构（如 `x86_64`、`aarch64`）。
2. **操作系统发行版与版本号**：
   - 优先读取 `/etc/os-release` 中的 `PRETTY_NAME`（如 `Ubuntu 24.04.1 LTS`, `Debian GNU/Linux 12` 等），兼容 `/etc/redhat-release` 与 `/etc/issue`。
3. **格式化组装与长度保护**：
   - 将系统版本与内核信息统一格式化为：`Ubuntu 24.04.1 LTS (Linux 6.6.0-43-generic x86_64)`；
   - 增加 128 字符安全截断，防止超出数据库 `os_info` 列长度约束。
4. **硬件规格探测与 Docker 初始化**：
   - 采集 `nproc`（CPU 核心数）与 `grep MemTotal /proc/meminfo`（物理内存总量）；
   - 自动检测 Docker 运行时，并在开启 `autoInstallDocker` 时自动执行静默安装并配置 2375 远程通信端口。

---

### 2.5 前端表单与类型严格对齐 (`volur-lite-view`)

1. **`src/types/cluster/index.ts`**：
   - 将 `sshPort` 与 `sshUser` 从可选调整为必填字段，并扩展 `sshKey?: string`。
2. **`AdminClusterSettingsView.vue`**：
   - 新增认证方式切换单选组件（`NRadioGroup` + `NRadioButton`），支持管理员切换“密码认证”与“私钥认证”；
   - 切换为密码认证时展示密码输入框，切换为私钥认证时展示私钥多行文本域；
   - 表单校验规则严格与后端 DTO 对齐；
   - 资源节点开关在角色为 Worker 时自动禁用并展示专属提示文案。

---

## 3. 影响范围与验证结果

1. **涉及修改文件**：
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/entity/ClusterNode.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/ClusterNodeRepository.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterNodeCreateDTO.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ClusterNodeMapper.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java`
   - `src/volur-lite-view/src/types/cluster/index.ts`
   - `src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue`
2. **编译与验证**：
   - 后端全部 12 个模块通过 `mvn compile` 编译，`BUILD SUCCESS`；
   - 前端通过 `vue-tsc --noEmit && vite build` 生产构建，0 错误 0 告警。
