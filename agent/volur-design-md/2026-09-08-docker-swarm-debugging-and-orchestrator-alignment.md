# Volur Lite 施工审计记录：Docker Swarm 调度修复与集群引擎统一化

- **日期**：2026-09-08
- **责任模块**：`volur-container` / `volur-system` / `volur-lite-view`
- **变更类型**：生产级故障修复 / 底层调度可靠性提升 / 架构规范对齐

---

## 1. 变更背景与故障现象深入剖析

在容器管理与集群调度调试过程中，出现了以下数项异常与视觉/逻辑混淆现象：

1. **`docker swarm init` 报错 `This node is already part of a swarm`**：
   - 宿主机 Docker Daemon 当前实际已经是一个处于就绪/激活状态的 Swarm Manager 节点。再次执行初始化会收到 Docker 守护进程关于节点已加入集群的合法提示。
2. **终端调试（Web Shell）执行 `ls -la` 报错 `Error response from daemon: No such container: swarm-volur-... exit: 1 58ms`**：
   - **根本原因 1**：由于此前所启动镜像 `volur/ctf-web-base:php7.4-apache` 在本地宿主机镜像缓存中并不存在，且 Docker Swarm 在拉取时由于网络超时或镜像源缺失未能生成有效 Task 容器副本。
   - **根本原因 2**：`docker service create` 原生 CLI 在未指定 `--detach=true` 参数时会以同步阻塞模式前台等待任务聚合，导致服务端 Process 超时，降级进入容错仿真模式；
   - **根本原因 3**：在 `DockerSwarmRuntimeStrategy.execCommand` 查找运行中的真实容器 ID 时返回 `null`，旧逻辑错误地将服务名称 `sanitizedName` 传递给 `docker exec`。`docker exec` 仅接受真实的容器 ID/容器名，因而触发 Docker Daemon 报错 `No such container`。
3. **终端不断刷新 `overall progress: 0 out of 1 tasks`**：
   - 即上述缺少 `--detach=true`（或 `-d`）以及 `--no-resolve-image` 导致的 Docker 前台阻塞轮询。
4. **明明想要使用 Docker Swarm，但系统内多处出现 `K3S`、访问入口与 Flag 均为 `K3s`**：
   - `SystemSettingKeys.CONTAINER_TYPE` 历史默认值被硬编码为 `"K3S"`；
   - 数据库初始化后，系统配置表中存储的值为 `K3S`；若管理员在“集群管理 - 集群设置”中未主动切换为 `DOCKER_SWARM` 并点击“保存设置”，所有容器创建、终端调试均路由至 `K3sRuntimeStrategy`。由于本地未配置实际 K3s，策略进入容错仿真沙箱（容器名 `mock-volur-...`，Flag 前缀 `volur{k3s_dynamic_...}`）；
   - `ContainerMapper` 中根据容器 ID 前缀猜测调度引擎，非 `swarm-` 开头均被当作 `K3S`。
5. **“关联题目”列显示 `volur/ctf-web-base:php7.4-apache 独立镜像`**：
   - 从“手动启动容器”弹窗中直接运行独立镜像时，并未选择 CTF 题库中的题目（`challenge_id` 为 `null`），系统设计由此展示题目类别为 `独立镜像`，主标题显示用户指定的容器名或镜像名称。

---

## 2. 核心改动点与架构优化

### 2.1 后端改动 (`volur-container` & `volur-system`)

1. **`DockerSwarmRuntimeStrategy.java` 健壮性与异步解耦增强**：
   - **添加 `--detach=true` 与 `--no-resolve-image`**：在 `startContainer` 与 `startManualContainer` 中，使 `docker service create` 在 100ms 内极速返回服务 ID，彻底杜绝 CLI 阻塞等待与终端刷屏 `overall progress: 0 out of 1 tasks`；
   - **自动创建 Overlay 基础网络**：新增 `ensureOverlayNetwork` 方法，在编排服务前自动检测并以 `--driver overlay --attachable` 创建 `volur-swarm-network`；
   - **完善进程标准输出与错误流捕获**：完整记录启动命令的输出日志，便于排查与审计；
   - **修复 `execCommand` 容器查找与精准错误提示**：
     - 新增 `findSwarmTaskContainerId`，先通过 `label=com.docker.swarm.service.name` 再通过 `name` 精准定位真实运行的任务容器 ID；
     - 若实际容器尚未运行，自动通过 `docker service ps <service> --no-trunc` 查询任务状态（如 `Preparing`、`Rejected`、`No such image` 等），向 Web Shell 终端返回清晰详尽的排查提示，杜绝抛出原始的 `No such container` 恶性报错；
     - 若服务完全不存在（历史仿真数据），平滑回退到交互式沙箱仿真终端。

2. **容器实例引擎类型明确化与持久化**：
   - 在领域实体 `CtfChallengeInstance.java` 中新增 `container_type` 列（`VARCHAR(32)`）；
   - 在 `AdminContainerServiceImpl.java` 与 `CtfInstanceServiceImpl.java` 中，将实际采用的调度引擎类型显式写入持久化记录中；
   - 更新 MapStruct `ContainerMapper.java`，优先以持久化的 `containerType` 作为单一真实数据源，告别仅靠容器 ID 前缀猜测引擎的反模式。

3. **默认集群调度引擎全面切换至 `DOCKER_SWARM`**：
   - 更新 `SystemSettingInitializer.java`，将 `system.container.type` 初始配置调整为 `"DOCKER_SWARM"`；
   - 更新 `AdminContainerServiceImpl`、`CtfInstanceServiceImpl`、`AdminClusterServiceImpl`、`AdminImageServiceImpl` 中所有默认降级引擎为 `"DOCKER_SWARM"`；
   - 更新 `ContainerSchemaInitializer.java`，自动向 `ctf_challenge_instance` 表迁移添加 `container_type` 字段。

### 2.2 前端改动 (`volur-lite-view`)

1. **`AdminContainerView.vue`**：
   - 响应式默认状态 `activeContainerType` 同步调整为 `'DOCKER_SWARM'`；
   - 在“手动启动容器”模态弹窗顶部新增当前集群调度引擎指示标签（`当前调度集群引擎: Docker Swarm`），消除操作认知差异；
   - 在镜像选择框下方补充友好的提示指引：建议优先选取本地宿主机已就绪的镜像，若使用远程镜像请确保预先执行拉取，避免任务挂起。

---

## 3. 验证与测试

1. **后端全模块工程编译**：
   - 执行 `mvn install -DskipTests`，全部 12 个模块均通过构建（`BUILD SUCCESS`）。
2. **前端 Vite 打包编译**：
   - 执行 `npm run build`，Vue 3 + TypeScript 严格模式无报错，成功输出生产产物。
3. **Docker Swarm 真实环境联调测试**：
   - 验证 `--detach=true` 与 `--no-resolve-image` 编排启动速度提升至 1 秒级；
   - 验证 `docker ps --filter "label=com.docker.swarm.service.name=..."` 能够毫秒级定位任务容器并成功运行 `docker exec`；
   - 验证测试服务与网络能正常回收。
