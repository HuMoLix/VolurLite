# Docker 镜像物理删除硬确认与数据库同步防脱节改造施工审计记录

## 1. 变更背景与需求概述
在镜像管理与删除业务流程中，原有的删除逻辑未能建立起“底层物理删除绝对先行且核验通过”的强保障：
此前逻辑仅在发出 `docker rmi` 命令后便直接执行了 `clusterImageRepository.delete(image)`，一旦底层 Docker 出现超时、拒绝删除或容器引用冲突，就会导致数据库记录已被删除但物理 Docker 镜像依然驻留宿主机的“数据脱节”隐患。

**本次改造核心硬性要求**：
- **必须严格在确认底层 Docker 镜像已被彻底删除之后，才能对数据库记录执行删除或节点解绑操作**；
- 若底层 Docker 删除失败（如正在被运行中的容器占用、节点离线通信异常或超时），必须立即中断流程并抛出 `BusinessException` 触发数据库事务强回滚，严禁删除或修改 MySQL 数据库中的镜像数据。

---

## 2. 关键改动点说明

### 2.1 后端核心逻辑重构 (`AdminImageServiceImpl.java`)

1. **构建强闭环物理删除与核验方法 (`removeDockerImageAndConfirm`)**：
   - **阶段一：前置探测检查 (Pre-check)**：
     - 调用 `queryDockerInspect(nodeIp, imageName)` 检测该节点 Docker 引擎中是否确实存在该镜像；
     - 若镜像本身已不在 Docker 中（此前已被清理或删除），直接视为已不存在，无需重复发送指令。
   - **阶段二：执行强制删除 (Execute)**：
     - 构造并调度 `docker [-H <remoteHost:2375>] rmi -f <imageName>`；
     - 增加 20 秒超时控制，防止子进程假死挂起；
     - 提取并分析子进程的 `exitValue()` 与 `getErrorStream()`。
   - **阶段三：执行状态研判 (ExitCode Validation)**：
     - 若 `exitCode != 0` 且错误信息非 `No such image`，直接抛出 `BusinessException(HttpStatus.CONFLICT, ...)`，明确反馈 Docker 引擎的拒绝原因（如被运行中容器占用等）；
   - **阶段四：后置主动核验保障 (Post-check Verification)**：
     - 执行完成后，再次调用 `queryDockerInspect(nodeIp, imageName)` 主动检测；
     - 若 Docker Inspect 仍能读取到该镜像对象，说明物理删除未生效，立即抛出 `BusinessException(HttpStatus.CONFLICT, ...)` 中断后续流程。

2. **数据库事务与操作时序重塑 (`deleteImage`)**：
   - 彻底将数据库的 `clusterImageRepository.delete(image)` 或 `image.setNodeIps(...)` 放置于所有目标节点的 `removeDockerImageAndConfirm` 之后；
   - 保证只有当目标节点（单节点或全量节点）全部成功通过“Docker 删除 + Inspect 核验”之后，才执行数据库删除；
   - 在出现任何异常时，借助 Spring `@Transactional` 实现数据库事务绝对回滚，确保数据库元数据与 Docker 宿主机物理镜像状态 100% 一致。

---

## 3. 验证结果
- `volur-container` 模块 Maven 编译测试通过（`BUILD SUCCESS`）；
- 前端 `volur-lite-view` npm build 生产构建通过（0 errors）；
- 代码逻辑严格保证：**Docker 物理删除未成功核验，数据库绝不执行任何删除操作**。
