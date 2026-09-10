# 集群节点修改、资源节点设定与存活测试弹窗聚合改造审计记录

## 1. 变更背景与需求概述

在平台原有的集群管理界面中：
1. 管理员无法对已加入集群的节点信息进行编辑（例如修改节点的 SSH 端口、登录用户名、密码或私钥凭据等）；
2. 节点列表的操作栏并排堆叠了“存活检测”、“设为资源节点”、“同步镜像”、“移除节点”等多个按钮，界面杂乱且缺乏安全聚焦的操作交互；
3. 节点存活测试缺乏针对新修改凭据的即时联调验证能力。

为此，本次重构完成了以下核心升级：
- **节点编辑功能（`updateNode`）**：支持对集群节点的标识名称、SSH 端口、用户名、登录密码/私钥、节点角色以及资源节点属性进行完整配置；
- **资源节点调度收敛至修改弹窗**：将原表格操作栏的“设为资源节点”移动至“修改节点”弹窗中统一维护，且保持“Worker 节点自动作为资源节点，只在纯 Master 集群下由 Master 承担资源节点”的架构约束；
- **存活测试收敛至修改弹窗（`testNodeConnection`）**：在修改节点弹窗内提供“执行存活测试”功能，支持传入弹窗内当前编辑的凭据与网络端口进行即时握手连通性与 Docker 运行状态探测，返回往返时延（Latency ms）与环境版本；
- **前后端全链路严格对齐**：Controller、Service、DTO/VO、前端类型定义、API 调用层、i18n 多语言及 Naive UI 视图全面遵循 `Rules.md` 与 `view-Rules.md` 规范。

---

## 2. 核心架构设计与改动明细

### 2.1 后端工程改造 (Java 25 / Spring Boot 3 / volur-container)

#### 2.1.1 新增修改节点请求入参 DTO (`AdminClusterNodeUpdateDTO.java`)
位于 `com.humolix.volur.container.domain.dto.AdminClusterNodeUpdateDTO`：
- `nodeName`: `@NotBlank`, `@Size(min = 2, max = 64)`；
- `sshPort`: `@NotNull`, `@Min(1)`, `@Max(65535)`，默认 22；
- `sshUser`: `@NotBlank`, `@Size(min = 1, max = 64)`，默认 "root"；
- `sshPassword`: 选填，留空表示不修改已有密码；
- `sshKey`: 选填，留空表示不修改已有私钥；
- `role`: `@NotBlank`，"MASTER" 或 "WORKER"；
- `isResourceNode`: 选填，是否指定为题目资源节点。

#### 2.1.2 服务层业务逻辑实现 (`AdminClusterServiceImpl.java`)
在 `AdminClusterService` 接口与其实现类中新增并完善：
1. **`updateNode(Long id, AdminClusterNodeUpdateDTO request)`**：
   - 目标节点存在性校验（不存在抛出 404）；
   - IP 地址不可变更（IP 是节点的唯一网络与拓扑身份标识）；
   - 密码/私钥更新逻辑：若传入非空内容，则更新对应凭据；若两者均有变更，进行 SSH 连通性测试以确保新凭据可用；
   - 角色与拓扑隔离约束：若节点被指定为 `WORKER`，自动设为 `isResourceNode = true`，并触发拓扑对齐使所有 `MASTER` 节点的 `isResourceNode` 设为 `false`；
   - 保存实体并返回规范的 `AdminClusterNodeVO`。
2. **`testNodeConnection(Long id, AdminClusterNodeUpdateDTO request)`**：
   - 提取待测试的目标 IP 与端口/凭据（若请求体中有传入新编辑的凭据则优先使用新凭据，未传入则回退读取数据库中原有凭据）；
   - **四级网络探测**：执行 `checkIpReachable(targetIp)`，验证主机 IP 基础可达性；
   - **SSH 认证连通握手**：调用 JSch 建立测试会话，精准捕获认证失败或超时；
   - **Docker 运行状态探针**：在 SSH 通道中执行 `docker --version`，采集 Docker Engine 版本；
   - 统计往返网络耗时 `latencyMs`，装配并返回 `AdminContainerTestConnectionVO`。

#### 2.1.3 控制器路由暴露 (`AdminClusterController.java`)
遵循 Thin Controller 原则，类注解 `@RequestMapping("/api/v1/admin/clusters")`：
- `PUT /nodes/{id}`：调用 `adminClusterService.updateNode(id, request)`，统一使用 `ApiResponseEntity.ok(...)` 返回 `ApiResponse<AdminClusterNodeVO>`；
- `POST /nodes/{id}/test-connection`：调用 `adminClusterService.testNodeConnection(id, request)`，统一使用 `ApiResponseEntity.ok(...)` 返回 `ApiResponse<AdminContainerTestConnectionVO>`。

---

### 2.2 前端工程改造 (Vue 3 / TypeScript / volur-lite-view)

#### 2.2.1 实体与 API 请求层 1:1 对齐
1. **`src/types/cluster/index.ts`**：
   - 新增 `AdminClusterNodeUpdateDTO` 接口，字段名与后端 DTO 100% 映射（小驼峰命名，杜绝 `any`）。
2. **`src/api/cluster/index.ts`**：
   - 新增 `updateAdminClusterNodeApi(id, data)`，指向 `PUT /admin/clusters/nodes/${id}`；
   - 新增 `testAdminClusterNodeConnectionApi(id, data)`，指向 `POST /admin/clusters/nodes/${id}/test-connection`。

#### 2.2.2 国际化语言包对齐
在 `src/locales/zh_CN/admin/clusters.json` 与 `src/locales/en_US/admin/clusters.json` 中：
- `nodes.edit`: "修改节点" / "Edit Node"；
- `edit_modal`: 增加完整的弹窗标题、副标题、表单项描述、凭据占位符、存活测试文案及成功提示。

#### 2.2.3 界面与交互重构 (`AdminClusterSettingsView.vue`)
1. **表格操作栏精简**：
   - 移除原行内的“存活检测”和“设为资源节点”按钮；
   - 操作列宽度从 `320px` 收窄为 `240px`，保留：
     - `修改`（`CreateOutline` 图标，点击唤起修改节点弹窗）；
     - `同步题目镜像`（`syncImagesToNode`）；
     - `移除节点`（`NPopconfirm` 二次确认弹窗）。
2. **轻量化主视图与弹窗职责解耦**：
   - 将添加节点与修改节点的卡片弹窗完整抽取为独立组件 `ClusterNodeModal.vue`，主视图仅通过 `nodeModalVisible`、`nodeModalMode` 和 `selectedNode` 进行控制，大幅精简主视图冗余代码（减少约 500 行模板与逻辑）。

#### 2.2.4 弹窗组件独立抽取与布局扁平化重构 (`components/ClusterNodeModal.vue`)
1. **组件封装与复用 (Unified Modal Component)**：
   - 单一组件同时承载“扩展集群 / 添加节点”(`mode='add'`)与“修改集群节点”(`mode='edit'`)两种模式；
   - 依据模式动态切换标题、校验规则（编辑时 IP 不可变更，密码与私钥选填留空保持不变；添加时必填）、提交 API 及操作按钮文字。
2. **去除 SSH 独立分界与表单流线型扁平化**：
   - 取消原有的“SSH 节点连接与自动化纳管”独立分隔线标题；
   - **紧密排布**：将“SSH 用户名”与“凭据认证方式”直接放置在“节点 IP 地址与端口”下方，使网络地址与登录凭据形成自然的上下文呼应，降低界面视觉断层。
3. **私钥认证采用文件导入方式 (`NUpload` & `FileReader`)**：
   - 当选择“私钥认证”时，废除手写大段文本的原始输入方式，全面采用文件选择与拖拽导入交互；
   - **拖拽/点击导入区**：通过 `<n-upload :show-file-list="false" :default-upload="false">` 提供拖拽区域，支持 `.pem`、`.key`、`id_rsa`、`id_ed25519` 等文本私钥文件；
   - **内容异步载入与格式校验**：利用浏览器原生 `File.text()` 读取密钥文本至 `form.sshKey`，核对非空并自动校验 `PRIVATE KEY` 标准格式；
   - **已载入卡片与状态感知**：私钥读取成功后展示绿意安全徽章卡片，呈现已载入文件名、文件体积（KB），并提供“重新导入”与“清空”操作；
   - **编辑模式防呆引导**：在修改已有节点时，若未导入新文件，提供“继续保持原有私钥凭据不变”的友好状态感知。
4. **存活测试区域与资源节点开关联动**：
   - 仅在修改模式下渲染“执行存活测试”黄色次级按钮，通过 JSch 真实握手并反馈毫秒耗时与 Docker 引擎版本；
   - 角色为 `WORKER` 时，资源节点开关自动锁定勾选并呈现专属业务提示。

---

### 2.3 本机 Master 平台控制面安全保护与异常处理专项

1. **Master 节点禁止修改与禁止存活测试设计**：
   - **架构背景**：Master 节点为部署 Volur-Lite 平台的本机主机本身，平台与 Master 节点的容器引擎通信直接基于本机的本地 Docker Daemon（`/var/run/docker.sock` 或 `localhost:2375`）或 K3s 本机 API，而非外部 SSH 隧道；因此 Master 节点的基础参数收敛在“集群参数与调度设置”中维护，不属于集群子节点管理范畴；
   - **前端交互保护**：
     - 在 `AdminClusterSettingsView.vue` 中，当 `row.role === 'MASTER'` 时，操作栏不再展示任何按钮，直接渲染 `NTag` 徽章：`本机控制面 (不可修改)`；
     - 在 `openEditNodeModal`、`handleRemoveNode`、`handleSyncImagesToNode` 中增加防呆逻辑拦截，阻止任何途径触发针对 Master 的变更动作；
     - 在 `ClusterNodeModal.vue` 中，将角色选择列表中的 `MASTER` 项置为 `disabled: true` 并标注 `(仅限本机平台控制面)`，确保新增节点只能为 Worker 扩展节点。
   - **后端接口防线**：
     - `AdminClusterServiceImpl.updateNode`：核查 `if ("MASTER".equalsIgnoreCase(node.getRole()))` 直接抛出 400 业务异常（`"主控制节点为本机平台控制面，禁止修改"`）；且禁止将 Worker 节点角色更改为 `MASTER`；
     - `AdminClusterServiceImpl.testNodeConnection`：核查 `if ("MASTER".equalsIgnoreCase(node.getRole()))` 直接抛出 400 业务异常（`"主控制节点为本机平台控制面，无需且不支持独立存活性测试"`）；
     - `AdminClusterServiceImpl.removeNode`：严格杜绝移除 Master 节点。
2. **SSH 连接 `SocketException: Network is down` 异常友好兜底**：
   - 在 `verifySshConnectivity` 中补充对 `Network is down`、`Network is unreachable` 以及 `No route to host` 等底层套接字网络接口状态异常的语义化转换，提示用户：“目标网络不可达或网络接口未启用 (详细底层原因)”，避免向前端直接暴露裸错误栈。

---

## 3. 影响范围与验证结果

1. **涉及修改文件清单**：
   - 后端：
     - [`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterNodeUpdateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterNodeUpdateDTO.java)
     - [`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/AdminClusterService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/AdminClusterService.java)
     - [`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
     - [`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/admin/AdminClusterController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/admin/AdminClusterController.java)
   - 前端：
     - [`src/volur-lite-view/src/types/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/cluster/index.ts)
     - [`src/volur-lite-view/src/api/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/cluster/index.ts)
     - [`src/volur-lite-view/src/locales/zh_CN/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/clusters.json)
     - [`src/volur-lite-view/src/locales/en_US/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/clusters.json)
     - [`src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue)
     - [`src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue)
2. **编译构建核验**：
   - **后端**：在 `volur-lite-platform` 下执行 `mvn compile`，12 个模块全部编译通过，`BUILD SUCCESS`；
   - **前端**：在 `volur-lite-view` 下执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 错误 0 告警，生产 Chunk 打包顺利完成。
