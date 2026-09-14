# AdminImageServiceImpl Record 拆分与 MapStruct 映射重构

## 1. 变更背景
在原 `AdminImageServiceImpl.java` 中：
1. 存在私有内部 Java `record`（`DiscoveredDockerImage`、`DockerContainerBriefInfo`、`DockerSwarmServiceBriefInfo`）混杂在 Service 实现类内部的问题，不符合模块领域模型统一定义与领域对象分层组织规范；
2. 存在手写 Builder 构建 VO 对象（例如 `AdminClusterImageStatisticsVO.builder()`、`AdminClusterImageDetailVO.builder()`、`DockerImageLayerVO.builder()`）及部分实体对象构建逻辑，未全面遵循 Volur Lite 架构规范中“统一使用 MapStruct 声明式接口进行跨层对象映射与模型转换”的要求。

为了保证容器模块的架构纯粹性、代码可读性以及对象转换的高性能与类型安全性，对 `AdminImageServiceImpl` 进行结构拆分与 MapStruct 映射器改造。

---

## 2. 核心改动点

### 2.1 独立 Record 抽取收敛至 `domain.record`
将原类内部的私有静态 record 抽取为公共独立类型，统一归置于 `com.humolix.volur.container.domain.record` 包路径下：
1. [`DiscoveredDockerImage`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/record/DiscoveredDockerImage.java)：
   承载 Docker 引擎本地及远程节点通过 HTTP API / Unix Socket / CLI 扫描发现的镜像摘要元数据（`repository`, `tag`, `imageId`, `sizeBytes`）。
2. [`DockerContainerBriefInfo`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/record/DockerContainerBriefInfo.java)：
   承载 Docker 活跃容器运行状态、祖先依赖及镜像关联简报（`id`, `name`, `status`, `state`, `image`）。
3. [`DockerSwarmServiceBriefInfo`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/record/DockerSwarmServiceBriefInfo.java)：
   承载 Docker Swarm 集群活跃服务运行状态与镜像关联简报（`id`, `name`, `image`, `replicas`）。

### 2.2 扩展 `ClusterImageMapper` MapStruct 接口
在 [`com.humolix.volur.container.mapper.ClusterImageMapper`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ClusterImageMapper.java) 中新增标准化转换接口定义：
1. `toStatisticsVO(...)`：
   用于由统计标量（`totalImages`, `challengeImages`, `baseImages`, `totalSizeBytes`, `registryOnline`, `registryImagesCount`）生成 `AdminClusterImageStatisticsVO`。
2. `toDetailVO(...)`：
   将 `ClusterImage` 实体与 Docker Inspect、History 分层等元数据综合映射为 `AdminClusterImageDetailVO`。
3. `toLayerVO(...)`：
   将 Docker 镜像历史分层参数编译期映射为 `DockerImageLayerVO`。
4. `toEntity(DiscoveredDockerImage discovered, String imageType, String nodeIp)`：
   将发现的镜像元数据直接投影映射为持久化 `ClusterImage` 实体。

### 2.3 `AdminImageServiceImpl` 业务重构
1. 移除类内部的私有 `record` 声明，统一通过 `import com.humolix.volur.container.domain.record.*` 导入；
2. 彻底重构 `getStatistics()` 方法，将原 `AdminClusterImageStatisticsVO.builder()...build()` 转为调用 `clusterImageMapper.toStatisticsVO(...)`；
3. 彻底重构 `getImageDetail(Long id)` 方法，将原 `AdminClusterImageDetailVO.builder()...build()` 转为调用 `clusterImageMapper.toDetailVO(...)`；
4. 彻底重构 `queryDockerHistory(...)` 方法，将分层 VO 的构造转为调用 `clusterImageMapper.toLayerVO(...)`；
5. 重构 `syncDiscoveredImageToRepository(...)` 方法中发现镜像入库实体构造，转为调用 `clusterImageMapper.toEntity(...)`。

---

## 3. 安全隐患修复与质量保证
1. **类型安全与编译期校验**：由 MapStruct 在编译期自动生成映射实现，杜绝了手动调用 Builder 时参数漏传或手写字段名拼写错误隐患；
2. **单一数据源与实体隔离**：将内部数据结构公开为领域 `record` 后，避免了模块间或子流程对宿主机容器与 Swarm 结构的反模式重复定义；
3. **完全向后兼容**：所有 API 接口出参 VO 属性结构完全一致，无任何破坏性变动。

---

## 4. 影响范围
- **受影响模块**：`volur-container`
- **新建文件**：
  - `com.humolix.volur.container.domain.record.DiscoveredDockerImage`
  - `com.humolix.volur.container.domain.record.DockerContainerBriefInfo`
  - `com.humolix.volur.container.domain.record.DockerSwarmServiceBriefInfo`
- **修改文件**：
  - `com.humolix.volur.container.mapper.ClusterImageMapper`
  - `com.humolix.volur.container.service.admin.impl.AdminImageServiceImpl`
- **回归验证**：
  - Maven 全模块编译构建通过（`mvn clean compile` / `mvn test-compile` 均 100% SUCCESS，无任何 MapStruct 映射告警或编译错误）。
