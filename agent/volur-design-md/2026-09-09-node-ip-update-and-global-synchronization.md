# 集群节点 IP 动态修改、运行中容器安全拦截与全平台数据级联同步

## 1. 变更背景与设计动机 (Background & Design Motivation)

在分布式容器编排与靶机集群部署运维中，工作节点（Worker Node）经常因机房迁移、子网重构、DHCP 变动或云服务器弹性 IP 调整而发生 IP 地址变更。

在既有系统架构中，节点 IP 存在如下痛点与安全隐患：
1. **IP 锁定与不可编辑**：前端编辑弹窗直接将 IP 字段置为 `:disabled="props.mode === 'edit'"`，后端 DTO 亦缺乏 IP 更新支持，导致运维人员一旦需要变更 IP，只能将节点强制删除后再重新注册；
2. **缺乏运行时安全性核验**：若在节点仍承载用户竞赛题目靶机容器时盲目修改 IP，将直接导致当前在线选手的题目靶机访问中断、动态 Flag 验证失联、端口映射孤立以及底层 Swarm Overlay 网络断裂；
3. **安全凭据与证书拓扑失效**：Volur Lite 采用标准的 Docker 2376 TLS 双向证书体系（Subject Alternative Name: `IP:<ip>,IP:127.0.0.1`）。若仅修改数据库 IP，平台将因客户端 TLS SAN 校验不匹配触发 `SSLPeerUnverifiedException`，导致节点管理永久脱机；
4. **全平台镜像与实例孤岛**：平台的镜像缓存拓扑（`ClusterImage.node_ips`）及所有历史与离线容器实例（`CtfChallengeInstance.node_ip` / `access_url`）深度绑定了旧 IP。若不进行级联对账与全平台同步，容器管理与镜像调度将发生调度偏差。

---

## 2. 核心实施方案与技术实现 (Key Implementations)

### 2.1 运行时容器前置安全熔断机制 (Active Container Pre-flight Safety Guard)
为杜绝“带病变更”，在 `AdminClusterServiceImpl.java` 的 `updateNode` 方法中，当检测到目标 IP 发生变更（`!newIp.equalsIgnoreCase(oldIp)`）时，执行三重严格核验：
1. **平台业务实例层核验**：
   通过 `ctfChallengeInstanceRepository.countByNodeIpAndStatusIn(oldIp, [RUNNING, STARTING, DOWNLOADING_IMAGE, QUEUED])` 查询属于该节点且处于活跃、启动中或拉取中的题目靶机实例数量；
2. **集群节点统计层核验**：
   核验 `node.getContainerCount()` 记录的活动容器计数；
3. **宿主机底层实时探针核验**：
   若节点 SSH 凭据有效，通过底层 SSH 直连执行 `docker ps -q | wc -l` 探测远程节点宿主机上实际处于运行态的真实 Docker 容器数量；
4. **熔断阻断**：
   若上述任一项统计大于 0，立即熔断并抛出友好具体的 `BusinessException`：
   > `安全检查未通过：目标节点当前仍有 X 个正在运行中的容器实例。为了避免运行中实例网络中断或失联，严禁在活跃状态下更改节点 IP！请先在容器管理中停止或销毁相关实例后再进行修改。`

### 2.2 目标 IP 唯一性与网络连通性预检
1. **IP 唯一性排他校验**：
   `clusterNodeRepository.existsByIpAddress(newIp)`，杜绝 IP 冲突；
2. **目标网络与 SSH 认证预检**：
   使用目标新 IP 与生效的端口/凭据执行 `checkIpReachable(newIp, port)` 与 `verifySshConnectivity(newIp, ...)`。在编辑弹窗中点击“测试连接”时，亦优先透传并测试用户输入的新 IP 地址。

### 2.3 Docker 2376 TLS 双向安全证书自动化重新签发
由于 Docker TLS Server 端证书的 `subjectAltName` 必须包含新 IP：
1. 调用 `setupDockerTlsAndValidate(newIp, ...)`：
   - 在远程宿主机自动生成包含 `IP:${newIp},IP:127.0.0.1` SAN 的 Server 与 Client 双向证书；
   - 远程配置 `systemd` `override.conf` 监听 `0.0.0.0:2376` 并重启 Docker 守护进程；
   - 将新证书拉回并持久化于平台本地目录 `data/certs/nodes/${newIp}`；
2. **旧证书垃圾回收**：
   遍历并安全清理平台本地失效目录 `data/certs/nodes/${oldIp}`，防止磁盘残留无用密钥文件。

### 2.4 Docker Swarm 自动脱网与重新入网
1. 对 Worker 节点，下发 `docker swarm leave --force 2>/dev/null` 释放旧 IP 在 Swarm 中的成员信息；
2. 获取主控端 Worker Join Token，向新 IP 下发 `docker swarm join --token <TOKEN> <MASTER_IP>:2377`；
3. 等待 Gossip 状态同步，获取新的 `Swarm Node ID` 并更新至 `node.setClusterNodeId(...)` 与 `node.setInCluster(true)`。

### 2.5 全平台功能与数据级联同步 (Full-Platform Cascade Synchronization)
1. **集群镜像缓存拓扑同步 (`ClusterImage.nodeIps`)**：
   遍历全平台所有 `ClusterImage`，将其 `nodeIps`（逗号分隔的 IP 列表）中的 `oldIp` 自动平滑置换为 `newIp`，并调用 `clusterImageRepository.save(image)`；修改完成后立即触发 `adminImageService.loadAndSyncClusterImages()` 进行全网对账，确保镜像分发与调度路由即时感知；
2. **容器靶机实例映射同步 (`CtfChallengeInstance`)**：
   通过 `ctfChallengeInstanceRepository.findByNodeIp(oldIp)` 获取历史已停止或已保存的实例，将 `inst.nodeIp` 更新为 `newIp`，并将 `inst.accessUrl` 中包含的 `oldIp` 文本级联置换为 `newIp`；
3. **前端交互与安全指引对齐 (`ClusterNodeModal.vue` & `types/cluster/index.ts`)**：
   - `AdminClusterNodeUpdateDTO` 显式补充 `ipAddress` 严格类型定义；
   - 放开编辑弹窗中的 IP 输入框，绑定实时表单校验（IPv4 正则）；
   - 当检测到管理员在编辑模式下修改了 IP 地址时，动态展示带有明确安全说明的橙色 `NAlert` 警告栏，提醒管理员安全检查规则与自动化迁移流程。

### 2.6 未入网节点基础配置编辑解耦与 Master 自动兜底 (Unjoined Node Decoupling)
1. **解除编辑阻塞与死锁消除**：
   - 前端移除 Worker 角色对 `isResourceNode` 的强制锁死逻辑，允许管理员自由启闭资源调度分配；
   - 后端 `updateNode` 移除对未入网节点的强制抛错拦截：当工作节点尚未加入编排集群（`inCluster == false`）时，后端允许正常保存其 IP、名称与 SSH 认证凭据，同时自动将 `isResourceNode` 降级置为 `false`，防止非法调度；
   - 前端在保存完成后给出提示：`节点配置已更新；该节点尚未入网，暂未激活为题目资源节点，请在配置网络后点击“加入集群”`；
2. **Master 控制面自动兜底机制**：
   - 后端在更新节点拓扑时，自动核验集群中是否存在任何已入网且处于就绪态的 Worker 资源节点；
   - 若不存在任何就绪的 Worker 资源节点，Master 控制面自动激活承担 `isResourceNode = true` 题目运行角色，确保单机模式或调试网络期间平台 CTF 题目仍能正常拉起并稳定运行。

---

## 3. 涉及文件清单 (Modified Files)

1. **后端平台 (Backend)**：
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterNodeUpdateDTO.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/CtfChallengeInstanceRepository.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java`
2. **前端视图 (Frontend)**：
   - `src/volur-lite-view/src/types/cluster/index.ts`
   - `src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue`

---

## 4. 验证与测试结果 (Verification)

1. **后端编译验证**：
   执行 `mvn test-compile -pl volur-container -am`：
   - 66 个源文件编译完成，无语法或类型错误，`BUILD SUCCESS`；
2. **前端打包验证**：
   执行 `npm run build`（含 `vue-tsc --noEmit`）：
   - 类型校验 100% 通过，`vite build` 成功，无任何 TypeScript 报错。
