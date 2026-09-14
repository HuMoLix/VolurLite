# 集群节点纳管 sudoer 用户特权自动提权支持施工记录

## 1. 变更背景与问题定位

在 Volur-Lite 平台集群管理（`volur-container`）中，扩展添加集群节点（`POST /api/v1/admin/clusters/nodes`）需要向目标 Linux 宿主机自动下发一系列系统级指令，包括但不限于：
1. 检查或安装 openssl 工具包；
2. 在 `/etc/docker/certs/` 生成 TLS 双向认证密钥与证书；
3. 读取 `/etc/docker/certs/key.pem`（权限 `0400`）等凭据；
4. 编排写入 `/etc/systemd/system/docker.service.d/override.conf`；
5. 重载 systemd 守护进程并重启 Docker 服务（`systemctl daemon-reload && systemctl restart docker`）；
6. 触发工作节点加入 Docker Swarm 覆盖网络（`docker swarm join`）。

**问题复现**：
当管理员在前端配置普通运维特权用户（例如 `user=volur`）而非 `root` 用户时，JSch 通过非交互式会话（`ChannelExec`）下发裸命令执行 `systemctl daemon-reload`，触发了 Polkit 系统的安全拦截，报错：
```
业务异常: status=500 INTERNAL_SERVER_ERROR, errorCode=500, message=在目标节点配置 2376+TLS override.conf 失败: Failed to reload daemon: Interactive authentication required.
```
同时，写入 `/etc/docker` 和 `/etc/systemd` 以及回读 `0400` 私钥文件也存在权限拒绝（`Permission denied`）的安全障碍。

---

## 2. 详细改动点与架构设计

根据“SSH 密码即为 sudo 密码”的约定，在 [`AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java) 中进行了如下重构：

### 2.1 引入智能提权探测机制（Privilege Detection）
在 SSH 握手成功后，首先对目标用户进行提权模式探针检测：
- **`ROOT`**：`sshUser` 为 `root`，直接执行原生指令，零性能损耗与零逻辑变化；
- **`SUDO_NOPASSWD`**：探测 `sudo -n true` 成功，代表目标机已配置免密 sudo（`NOPASSWD: ALL`），后续特权命令包装为 `sudo -n -- sh -c '<command>'`，不占用任何 stdin 输入；
- **`SUDO_PASSWORD`**：探测 `sudo -n true` 失败但提供了密码，通过 stdin 喂入密码执行 `sudo -S -p '' true` 成功，后续特权命令包装为 `sudo -S -p '' -- sh -c '<command>'` 并通过 JSch 自动向 stdin 输入密码；
- **`UNPRIVILEGED`**：若既非免密亦无法通过密码提权，在执行任何安装操作前立即拦截并抛出精准的 `BusinessException(HttpStatus.BAD_REQUEST)`，提示管理员检查 sudoers 配置。

### 2.2 重构底层 SSH 执行通道（Stdin Password Injection）
- 在 `executeSshCommandInternal` 中，当检测到需要密码提权时，使用 `ByteArrayInputStream` 绑定 `channel.setInputStream(...)`，在建立 SSH 通道时安全地将密码流式输入目标进程；
- 避免使用 `echo "<password>" | sudo` 这种容易在远程宿主机 `ps -ef` 中泄露明文密码的反模式；
- 使用 `-p ''` 清空 sudo 密码提示符，防止 `Password:` 字符混淆 stderr。

### 2.3 复合 Shell 指令单引号安全转义（Single-Quote Escaping）
- 在执行诸如 `printf '[Service]\n...' > override.conf && systemctl ...` 这类包含重定向与多指令复合逻辑时，统一使用标准 Shell 转义 `command.replace("'", "'\\''")`，并包裹至 `sh -c '...'` 中执行，确保重定向与子进程全部以 root 身份运行，彻底规避权限拒绝。

### 2.4 全面升级核心特权指令调用链路
- `addNode` 中的 Docker 存在性检查、版本获取、硬件指标采集接入特权模式；
- `setupDockerTlsAndValidate` 中证书生成、回读 0400 证书、override.conf 配置写入、Docker 重启与 TLS 握手测试全面改用特权执行；
- 工作节点加入 Swarm 集群及 `checkWorkerSwarmNodeId` 升级为特权执行，防止普通用户未加入 `docker` 用户组时无法访问 Docker Socket。

---

## 3. 安全隐患说明与防护措施

1. **凭据安全**：密码通过 JSch 加密通道的 stdin 字节流直接传输给目标主机的 PAM/sudo 认证机制，不会作为命令行参数暴露在系统进程树中。
2. **非交互式超时保护**：所有特权命令带有明确的超时截止时间（`deadline`），如果由于网络拥塞或死锁，JSch 循环检测会在超时后主动中断并销毁 Channel 与 Session。
3. **前置权限隔离与快速失败（Fast-Fail）**：未通过 sudo 验证的非 root 账号不会触碰任何后续的证书生成与系统配置，防止留下半初始化的脏数据。

---

## 4. 影响范围分析

- **涉及模块**：`volur-container`
- **修改文件**：[`AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
- **兼容性**：
  - 对原有使用 `root` 账户添加节点保持 100% 向下兼容；
  - 对数据库实体和前后端交互 DTO 零破坏，不需要执行数据库 DDL 变更；
  - 平台所有 12 个子模块及整体平台编译与测试构建（`mvn test-compile`）全部成功通过。
