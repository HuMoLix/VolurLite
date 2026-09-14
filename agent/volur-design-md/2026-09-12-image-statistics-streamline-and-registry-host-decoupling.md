# 集群镜像统计接口精简与 Registry 主机显示解耦施工记录

## 1. 变更背景

在 Volur Lite 容器与镜像架构向统一中心私有 Registry（Docker Registry V2）全面收敛的演进过程中，镜像的存储与拉取均围绕中心 Registry 开展。原先在节点分散拉取与多节点副本同步时期设计的统计字段已不再适用：
1. **服务端单位格式化冗余**：后端在接口响应中同时返回 `totalSizeBytes` 与预格式化的 `totalSizeFormatted` 字符串，破坏了 RESTful API 的纯数据职责原则。前端应基于原始数值自行进行高内聚的单位换算与自适应展示（MB、GB、TB）；
2. **废弃节点覆盖与同步统计**：由于已废除旧的多节点点对点镜像同步，`syncingImages`（同步中镜像数）与 `totalNodesCovered`（覆盖节点数）在中心化 Registry 架构下失去统计意义；
3. **Registry 主机端点解耦与 UI 净化**：镜像统计接口无需返回 `registryPublicHost`。前端各视图（概览 Ribbon、拉取提示框、镜像详情抽屉）此前硬编码或拼接展示的 `(127.0.0.1:5001)` / `(127.0.0.1:5000)` 等主机端口字样予以彻底清理，仅呈现统一语义的“中心私有 Registry”。

---

## 2. 具体改动点

### 2.1 后端工程改造 (Java & Spring Boot)

1. **统计视图模型精简 (`AdminClusterImageStatisticsVO.java`)**：
   - 彻底移除 `totalSizeFormatted`（预格式化存储大小）；
   - 彻底移除 `totalNodesCovered`（覆盖节点数）；
   - 彻底移除 `syncingImages`（同步中镜像数）；
   - 彻底移除 `registryPublicHost`（Registry 端点信息）；
   - 严格保留核心指标：`totalImages`、`challengeImages`、`baseImages`、`totalSizeBytes`、`registryOnline`、`registryImagesCount`。
   - 严格遵循 `Rules.md` 规范，使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 并补充各字段 `@Schema`。

2. **统计业务逻辑收敛 (`AdminImageServiceImpl.java`)**：
   - 在 `getStatistics()` 方法中移除对 `dockerRegistryClient.getPublicRegistryHost()` 和 `formatBytes(totalSizeBytes)` 的冗余调用；
   - 仅保留基于中心 Registry 镜像数据的聚合计算（总数、赛题镜像数、基础镜像数、总字节数 `totalSizeBytes`、Registry 在线连通状态 `registryOnline`），实现纯粹的数值统计。

### 2.2 前端工程改造 (Vue 3 + TypeScript)

1. **类型定义 1:1 对齐 (`src/types/image/index.ts`)**：
   - 严格根据后端 `AdminClusterImageStatisticsVO` 更新 TypeScript 接口定义，剔除 `totalSizeFormatted`、`totalNodesCovered`、`syncingImages` 以及 `registryPublicHost` 字段。

2. **自适应字节单位换算工具 (`src/utils/parser.ts`)**：
   - 在前端通用解析工具库中新增 `formatBytes(bytes?: number | null, decimals = 1): string` 方法；
   - 支持根据字节数值自适应计算阶数，准确转换并格式化为 `B`、`KB`、`MB`、`GB`、`TB` 人类可读字符串，当字节数值为 0 或空值时安全返回 `'0 B'`。

3. **镜像管理视图重构 (`src/views/admin/image/AdminImageView.vue`)**：
   - **统计概览 Ribbon**：
     - 使用 `computed` 属性 `formattedTotalSize` 实时调用 `formatBytes(statistics.totalSizeBytes)` 进行响应式转换；
     - 彻底移除 Registry 在线状态标签后附带的 `({{ statistics.registryPublicHost }})` / `(127.0.0.1:5001)` 提示角标；
   - **镜像入库拉取弹窗 (Pull Modal)**：
     - 移除 `registryNotice` 的 `{host}` 动态占位入参；
   - **镜像详情抽屉 (Detail Drawer)**：
     - 将托管仓库位置标签由 `中心私有 Registry (127.0.0.1:5000)` 简化为清晰干净的 `中心私有 Registry`；
   - **状态与样式清理**：
     - 在 `statistics` 响应式对象中移除被废除的字段；
     - 清理 SCSS 样式表中无用的 `.registry-host-hint` 类规则。

4. **国际化语言包更新 (`src/locales/{zh_CN,en_US}/admin/images.json`)**：
   - 同步修正中英文语言包中的 `admin.images.pullModal.registryNotice`，剔除原有的 `({host})` 占位符，保持文案语义通顺简洁。

---

## 3. 安全隐患说明与影响范围

- **数据一致性与单一职责**：单位换算移交前端保证了后端 API 返回的数据类型严格为纯净数字，避免了因国际化、数值精度或客户端不同展示需求导致的后端格式化耦合。
- **配置泄露防护**：统计接口不再无序附带宿主机公开或内部 IP 端口信息（如 `127.0.0.1:5001`），降低了底层容器网络拓扑的非必要暴露面。
- **影响范围**：
  - 后端：`volur-container` 模块中的镜像概览统计接口 `/api/v1/admin/images/statistics`；
  - 前端：镜像管理控制台 `AdminImageView.vue`、镜像类型定义 `src/types/image/index.ts`、通用解析工具 `src/utils/parser.ts` 及镜像国际化语言包。
- **验证结论**：后端 Maven 模块编译及单元测试全部 PASS，前端 Vite 全量打包构建与 TypeScript 静态类型检查 100% 通过。
