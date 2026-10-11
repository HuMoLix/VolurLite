# 2026-10-12 CompetitionDetailView 导航与独立 Banner 渲染重构记录

## 1. 变更背景与需求概述
在 `src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue` 中，用户对顶部交互细节与 Banner 呈现方式提出了进一步优化需求：
1. **返回按钮视觉定制**：将“返回竞赛列表”按钮背景色调整为透明，文字颜色设为 `#8c8c8c`（日间浅色模式下为 `#565b61`），悬停 hover 状态下背景色为 `#2a2a2a`、文字颜色为 `#d4d4d4`；
2. **精简操作区**：移除顶部右侧冗余的“复制短链接”与“分享”按钮；
3. **Banner 独立与完整展示**：移除原先包裹 Banner 图片且包含内边距与固定高度裁切的容器 Card，将 Banner 图片重构为独立渲染单元，并使用 `object-contain` 消除裁切，完整展现主办方横幅图片。

---

## 2. 核心架构与样式重构详情

### 2.1 返回按钮无边框悬停动效
- **类名封装**：`.back-btn`
- **样式规范**：
  - 常态：`background-color: transparent; border: none; outline: none; font-size: 13px; font-weight: 500; cursor: pointer;`
  - 日间模式文字颜色：`color: #565b61;`
  - 夜间模式文字颜色：`color: #8c8c8c;`
  - 悬停动效（日间模式）：`background-color: #efefef; color: #1b1b1e;`
  - 悬停动效（夜间模式）：`background-color: #2a2a2a; color: #d4d4d4;`
  - 内部 `:deep(.n-icon)` 继承 `currentColor`，保持图标与文本颜色同步过渡。

### 2.2 顶部栏排版与死代码清理
- 移除顶部右侧的“复制短链接”与“分享”按钮容器；
- 清理 `<script setup>` 中不再调用的 `copyShortLink()` 与 `handleShare()` 函数；
- 清理 `@vicons/ionicons5` 中未使用的 `CopyOutline`、`ShareSocialOutline`、`TrophyOutline`、`CalendarOutline`、`TimeOutline` 图标导入；
- 将赛事主标题（`competition.title`）、运行状态标签（`status` 胶囊徽章）与赛制类型标签（`type` 胶囊徽章）紧凑对齐至顶部导航栏返回按钮右侧，形成清晰直观的上下文指引。

### 2.3 独立 Banner 盒子渲染 (Full-Aspect Banner Box)
- **类名封装**：`.comp-banner-box` 与 `.comp-banner-img`
- **呈现逻辑**：
  - 彻底解耦原包含标题、时间与网格的外层包裹 Card；
  - 采用独立的圆角容器：`width: 100%; border-radius: 16px; overflow: hidden;`
  - 图片自适应完整渲染：`width: 100%; height: auto; display: block; object-fit: contain;`，杜绝任何纵向 `max-height` 强制裁剪与拉伸，确保主办方横幅全比例完整呈现。

---

## 3. 规范执行与验证

- **零注释铁律**：所有修改的代码文件保持纯净自解释，未编写冗余行内解释性注释。
- **类型安全与构建验证**：
  - 执行 `npx vue-tsc --noEmit`：0 错误，严格类型校验全量通过；
  - 执行 `npm run build`：生产环境打包成功（`✓ built in 14.77s`）。
