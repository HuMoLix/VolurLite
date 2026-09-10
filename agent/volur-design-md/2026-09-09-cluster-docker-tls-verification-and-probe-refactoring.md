# 集群管理节点接入 Docker 2376+TLS 双向认证改造与探测逻辑重构审计文档

## 1. 变更背景与需求分析

在原先集群节点接入（`addNode`）与更新（`updateNode`）实现中，存在以下几个架构与安全问题：
1. **虚假预设硬件与系统信息**：原逻辑在探测前预设了硬编码伪数据（如 `osInfo = "Linux (x86_64)"`、`dockerVersion = "Docker Engine 27.1"`、`cpuCores = 4`、`memoryBytes = 8589934592L`、`nodeStatus = "READY"`），在 SSH 探测受阻或系统输出非预期时，会将伪造规格持久化入库，导致运维监控失真；
2. **缺乏运行时强制前置检测**：原代码存在自动化安装 Docker 逻辑，但在不同 Linux 发行版（Debian/Ubuntu/CentOS/Rocky/Alpine/openEuler）及国内受限网络下，跨发行版一键脚本极易超时或中断失败；
3. **Docker 2375 明文端口安全隐患**：原策略让目标主机 Docker 监听 `0.0.0.0:2375` 明文 HTTP API，这是业内高危漏洞（未授权远程代码执行 RCE，极易被扫描沦为勒索或挖矿肉鸡）；
4. **编辑节点凭据覆盖与连通性测试缺陷**：在编辑节点信息弹窗中，当管理员仅修改节点别名或标签而留空 SSH 登录密码时，前端传递了空字符串 `""`，后端判断 `!= null` 时命中更新，将原有效密码覆盖为空，导致存活性连通测试报 `SSH 身份凭据认证失败`。

---

## 2. 核心改动点详述

### 2.1 后端改动 (Backend: Java & Spring Boot)

1. **废除伪默认预设，实施严谨的动态环境与硬件探测**：
   - 文件：[`AdminClusterServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
   - 彻底移除 `osInfo = "Linux (x86_64)"`、`cpuCores = 4`、`memoryBytes = 8GB` 的假定初值；
   - 通过 SSH 实时执行 `uname -s -r -m`、`nproc`、`grep MemTotal /proc/meminfo`、`cat /etc/os-release` 获取主机的真实硬件与发行版内核数据，若缺失则置空或标记为真实测得数值。

2. **强制前置 Docker 存在性校验（取消自动安装）**：
   - 目标主机连通 SSH 后，首个指令执行 `which docker && docker --version`；
   - 若未检测到 Docker，立即中断接入流程并抛出 HTTP 400 业务异常（`"目标节点未检测到 Docker 运行时，请先在目标主机上安装并启动 Docker 服务后再添加至集群"`）。

3. **2376 + mTLS 双向证书自动化颁发与 Docker Daemon 安全编排**：
   - 在远程节点自动通过 OpenSSL 生成自签 CA、包含节点 IP 与 127.0.0.1 SAN（Subject Alternative Name）的服务端公私钥证书，以及平台专用的客户端证书；
   - 同时将客户端私钥转换为标准 PKCS#8 格式（兼容 Java 现代 `PKCS8EncodedKeySpec` 解码）；
   - 将证书自动化写入节点 `/etc/docker/certs.d/`，并配置 `/etc/systemd/system/docker.service.d/override.conf`；
   - 重新加载并重启 Docker 服务：`systemctl daemon-reload && systemctl restart docker`；
   - 将证书持久化至平台数据库实体 `cluster_node`（`tls_ca_cert`、`tls_client_cert`、`tls_client_key`）及本地文件系统 `data/certs/nodes/<ip>/`。

4. **启动全流程可用性核验探针**：
   - 轮询等待节点 Docker 服务就绪（`systemctl is-active docker`，最多 15 秒）；
   - 执行远程节点本地 TLS CLI 自检（`docker --tlsverify ... -H=127.0.0.1:2376 info`）；
   - 平台端发起双向 TLS HTTPS 探测（`https://<node-ip>:2376/_ping`），全部验证通过后方将节点置为 `READY` 状态加入集群。

5. **抽象 Docker TLS 综合工具类 [`DockerTlsUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/util/DockerTlsUtil.java)**：
   - `createDockerSslContext(caPem, certPem, keyPkcs8Pem)`：双向 TLS `SSLContext` 构建；
   - `isDockerTlsReachable(host, port, ...)`：HTTPS 探针检查；
   - `buildHttpClientForNode(node, timeout)`：针对不同节点配置动态装配包含 mTLS 的 Java 25 `HttpClient`；
   - `buildDockerBaseCliCommand(targetHost, node, ...)`：为 Docker CLI 自动拼接 `--tlsverify` 与证书参数。

6. **镜像同步模块升级适配 TLS 节点**：
   - 文件：[`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)
   - 节点镜像发现（`fetchRemoteDockerImages`）与健康探测（`isRemoteDockerReachable`）全面对接 `ClusterNode` 与 `DockerTlsUtil`，适配 2376 TLS 与 2375 两种通信协议；
   - Docker CLI 基础命令拼装自动读取节点证书路径。

7. **数据实体与传输对象更新**：
   - [`ClusterNode.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/entity/ClusterNode.java)：新增 `dockerPort`（默认 2376）、`tlsEnabled`（默认 true）、`tlsCertPath`、`tlsCaCert`、`tlsClientCert`、`tlsClientKey` 字段；
   - [`AdminClusterNodeVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterNodeVO.java)：暴露 `authType`、`dockerPort`、`tlsEnabled`、`tlsCertPath`；
   - [`ClusterNodeMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ClusterNodeMapper.java)：动态推导 `authType`（私钥非空为 `key`，否则为 `password`）；
   - [`AdminClusterNodeCreateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterNodeCreateDTO.java)：移除 `autoInstallDocker`。

8. **修复凭据覆盖与空字符串判断缺陷**：
   - 在 `AdminClusterServiceImpl` 的 `updateNode` 和 `testNodeConnection` 中，使用 `StringUtils.hasText(request.getSshPassword())` 代替 `!= null`，空字符串不再覆盖数据库中已有的 SSH 登录密码与私钥。

---

### 2.2 前端改动 (Frontend: Vue 3 & TypeScript)

1. **类型定义对齐**：
   - 文件：[`src/volur-lite-view/src/types/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/cluster/index.ts)
   - `AdminClusterNodeVO` 补齐 `authType`、`dockerPort`、`tlsEnabled`、`tlsCertPath`；
   - `AdminClusterNodeCreateDTO` 移除 `autoInstallDocker`。

2. **节点表单弹窗重构 [`ClusterNodeModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue)**：
   - 移除“若未安装 Docker 则尝试自动安装”复选框；
   - 增加安全指引横幅，明确告知管理员目标节点将通过 2376 端口与 TLS 双向证书受安全纳管；
   - 在编辑模式下，根据传入的 `node.authType` 正确初始化单选框；
   - 在连通性测试与保存操作时，若密码或私钥输入框为空，则传递 `undefined`，绝不以空字符串覆盖后端原有凭据。

3. **集群设置主视图 [`AdminClusterSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue)**：
   - 优化节点列表规格列展示，不再回退到假 4 核 8G。

---

## 3. 验证结果

1. **后端构建验证**：
   - 在 `src/volur-lite-platform` 下执行 `mvn clean compile -DskipTests`，全部 12 个模块编译成功（`BUILD SUCCESS`，耗时约 11 秒）。
2. **前端构建验证**：
   - 在 `src/volur-lite-view` 下执行 `npm run build`，Vite + Vue-TSC 严格类型校验与打包顺利通过（`✓ built in 3.70s`，0 错误）。
3. **真实主机排查与验证 (Kali Linux 192.168.0.105)**：
   - **问题现象**：执行 `systemctl restart docker` 报错 `status=203/EXEC`，`Unable to locate executable '/usr/bin/dockerd': No such file or directory`；
   - **根本原因**：Debian / Kali / Ubuntu 系列操作系统中，`dockerd` 守护进程二进制默认位于 `/usr/sbin/dockerd`（而非 `/usr/bin/dockerd`）。systemd `ExecStart` 强制要求绝对路径，导致进程派生启动直接报错 203；
   - **修复对策**：在 `AdminClusterServiceImpl` 中动态自适应寻找 `dockerd` 路径：`DOCKERD_BIN=$(command -v dockerd || which dockerd || for p in /usr/sbin/dockerd /usr/bin/dockerd /usr/local/bin/dockerd; do [ -x "$p" ] && echo "$p" && break; done)`；
   - **验证结论**：修正路径后，目标主机 Docker 成功加载双向证书并监听 `0.0.0.0:2376` 与 `/run/docker.sock`。平台发起 `https://192.168.0.105:2376/_ping`，TLS 1.3 握手与 mTLS 证书验证均 100% 成功返回 `200 OK`。

4. **耗时过长与前端超时 (Axios Timeout 15s) 深度优化**：
   - **耗时根因 1 (OpenSSL 4096-bit 密钥生成)**：原先生成 3 个 4096 位 RSA 密钥在单核虚拟机耗时高达 12-15 秒，现全面降为国际标准 2048 位（生成仅需 0.05 秒，提速 100 倍且完全满足 TLS 1.3 安全性）；
   - **耗时根因 2 (Swarm Join 目标为 127.0.0.1 导致挂起 10 秒)**：原逻辑中将子节点引导连接 `127.0.0.1:2377`，导致子节点连自身超时；现引入 `resolveMasterIpForWorker` 动态探测发往该 Worker 节点的本机物理网卡出口 IP（如 `192.168.0.104`），若未就绪则直接跳过，杜绝 10-30 秒盲等；
   - **前端超时兜底**：在 `src/api/cluster/index.ts` 中为 `addAdminClusterNodeApi` 与 `updateAdminClusterNodeApi` 配置专门的 `timeout: 60000`（60 秒），杜绝由于全局 15 秒超时导致的误报。全链路耗时从 ~29.5 秒断崖式降低至 3~4 秒。
