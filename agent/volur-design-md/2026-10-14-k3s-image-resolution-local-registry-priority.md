# K3s 靶机调度镜像名称本地私有仓库优先解析机制施工与审计文档

## 1. 变更背景与问题定位
在 Volur Lite 基于 K3s 调度的容器靶机架构中，各赛题容器镜像可以通过主节点中继拉取并推送入集群内建的中心私有 Registry（`volur-registry`，例如 `10.145.251.25:5000`）。工作节点 / 子节点（如 `volur-resource-node`）在启动靶机 Pod 时，预期应当优先从本地中心私有 Registry 高速拉取镜像，避免因公网访问受限或私有镜像权限不足导致启动失败。

### 1.1 现场复现与根因
在子节点拉取镜像时出现以下故障事件：
- Pod 状态陷入 `ImagePullBackOff`；
- 报错信息显示：Kubelet 直接向 `ghcr.io` 发起匿名请求，触发 `401 Unauthorized`；
- 根因分析：在 [`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java) 的 `resolveEffectiveImageName` 解析方法中，原有逻辑直接判断镜像名称首段是否包含点号 `.` 或冒号 `:`。若包含（如 `ghcr.io/humolix/...`），则将其判定为外部公网镜像并立即提前返回，**导致彻底绕过了对本地私有 Registry 的存在性校验**，未拼接本地 Registry 前缀（`10.145.251.25:5000/`），使 K3s 子节点直接向外部公网地址请求镜像。

---

## 2. 核心改动点与实现细节

### 2.1 K3s 运行时镜像解析策略优化 (`K3sRuntimeStrategy.java`)
- 文件路径：[`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
- **改动详情**：
  1. **移除提前截断判断**：移除原先检测到外部域名直接提前 `return clean` 的过早退出逻辑；
  2. **双通道存在性判定 (`isImageInLocalRegistry`)**：
     - **数据库索引通道**：优先校验 `clusterImageRepository.findByRepositoryAndTag` 且 `inRegistry == true`；
     - **私有 Registry API 通道**：若数据库未及时刷新或管理员通过 CLI 手动推入镜像，直接通过 `dockerRegistryClient.getManifest(repo, tag)` 向本地 Registry 快速校验 Manifest 存在性；
  3. **层级回退与前缀裁剪**：
     - 优先匹配完整仓库名（如 `ghcr.io/humolix/wzbc2025_web_babyweb:latest`）；
     - 若未匹配且镜像名包含域名（如 `ghcr.io/`），自动尝试去除域名后的二级路径（如 `humolix/wzbc2025_web_babyweb:latest`）进行本地 Registry 探测；
     - 命中任意一项，则自动组装并返回带私有 Registry 地址的镜像 Tag（如 `10.145.251.25:5000/ghcr.io/humolix/wzbc2025_web_babyweb:latest`）；
     - 均未命中时，方回退为原始外部镜像名称，允许集群向外部公网尝试拉取。

### 2.2 镜像分发与同步保障 (`AdminImageServiceImpl.java`)
- 文件路径：[`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)
- **改动详情**：
  - 在 `ensureImageOnNode` 节点镜像同步逻辑中，补充私有仓库 API Manifest 存在性校验与域名截断二次探测，确保手动推送或未同步数据库记录的镜像也能通过中心私有仓库下发。

---

## 3. 安全性与规范遵从

1. **零注释铁律**：后端 Java 代码文件内严禁任何冗余解释性行内注释，所有架构演进与设计思路均收敛记录于本审计文档。
2. **零 Mock 与快速失败**：Registry 探测直接基于真实网络 API 与持久化数据表，未命中则严格回退，不存在任何硬编码伪造数据。
3. **不可变依赖与编码规范**：组件注入严格遵守 `private final` 与 Lombok `@RequiredArgsConstructor` 规范。
