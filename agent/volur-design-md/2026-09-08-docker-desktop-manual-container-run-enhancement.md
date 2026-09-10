# 容器管理手动启动容器功能升级：对齐 Docker Desktop 体验与双引擎编排扩展

## 1. 变更背景与设计动机

在 Volur Lite 容器与动态靶机管理体系中，原有的“手动启动容器”功能仅支持关联已配置动态环境的 CTF 题目（传入 `challengeId`），无法让管理员像使用 **Docker Desktop** 那样自由基于本地或远程 Docker 镜像启动独立的调试容器、评估容器或沙箱环境。

为赋予管理员更灵活高效的容器与靶机调度能力，本次变更全面参考 Docker Desktop 的 **"Run a new container"** 运行体验，升级了前后端调度逻辑与数据模型：
1. **独立镜像启动支持**：支持直接从集群镜像列表选择或任意输入自定义镜像标签（如 `python:3.10-alpine`、`nginx:alpine`、`ctf-web:v1`），脱离强依赖 `challengeId` 的硬性约束；
2. **容器名称自定义 (Container Name)**：支持管理员指定人性化容器服务名称，未指定时遵循系统默认规范自动生成；
3. **多端口映射配置 (Ports)**：支持动态增删多组端口映射，可自定义 `Host Port`（主机暴露端口，留空自动分配）、`Container Port`（容器内部服务端口）以及传输协议（`TCP` / `UDP`）；
4. **数据卷目录挂载 (Volumes / Bind Mounts)**：支持动态增删宿主机路径与容器内部路径的挂载映射，支持读写模式（`rw` / `ro`）；
5. **自定义环境变量注入 (Environment Variables)**：支持动态注入任意 `Key=Value` 环境变量，同时保留 CTF 动态 Flag（`FLAG` / `GZCTF_FLAG`）注入通道；
6. **精细化资源限制 (Resource Limits)**：支持配置 CPU 核数限制与内存容量配额（MB）。

---

## 2. 领域模型与实体改造

### 2.1 基础 DTO 扩展 (`volur-container`)
按照 `Rules.md` 规范，所有模型集中收敛于 `com.humolix.volur.container.domain`：
- **`ContainerPortMappingDTO`**：端口映射入参（`hostPort`, `containerPort`, `protocol`）；
- **`ContainerVolumeMappingDTO`**：数据卷绑定入参（`hostPath`, `containerPath`, `mode`）；
- **`ContainerEnvVarDTO`**：环境变量键值对入参（`name`, `value`）；
- **`AdminContainerManualStartDTO`**：
  - `challengeId` 设为可选（`@NotNull` 移除）；
  - 新增 `imageName`, `containerName`, `ports`, `volumes`, `environments`, `cpuLimit`, `memoryLimit` 字段；
  - 遵循 DTO 规范使用 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`。

### 2.2 持久化实体改造 (`CtfChallengeInstance`)
- 将 `@JoinColumn(name = "challenge_id", nullable = true)` 改为允许为空，以支持独立镜像容器；
- 新增持久化列：
  - `image_name` (`VARCHAR(255)`)：运行的镜像名称；
  - `container_name` (`VARCHAR(128)`)：自定义容器名称；
  - `ports` (`VARCHAR(512)`)：端口映射摘要字符串（如 `20080:80/TCP, 20081:443/TCP`）；
  - `volumes` (`VARCHAR(1024)`)：数据卷挂载摘要；
  - `env_vars` (`VARCHAR(2048)`)：环境变量摘要。

### 2.3 视图对象与对象映射器 (`AdminContainerInstanceVO` & `ContainerMapper`)
- `AdminContainerInstanceVO` 新增 `imageName`, `containerName`, `ports`, `volumes`, `envVars` 字段展示；
- `ContainerMapper`：
  - 为防止独立容器在 `challenge == null` 时出现 NPE 或标题空白，采用 `resolveChallengeTitle` 与 `resolveChallengeCategory` 回退方法，在题目为空时安全使用 `containerName`、`imageName` 或 `"独立容器"`，分类回退为 `"独立镜像"`。

---

## 3. 双编排引擎生命周期调度实现

### 3.1 Docker Swarm 调度实现 (`DockerSwarmRuntimeStrategy`)
在 `startManualContainer` 中实现原生 CLI 命令组装：
1. **端口映射**：组装 `--publish <hostPort>:<containerPort>/<protocol>`；
2. **数据卷绑定**：组装 `--mount type=bind,source=<hostPath>,target=<containerPath>[,readonly]`；
3. **环境变量注入**：组装 `--env <key>=<value>`；
4. **资源限制**：组装 `--limit-cpu <cpu>` 与 `--limit-memory <mem>M`；
5. **智能工作节点约束**：仅当检测到真实 `WORKER` 节点存在时注入 `--constraint node.role == worker`，避免单节点 Swarm 集群因调度约束导致服务永久挂起；
6. **容错与回退**：真实 Docker 异常或超时（15s）时自动降级至沙箱高可用仿真模式。

### 3.2 K3s / Kubernetes 调度实现 (`K3sRuntimeStrategy`)
在 `startManualContainer` 中利用 Fabric8 Kubernetes Client 构建声明式模型：
1. **Pod 容器端口与 Service 端口**：
   - 为 Pod 构建 `ContainerPort` 列表；
   - 为 NodePort Service 构建 `ServicePort` 列表，智能提取分配的主机 NodePort 作为对外访问入口；
2. **HostPath 卷与挂载**：
   - 构建 `Volume`（`HostPathVolumeSource`）与 `VolumeMount`；
3. **环境变量与资源配额**：
   - 构建 `EnvVar` 列表并添加 Flag 注入；
   - 构建 `ResourceRequirements`（CPU 与内存 Limits / Requests）；
4. **工作节点亲和**：仅当存在工作节点时添加 `node-role.kubernetes.io/worker` 标签选择器。

### 3.3 JPA 复杂查询安全防坑 (`AdminContainerServiceImpl`)
在 `buildSpecification` 模糊查询中：
- 原代码使用 `root.join("challenge")`（默认 `INNER JOIN`），导致所有 `challenge_id IS NULL` 的独立容器在带关键字过滤时被数据库自动剔除；
- 修复为：`root.join("challenge", JoinType.LEFT)`，并追加 `imageName` 与 `containerName` 的模糊匹配条件。

---

## 4. 前端交互与视觉规范实现 (`volur-lite-view`)

### 4.1 模态框重构 (Docker Desktop 风格)
- 弹窗宽度扩展至 `760px`，划分为层次分明的配置卡片：
  - **基础配置**：镜像选择器（支持从集群镜像下拉选择，支持自输入 tag 标签）、容器名称、关联赛题 ID；
  - **端口映射 (Ports)**：动态多行输入，包含主机端口、容器端口、协议（TCP/UDP）、删除行与新增行；
  - **数据卷挂载 (Volumes)**：动态多行输入，包含宿主机路径、容器内部路径、读写权限模式（rw/ro）；
  - **环境变量 (Environment Variables)**：动态多行输入，包含 Key、Value；
  - **高级选项折叠面板**：CPU 配额、内存容量、存活时长、自定义 Flag、绑定用户 ID。

### 4.2 视觉与技术栈遵循 (`view-Rules.md`)
- 亮色主要操作按钮背景强制 `#000000`，文字 `#ffffff`（`.theme-primary-btn`）；
- 描述性副标题、标签及辅助提示统一为 `#444444`；
- 多语言完备：更新 `zh_CN` 与 `en_US` 的 `admin/containers.json` 翻译字段；
- TypeScript 严格强类型校验，杜绝 `any` 侵入。

---

## 5. 验证与构建审计

1. **后端构建校验**：
   - 执行 `mvn test-compile -DskipTests`：Reactor 12 个模块全量编译通过 (`BUILD SUCCESS`)。
   - 执行 `mvn test -pl volur-container`：单元测试全部通过（6 tests run, 0 failures）。
2. **前端构建校验**：
   - 执行 `npm run build`：`vue-tsc --noEmit` 0 错误通过，Vite 4264 modules 成功构建出生产 `dist` 包。
