# Docker 镜像删除失效排查与强制解绑 (-f) 修复审计记录

## 1. 问题背景与现象
用户在管理端前端页面（`AdminImageView.vue`）删除镜像（`docker-broker`、`docker-attacker`、`deploy-app`）后，前端页面提示删除成功，数据库记录已被移除，但在宿主机终端执行 `docker images` 时发现部分镜像依然存在未被清理。

---

## 2. 根因深度溯源分析

经过对宿主机 Docker 运行时环境与后端执行链路的排查，发现以下核心原因：

1. **已停止容器存在残留引用冲突（Docker Image Conflict）**：
   - 宿主机存在已停止的历史容器：
     - `docker-broker-1` (容器 ID: `c422971819ef`) 引用了 `docker-broker`；
     - `docker-attacker-1` (容器 ID: `9070c060467f`) 引用了 `docker-attacker`；
   - 当调用常规 `docker rmi <imageName>` 时，Docker 守护进程会阻止删除并返回错误：
     ```text
     Error response from daemon: conflict: unable to remove repository reference "docker-broker" (must force) - container c422971819ef is using its referenced image 4ef4ee7febc2
     ```
   - 相比之下，`deploy-app` 因无任何存活或停止容器引用，在先前已经被成功删除。

2. **后端 `triggerDockerRmiOnNode` 缺少 `-f` 强制选项**：
   - 原代码构造的删除命令仅为 `docker rmi <imageName>`；
   - 面对存在历史容器弱引用的情况，未启用强制模式导致 Docker 拒绝移除 Tag。

3. **执行状态与异常静默忽略问题**：
   - 原代码调用 `Process.waitFor(15, TimeUnit.SECONDS)` 后，既未检查 `process.exitValue()`，也未消费或记录 `getErrorStream()`；
   - 无论 Docker 底层成功还是冲突报错，方法均静默结束，随后直接执行了 `clusterImageRepository.delete(image)`；
   - 最终呈现为“数据库已删、前端已无、Docker 仍存”的现象。

4. **节点列表为空时的本地主机兜底缺失**：
   - `deleteImage` 中如果某些镜像元数据的 `nodeIps` 字段未登记节点 IP，导致遍历集合为空，不会向本地 Docker 发送 `docker rmi` 命令。

---

## 3. 修复方案与代码改动

1. **`AdminImageServiceImpl.java` 增强与修复**：
   - **增加 `-f` 强制删除参数**：
     在 `triggerDockerRmiOnNode` 中，将执行命令变更为 `docker rmi -f <imageName>`。即使存在已停止的历史容器引用，Docker 也会强制解绑（Untag）并清理该镜像标识；
   - **捕获与输出 STDERR 错误日志**：
     检查 `process.exitValue()`，若退出码非 0，及时提取并记录错误流信息，便于运维审计与排错；
   - **节点缺省兜底**：
     在全量删除镜像时，若 `parseNodeIps(image.getNodeIps())` 为空，自动以系统配置的本地主节点 IP（缺省 `127.0.0.1`）进行兜底清理，确保本地宿主机始终执行物理删除。

---

## 4. 验证结果
- 手动对残留的冲突镜像执行 `docker rmi -f`，成功 Untag 并彻底移除；
- 执行 `docker images` 核验，`docker-broker`、`docker-attacker`、`deploy-app` 均已在宿主机 Docker 中完全消失；
- 后端模块 `mvn test-compile -pl volur-container` 编译通过（BUILD SUCCESS）。
