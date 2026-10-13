# Docker 镜像拉取全链路生命周期、Registry 契约与跨节点复制 Stream Closed 异常根因诊断与架构分析

## 1. 现象背景复盘

在 Volur 平台管理后台进行镜像管理与容器调度操作时，出现以下关联现象：
1. **界面交互修改**：`AdminImageView.vue` 中的“拉取新镜像”操作按钮文案需精简统一为“拉取镜像”；
2. **异步任务与镜像管理列表状态不一致**：在后台异步任务中查看镜像拉取已 100% 完成，但在镜像管理页面列表中未出现该镜像；
3. **宿主机 Docker 存在打标镜像**：在宿主机上执行 Docker 指令，能看到已打标的 `10.145.251.25:5000/nginx:latest` 镜像；
4. **重新扫描镜像后依然无显示**：管理员点击“重新扫描镜像”后，不仅没有出现该镜像，此前写入的记录反被清理；
5. **体积显示为 0B**：在拉取任务完成后，若镜像被列表查询到，其展示的存储占用体积始终为 `0B`；
6. **第二次拉取才展示且体积仍为 0B**：第二次再次触发拉取后，镜像列表短暂显示出了该镜像，但体积仍为 `0B`；
7. **启动容器报错跨节点复制异常**：使用该体积为 `0B` 的镜像在 `AdminContainerLanuchModal.vue` 中启动容器时，后端抛出 `跨节点复制镜像异常: Stream closed`。

---

## 2. 后端核心执行流深度逆向与因果链分析

### 2.1 镜像拉取与中心入库逻辑 (`AdminImageServiceImpl.pullImage`)

在管理员调用 `POST /api/v1/admin/images/pull` 时：
1. **预落库与状态标记**：
   - 系统将镜像解析为 `repository` (`nginx`) 与 `tag` (`latest`)；
   - 在数据库中创建或更新一条 `ClusterImage` 记录，初始状态为 `status = "PULLING"`，**`inRegistry = false`**，**`sizeBytes = 0L`**；
   - 提交后台异步任务 `adminAsyncTaskService.submitTask` 并返回任务 ID。
2. **异步执行阶段（主控节点拉取与本地打标）**：
   - 后端在主控节点执行 `executeDockerPull(masterIp, rawImage, ...)`，从公共 Registry（Docker Hub 等）将 `nginx:latest` 拉取到宿主机本地 Docker 引擎缓存；
   - 系统读取动态配置中的 `system.container.public_host` (`10.145.251.25`) 与端口 `5000`，拼装私有 Registry 仓库标签 `10.145.251.25:5000/nginx:latest`；
   - 执行 `tagDockerImageOnNode`，将本地镜像打标为该完整路径。因此宿主机 Docker 引擎中能看到 `10.145.251.25:5000/nginx:latest`。
3. **推送私有 Registry 与 Manifest 解析缺陷 (Zero-Mock 违背与静默吞错)**：
   - 随后执行 `triggerDockerPushOnNode(masterIp, registryTag)`：
     ```java
     boolean pushed = triggerDockerPushOnNode(masterIp, registryTag);
     if (!pushed) {
         log.warn("[AdminImageService] 推送镜像至中心私有 Registry 失败: {}", registryTag);
     }
     ```
   - **关键根因 1**：Docker 引擎推送镜像至 `10.145.251.25:5000` 属于 HTTP 协议明文通信，若宿主机 Docker 守护进程未配置 `insecure-registries: ["10.145.251.25:5000"]`，Docker 会默认以 HTTPS 握手失败而拒绝推送（返回 `server gave HTTP response to HTTPS client`，exitCode 1）；
   - **关键根因 2**：`triggerDockerPushOnNode` 在推送失败时**仅打印了 WARN 日志，未抛出异常阻断任务**；
   - 紧接着后端调用 `dockerRegistryClient.getManifest(repository, tag)`。由于镜像实际未成功进入私有 Registry（或 Registry 容器未正常提供 V2 API 服务），该请求返回 `null`，导致局部变量 `sizeBytes` 与 `digest` 保持默认初始值（`0L` 与 `""`）；
   - 任务依然继续推进至 `context.updateProgress(100, ...)`，并在末尾将数据库实体的 `inRegistry` 强行设为 `true`、`status = "READY"`，但其 `sizeBytes` 依然为 `0`。

---

### 2.2 为什么第一次拉取完成后列表无显示，且点击“重新扫描镜像”也无显示？

1. **列表过滤规则强约束**：
   - 镜像管理列表接口 `listImages` 包含硬编码查询断言：
     ```java
     predicates.add(cb.isTrue(root.get("inRegistry")));
     ```
   - 在异步任务尚未执行完毕（处于 `inRegistry = false`）时，前端列表绝对无法查出该镜像。
2. **“重新扫描镜像”(`loadAndSyncClusterImages`) 的对账清理机制**：
   - 当用户在界面点击【重新扫描镜像】时，系统调用 `dockerRegistryClient.listRepositories()` 遍历私有 Registry 的 `_catalog`；
   - 后续执行对账核验清理逻辑：
     ```java
     for (ClusterImage dbImg : allDbImages) {
         String key = dbImg.getRepository() + ":" + dbImg.getTag();
         boolean inRegistry = registryDiscoveredKeys.contains(key);
         if (!inRegistry || !Boolean.TRUE.equals(dbImg.getInRegistry())) {
             clusterImageRepository.delete(dbImg);
         }
     }
     ```
   - **关键根因 3**：因为此前的 `docker push` 失败，中心 Registry 的 `_catalog` 实际上根本没有 `nginx` 镜像。扫描比对时，该镜像未命中 `registryDiscoveredKeys`，**被当作“无效宿主机/孤儿镜像”直接从 MySQL 物理删除 (`delete`)**！
   - 这导致第一次拉取任务写入的元数据被重新扫描彻底抹除，列表再次刷新后空空如也。

---

### 2.3 为什么第二次拉取后显示出来，但体积仍为 0B？

1. 用户第二次提交拉取任务后，异步任务重新执行，并在任务最后将数据库中该记录设为 `inRegistry = true`；
2. **用户在第二次拉取后未再次点击“重新扫描镜像”**，因此未触发孤儿清理逻辑，数据库中暂时保留了该记录；
3. 由于满足 `inRegistry = true`，管理端列表成功展示该镜像；
4. 但因推送依然未入库 Registry，未能获取到 Manifest 中的分层体积（`sizeBytes` 依然是默认值 `0L`），故前端格式化展示为 `0B`。

---

### 2.4 为什么启动容器报错 `跨节点复制镜像异常: Stream closed`？

1. **调度与镜像就绪检测 (`ensureImageOnNode`)**：
   - 在 `AdminContainerLanuchModal.vue` 中选择镜像启动时，后端解析出 `resolvedImageName = "nginx:latest"`；
   - 调用 `adminImageService.ensureImageOnNode(targetNodeIp, "nginx:latest")`；
   - 目标节点（如工作节点或指定节点）本地尚未缓存该镜像，且中心 Registry 根本不存在该镜像（从 Registry pull 失败进入 catch 块）；
2. **回退至跨节点流式复制 (`replicateImageToNode`)**：
   - 后端进入回退机制，尝试在持有镜像的源节点（宿主机/主节点）执行 `docker save`，通过 Java 管道流实时直通目标节点的 `docker load`：
     ```java
     List<String> saveCmd = buildDockerBaseCommand(sourceNodeIp);
     saveCmd.add("save");
     saveCmd.add(safeImageName); // 此时为 nginx:latest

     List<String> loadCmd = buildDockerBaseCommand(targetNodeIp);
     loadCmd.add("load");

     try (InputStream is = saveProcess.getInputStream();
          OutputStream os = loadProcess.getOutputStream()) {
         is.transferTo(os);
         os.flush();
     }
     ```
3. **Stream Closed 异常触发链**：
   - **子进程异常中断导致管道破裂**：
     - 源节点本地 Docker 引擎中被打标为 `10.145.251.25:5000/nginx:latest`，如果 `docker save nginx:latest` 执行时找不到原标签或者执行出错，`saveProcess` 瞬间退出并关闭标准输出流；
     - 或者目标节点 Docker CLI 由于远程 Daemon 端口（2375/2376）网络不可达、认证拒绝，`loadProcess` 启动失败并立即退出，关闭了标准输入流；
   - Java 在执行 `is.transferTo(os)` 时，检测到底层 `Process` 的 I/O 流已非正常关闭，抛出 `java.io.IOException: Stream closed`；
   - 外层捕获为 `BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "跨节点复制镜像异常: Stream closed")`，并呈现给前端。

---

## 3. 架构优化与防御修复建议

1. **镜像推送全链路快速失败 (Fail-Fast 原则)**：
   - `pullImage` 任务中若 `triggerDockerPushOnNode` 返回 `false`，必须记录详细 CLI stderr 并直接抛出业务异常快速失败，严禁伪造进度推进至 100%；
2. **宿主机 Docker 守护进程私有仓库放行**：
   - 在宿主机 `/etc/docker/daemon.json` 中配置：
     ```json
     {
       "insecure-registries": ["10.145.251.25:5000"]
     }
     ```
     并重启 Docker 守护进程 (`systemctl restart docker`)；
3. **镜像本地体积双重解析兜底**：
   - 当从 Registry 获取 Manifest 失败或未入库时，降级通过本地 Docker 引擎执行 `docker image inspect <image>` 获取 `Size` 字段，杜绝直接暴露 `0B` 脏数据；
4. **跨节点管道复制进程防护与错误输出捕获**：
   - 在 `replicateImageToNode` 中，在管道传输前应先校验两端 Docker 连通性，并在 `save/load` 异常退出时先读取其 `stderr` 详细报错，而不是笼统报 `Stream closed`。

---

## 4. 代码修复实施落地复盘

1. **`AdminImageView.vue` 文案精简**：
   - 将操作按钮文案由“拉取新镜像”修改为“拉取镜像”。
2. **`DockerRegistryClient` 探测与地址解析多级回退**：
   - 支持候选地址自适应探测（内部服务名 -> 公共宿主机配置 `system.container.public_host:5000` -> 本地 `127.0.0.1:5000`），并缓存首个连通成功的活动基地址，消除因网络拓扑差异导致的假离线。
3. **`pullImage` 任务 Fail-Fast 与体积双重兜底**：
   - `triggerDockerPushOnNode` 重构为 `executeDockerPushWithOutput`，实时捕获进程完整输出；推送失败时抛出带有明细与 `insecure-registries` 提示的业务异常，杜绝虚假 100% 进度；
   - Manifest 体积解析缺失时，自动调用本地 `docker inspect` 读取字节大小并回填，杜绝 `0B` 脏数据落库。
4. **`loadAndSyncClusterImages` 防误删保护**：
   - 若 Registry Catalog 返回为空，禁止触发孤儿镜像删除逻辑；
   - 排除 `PULLING` 状态镜像，并在物理删除前核验宿主机本地是否存在该镜像（含带私有仓库前缀标签），保护合法镜像元数据不被误删。
5. **`ensureImageOnNode` 与 `replicateImageToNode` 管道强化**：
   - 目标节点检测时，若本地已存在带私有仓库前缀的标签镜像，自动在本地重新打标就地就绪，规避无效网络传输；
   - 源节点导出前智能识别本地实际持有的标签版本，并在目标节点导入后自动补齐原镜像名称；
   - 跨节点流式直通若发生 I/O 管道破裂，异步等待并汇聚子进程标准错误流，精准暴露底层 Docker 错误详情。
6. **K3S 运行模式与 Docker 跨节点复制彻底解耦**：
   - 在 `AdminContainerServiceImpl` 与 `CtfInstanceServiceImpl` 中，将 `ensureImageOnNode` 限制在 `DOCKER_SWARM` 模式下触发；在 `K3S` 模式下直接交由 Kubernetes Pod 调度机制拉取镜像，彻底杜绝 K3S 节点因缺少 Docker 2375 端口而触发 `Stream closed` 异常。
7. **K3S 镜像私有仓库前缀智能解析 (`resolveEffectiveImageName`)**：
   - 在 `K3sRuntimeStrategy` 中自动识别平台托管的私有 Registry 镜像，调度 Pod 时若未携带私有仓库主机名前缀（如输入 `nginx:latest`），自动装配为中心仓库绝对路径（如 `10.145.251.25:5000/nginx:latest`），阻止 containerd 错误地向外网 Docker Hub 发起拉取导致 `ImagePullBackOff`。
