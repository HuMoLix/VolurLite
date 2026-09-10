# 集群节点信息修改与镜像管理同步对齐设计与落地

## 1. 变更背景与设计动机 (Background & Motivation)

在 Volur Lite 分布式靶机平台中，集群节点（`ClusterNode`）承担着 Docker 容器实例运行与镜像分发拓扑的核心职能。管理员在集群管理页面（`AdminClusterSettingsView.vue`）中对节点信息进行修改（例如修改节点 IP 地址、调整角色或启闭题目资源调度属性 `isResourceNode`）时，平台的镜像管理（`AdminImageView.vue` 及底层 `ClusterImage`）需要与节点状态保持严格的强一致性。

在既有实现中存在以下逻辑脱节与数据失真问题：
1. **旧节点拓扑虚假置换**：当管理员变更节点 IP 时，后端仅在内存中将 `ClusterImage.nodeIps` 中的旧 IP 字符串简单替换为新 IP，导致新节点底层尚未拉取该镜像时，数据库却虚假记录为已拥有；
2. **旧节点镜像记录残留**：旧节点的 IP 应当从平台镜像管理拓扑中彻底清除，若新节点尚未完成镜像同步，镜像分发状态应真实反映为“未分发”或仅属于 Master 节点；
3. **节点角色与资源降级失步**：当节点取消题目资源调度资格（`isResourceNode` 设为 `false`）或节点被直接移除（`removeNode`）时，镜像管理中仍残留该节点的赛题镜像关联，未能联动清理；
4. **前端跨视图脱节**：管理员在集群设置页面修改节点后，镜像管理页面缺乏实时的状态感知与数据同步机制。

根据用户指示，**无需主动连接删除旧宿主机物理 Docker 镜像**（保留旧机物理镜像以兼顾网络下线与缓存容灾），重点**落地平台镜像管理层面的旧节点镜像记录剥离、新节点镜像真实同步、节点降级/移除联动与前端跨视图实时响应**。

---

## 2. 核心改动点与技术实现 (Key Implementations)

### 2.1 后端服务层扩展与镜像拓扑清理 (`AdminImageService`)
在 `AdminImageService` 与 `AdminImageServiceImpl` 中新增接口与实现：
```java
/**
 * 从平台镜像管理中彻底剥离指定节点的镜像拓扑映射
 *
 * @param nodeIp 目标节点 IP
 * @param onlyChallenge 是否仅针对赛题镜像 (用于节点降级退出资源池场景)
 */
void removeNodeFromImages(String nodeIp, boolean onlyChallenge);
```
- **实现逻辑**：
  1. 扫描所有 `ClusterImage` 记录（若 `onlyChallenge=true`，仅过滤 `imageType == CHALLENGE`）；
  2. 从每个镜像的 `nodeIps` 集合中移除 `nodeIp`；
  3. 若移除后集合为空，置为空字符串（镜像管理视图将准确展示为“未分发”），否则重新拼接去重后的 IP 列表并持久化；
  4. 级联调用 `updateNodeImageCounts()` 重新核算集群中所有存活节点的 `imageCount`。

### 2.2 节点更新与移除的级联同步 (`AdminClusterServiceImpl`)
1. **节点 IP 变更场景 (`ipChanged == true`)**：
   - 移除原先的字符串粗暴替换逻辑；
   - 调用 `adminImageService.removeNodeFromImages(oldIp, false)`，从所有镜像拓扑中剔除旧节点 IP；
   - 保存新节点信息后，若新节点为资源节点（`saved.getIsResourceNode() == true`）且开启了自动迁移（`autoMigrateImagesEnabled == true`），主动向新节点触发赛题镜像拉取与分发（`migrateMasterImagesToWorker(saved)`），拉取成功后才将新 IP 纳入对应镜像的 `nodeIps`；
   - 触发全网镜像对账 `loadAndSyncClusterImages()`。
2. **资源节点降级场景 (`isResourceNode: true -> false`)**：
   - 当节点 IP 未改变，但管理员将 `isResourceNode` 设为 `false`（或在 `updateNodeRole` 中取消资源节点属性），自动调用 `adminImageService.removeNodeFromImages(effectiveIp, true)`，从赛题镜像中剔除该节点拓扑，释放该节点的题目镜像标记。
3. **节点移除场景 (`removeNode`)**：
   - 节点删除时，同步调用 `adminImageService.removeNodeFromImages(nodeIp, false)`，防止已删除的孤儿 IP 悬挂在镜像管理中。

### 2.3 前端跨模块联动与视图渲染对齐
1. **全局事件广播 (`AdminClusterSettingsView.vue`)**：
   - 在节点修改成功（`handleNodeModalSuccess`）、镜像手动同步（`handleSyncImagesToNode`）、重新入网（`handleJoinCluster`）及节点删除（`handleRemoveNode`）后，统一向全局分发自定义同步事件：
     ```ts
     window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
     ```
2. **镜像管理实时感知与刷新 (`AdminImageView.vue`)**：
   - 挂载时监听 `volur:cluster-nodes-updated` 事件，卸载时注销监听；
   - 监听到事件后自动重新拉取 `fetchClusterNodes()`、`fetchImages()` 与 `fetchStatistics()`；
   - 搜索刷新函数 `handleSearch()` 同步增强为联动刷新节点与统计；
   - 表格 `nodeIps` 列因旧 IP 已被彻底剥离，不再渲染带有误导性的红标 `(离线)` 孤儿标签。
3. **编辑弹窗安全指引优化 (`ClusterNodeModal.vue` & 国际化)**：
   - 升级 IP 变更警告中的说明文案，明确提示平台将自动从镜像管理中解绑旧节点并同步新节点镜像拓扑；
   - 统一中英文保存成功通知：`“节点配置已更新，已同步更新镜像管理拓扑”`。

---

## 3. 涉及文件清单 (Modified Files)

1. **后端服务层 (Backend)**：
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/AdminImageService.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java`
   - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java`
2. **前端视图层与国际化 (Frontend & I18n)**：
   - `src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue`
   - `src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue`
   - `src/volur-lite-view/src/views/admin/image/AdminImageView.vue`
   - `src/volur-lite-view/src/locales/zh_CN/admin/clusters.json`
   - `src/volur-lite-view/src/locales/en_US/admin/clusters.json`

---

## 4. 验证与测试结果 (Verification)

1. **后端工程编译**：
   - 执行 `mvn test-compile -pl volur-container -am`；
   - 10 个模块编译全部通过，`BUILD SUCCESS`。
2. **前端工程类型检查与打包**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - TypeScript 类型校验 100% 通过，无任何报错，打包顺利完成。
