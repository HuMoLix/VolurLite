# 提取 VCompetitionCard 组件并实现基于 SliceResponse 的无限滚动切片分页

## 1. 变更背景与需求目标

在 Volur Lite NEXT 前端工程中，原 `CompetitionListView.vue` 竞赛列表视图将卡片渲染内联在视图中，且依赖传统的全量分页器（`PageResponse` + `n-pagination`）。为了：
1. **组件解耦与视图瘦身**：将单个竞赛卡片解耦并提取为独立的平台通用竞赛卡片组件 `VCompetitionCard.vue`，存放于 `@/components/competition/` 并统一导出；
2. **优化排版布局**：根据 Google Material Design 3 色彩与设计规范对竞赛卡片进行全方位现代重构，优化封面图比例、浮动标签、标题层级、比赛与报名起止时间排版、短链接一键复制及悬停微动效 CTA 按钮；
3. **切片流式翻页与无限滚动**：利用后端的 `SliceResponse<T>` 响应模型，实现前台竞赛广场当用户下滑至底部时自动触发下一页切片数据加载（每次拉取 5 条，免去 `COUNT(*)` 开销，提升并发性能）。

---

## 2. 核心架构与代码变更

### 2.1 后端工程变更 (`volur-competition` & `volur-framework`)
- **服务层接口与实现 (`CompetitionService` & `CompetitionServiceImpl`)**：
  - 新增 `listCompetitionSlice(Integer page, Integer size, String keyword, CompetitionType type)` 方法，基于 JPA Specification 与 `Pageable` 进行切片检索；
  - 检索结果通过 `SliceResponse.of(pageResult)` 构造标准切片模型响应，杜绝底层泄露。
- **控制器层 (`CompetitionController`)**：
  - 新增端点 `GET /api/v1/competitions/slice`，默认参数 `page = 1, size = 5`，支持可选参数 `keyword` 与 `type`；
  - 统一返回 `ApiResponseEntity<SliceResponse<CompetitionSummaryVO>>`，小写驼峰工厂方法 `ApiResponseEntity.ok(...)`；
  - 保留原有 `GET /api/v1/competitions` 传统全量分页接口以确保历史版本向后兼容。
- **单元测试验证 (`CompetitionControllerTest`)**：
  - 新增 `testListCompetitionSliceRoute` 针对切片路由进行 MockMvc 验证，确保响应结构与 HTTP 状态码 200 吻合。

### 2.2 前端 API 与类型对齐 (`src/volur-lite-view-NEXT`)
- **类型定义 (`src/types/competition/index.ts`)**：
  - `CompetitionQueryDTO` 扩展可选字段 `type?: CompetitionType | string`，支持根据竞赛类型进行切片筛选。
- **请求客户端 (`src/api/competition.ts`)**：
  - 新增 `getCompetitionSliceApi(params?: CompetitionQueryDTO)`，严格 1:1 对齐后端 `/competitions/slice` 路径，返回强类型 `ApiResponse<SliceResponse<CompetitionSummaryVO>>`。

### 2.3 提取并重构 `VCompetitionCard.vue` 组件
- **组件路径**：`src/components/competition/VCompetitionCard.vue`
- **导出收敛**：`src/components/competition/index.ts` 导出 `VCompetitionCard`。
- **排版布局优化点**：
  - **容器结构**：外层自适应 Flex 栅格（桌面端行布局、移动端列布局），表面应用纯白卡片（暗色 `#292a2d`），辅以细腻边框与上浮悬停阴影；
  - **封面与降级处理**：支持响应式封面图，图片应用优雅缩放过渡动效；未配置封面图时采用现代渐变背景与奖杯图标占位；
  - **标签体系**：状态徽章根据竞赛状态智能着色（进行中呼吸绿点、报名中蓝底、封榜黄底、归档沉稳灰），赛制胶囊与短链复制药丸微卡；
  - **时间排版**：区分比赛时间与报名时间，结合 Google 图标与等宽字体，消除视觉杂乱；
  - **CTA 行动点**：状态自适应文案（"进入竞赛" / "报名中" / "查看详情"），配合向右微移的平滑箭头交互。

### 2.4 竞赛列表视图重构 (`CompetitionListView.vue`)
- **替换卡片与移除传统分页器**：完全移除了旧版 `<n-pagination>`，直接消费 `<VCompetitionCard>` 组件。
- **IntersectionObserver 无限滚动**：
  - 引入底部哨兵元素 `<div ref="sentinelRef" class="h-4 w-full"></div>`；
  - 使用 `IntersectionObserver` 监听哨兵可见性，在接近视口底部（rootMargin: 100px）时自动无缝加载下一片（size = 5）；
  - 状态完整反馈：包含首屏骨架/进度条、滚动中微型 Spinner、无侵入底栏静默衔接及空数据占位（已移除“已加载全部竞赛”文字提示，保持底栏极致简洁）。

---

## 3. 规范遵循与代码纯净度核查

1. **文档外置与零注释铁律**：所有新增与重构代码（`.java`, `.vue`, `.ts`）中不包含解释性行内注释与冗余说明，所有考量集中记录于本文档；
2. **Fail-Fast & Zero-Mock 原则**：所有流程由真实 SliceResponse 数据模型驱动，无前端假数据伪造；
3. **主题与视觉规范**：100% 遵照 Google Material Design 3 / Workspace 色彩体系，杜绝全黑刺眼正文；
4. **编译与测试验证**：
   - 后端 Maven 测试 `CompetitionControllerTest` 全部通过（BUILD SUCCESS）；
   - 前端 `vue-tsc --noEmit && vite build` 生产构建成功通过。
