# 施工与审计记录：Git 变更深度分析、平台编译验证与本地仓库全量提交归档

## 1. 基本信息

- **任务名称**：Git 变更深度分析、全平台编译与测试验证、敏感数据防护与本地仓库全量提交归档
- **执行时间**：2026-09-15
- **操作范围**：
  - 后端架构与领域服务：[`volur-lite-platform`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform)（覆盖 12 个子模块）
  - 前端工程与组件生态：[`volur-lite-view`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view)
  - 架构设计与审计记录：[`agent/volur-design-md/`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-md)
  - 版本控制归档：本地 Git 仓库全量变更归档与提交（Git Commit）

---

## 2. 变更背景与演进脉络

自 2026-09-10 提交 `752e444`（`feat(platform,view): add cluster orchestration, async task center, email audit, unified security whitelist, and view management`）以来，Volur Lite 平台在竞赛体系、容器与云原生集群编排、Java 25 虚拟线程基础设施、前端通用组件生态化及数据安全防护等领域展开了连续的多轮重构与技术升级。

在执行本次全量提交归档前，我们对工作区内所有暂存（Staged）与未暂存（Unstaged）的改动进行了拉网式审计，确保无遗留语法错误、类型缺陷或测试失败，阻断任何敏感信息入库，并规范化统一提交。

---

## 3. Git 变更核心内容深度解析

本次提交共涵盖 280 余个文件的更新与新增（含 40 余篇架构设计与审计文档），主要收敛在以下五大核心演进维度：

### 3.1 竞赛全生命周期管理与数据安全脱敏 (volur-competition & 前端竞赛视图)
1. **竞赛参赛队伍隐私数据脱敏与鉴权加固**：
   - 在 [`CompetitionTeamController`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) 的 `listTeams` 接口中引入 `@CurrentUserId` 注解，由框架层在未登录时 Fail-Fast 拦截并返回 `401 Unauthorized`；
   - 在 [`CompetitionTeamServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 中实现细粒度数据脱敏：队长与领队可查看本队明文数据，队员查看队友隐私信息（身份证、手机号、邮箱）进行标准化掩码脱敏，外部访客完全隐藏敏感信息（置为 `null`）；
   - 补充 `CompetitionTeamControllerTest` 与 `CompetitionTeamServiceImplTest` 完备单元测试。
2. **竞赛业务模型与流程重构**：
   - 规范竞赛生命周期（草稿、已发布、报名中、进行中、已结束、已归档），支持未发布草稿 404 防越权；
   - 引入竞赛自定义 Slug 别名访问支持；
   - 重构队伍报名流程为表格化管理，移除历史冗余字段（如自定义字段、邀请码等），并在队员变动时触发重新审核（`TeamStatus.PENDING`）。
3. **前端竞赛交互体系升级**：
   - **竞赛列表与详情**：重构 [`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 为单行紧凑卡片，支持倒计时与 Markdown 描述渲染；重构 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) SPA 标签页与面包屑导航；
   - **竞赛竞技场**：新增 [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue)（题目大厅、容器实例拉起、Flag 提交、排行榜）；
   - **管理后台深度拆分**：将历史巨型视图解耦拆分为 [`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue)、[`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 以及 6 个专属 Tab（基本信息、公告、题目、容器、队伍审核、Writeup）。

### 3.2 容器与集群云原生编排深化 (volur-container & 前端集群视图)
1. **Docker Registry v2 深度集成**：
   - 实现强类型 [`DockerRegistryClient.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/client/DockerRegistryClient.java)，直连镜像仓库拉取 Manifest、Catalog、Tags 及分层信息；
   - 彻底移除废弃的 `AdminImageTypeUpdateDTO` 与手动镜像类别修改逻辑，转向以 Registry 为真实源的管理模型；
   - 升级 `ClusterImageMapper`，引入 Java Record 传输模型。
2. **容器网络与端口冲突自愈分配**：
   - 新增 [`ContainerPortAllocator.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/util/ContainerPortAllocator.java)，在宿主机与集群现有端口集合中进行原子探测与动态可用端口分配；
   - 增强 [`K3sRuntimeStrategy`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java) 与 [`DockerSwarmRuntimeStrategy`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java)：支持容器 Host 端口模式映射、NodePort 范围边界防护校验及超限友好错误提示；
   - 支持题目容器 sudoer 权限提权设置。
3. **前端集群管理拆分**：
   - 将 [`AdminClusterSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue) 解耦拆分为参数配置（`ParamsTab`）、节点拓扑（`NodesTab`）及架构总览（`TopologyTab`）。

### 3.3 平台底座虚拟线程解耦与架构演进 (volur-framework & volur-server)
1. **Java 25 虚拟线程池架构解耦**：
   - 新增 [`AsyncConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/AsyncConfig.java)：提供基于虚拟线程的异步任务执行器，集成 `AsyncContextTaskDecorator` 维持安全上下文穿透；
   - 新增 [`SchedulingConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/SchedulingConfig.java)：实现独立的定时任务调度器并配置完备单元测试；
   - 构建 [`VolurThreadManager.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/thread/VolurThreadManager.java) 线程指标度量与资源监控。
2. **全局通用脱敏工具类**：
   - 提取 [`DesensitizationUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/DesensitizationUtil.java)，支持手机号、身份证、邮箱、姓名、银行卡及密钥的标准化掩码处理；
   - 补充 [`DesensitizationUtilTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/util/DesensitizationUtilTest.java) 完备覆盖测试。
3. **系统配置与 Jackson 3 兼容**：
   - 清理所有历史大写下划线冗余配置键，统一推行规范的点分层级键（`system.*`）；
   - 增强 Jackson 序列化配置以兼容 Java 25 新特性与时间/Record 结构。

### 3.4 前端通用组件库提取与设计规范收敛 (volur-lite-view)
1. **通用组件库提取**：
   - [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)：标准化全站管理表格的卡片外壳、分页器、高度自适应与操作栏粘性定位，并在全站 10 余个管理界面统一替换接入；
   - [`VolurSidebar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue)：抽取独立侧边栏导航组件，集成路由激活态判断与折叠动画；
   - [`AppAvatar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppAvatar.vue)：通用头像组件，支持图片加载失败平滑降级与首字母彩色头像；
   - [`MarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/MarkdownEditor.vue) 与 [`markdown.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/markdown.ts)：封装基于 marked + DOMPurify 的轻量 Markdown 编辑与安全渲染套件。
2. **多语言与样式统合**：
   - 全面更新中英文国际化资源包（`zh_CN` / `en_US`），覆盖集群、竞赛、容器、镜像与通用词条；
   - 新增前端轻量容器化部署支持：交付 [`Dockerfile`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/Dockerfile)、[`.dockerignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/.dockerignore) 与生产级 [`nginx.conf`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/nginx/conf/nginx.conf)。

---

## 4. 验证与审计结果

| 验证项 | 验证命令 | 验证结果 | 状态 |
| :--- | :--- | :--- | :---: |
| **后端编译检查** | `mvn test-compile` | 12 个模块全部编译通过，0 错误 | PASS |
| **后端单元测试** | `mvn test` | 包含脱敏工具、并发管理、控制器鉴权在内的全部用例通过 | PASS |
| **前端类型检查与打包** | `npm run build` (`vue-tsc && vite build`) | TypeScript 类型 100% 通过，产物构建成功 | PASS |
| **敏感信息审查** | `git diff` 检索私钥/证书/密钥/token | 无任何机密数据外溢，`data/` 目录隔离良好 | PASS |

---

## 5. 本地提交规划 (Commit Message)

```git
feat(competition,container,framework,view): overhaul competition lifecycle, registry integration, virtual threads, and unified component ecosystem

- competition: overhaul single-competition lifecycle, tabular team registration, team member privacy desensitization, and decompose admin views into dedicated tabs
- container: integrate Docker Registry v2 API for layer inspection, implement dynamic port allocator with conflict avoidance, and enforce K3s NodePort range guards
- framework: introduce Java 25 virtual thread async/scheduling decoupling, create unified DesensitizationUtil, and modernize Jackson configurations
- view: extract AppTableCard, VolurSidebar, AppAvatar, and MarkdownEditor components; harmonize admin views with unified pagination and full zh_CN/en_US i18n
- devops: add frontend production Dockerfile and Nginx configuration; preserve comprehensive architectural audit trails
```
