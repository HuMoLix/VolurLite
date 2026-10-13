# 竞赛详情视图操作按钮样式统一与通知按钮进阶重构报告

## 1. 变更背景与需求概述

在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中：
1. 竞赛列表视图（`CompetitionListView.vue`）此前已将通用次级操作按钮抽离并确立为标准的 `.competition-btn` 样式类（具有 36px 物理高度、12px 微圆角矩形、14px 字号 / 500 字重、透明底色、Google 风格灰色边框及平滑过渡）；
2. 竞赛详情视图（`CompetitionDetailView.vue`）中，“查看战队信息”按钮原先使用内联 Tailwind 类名（`rounded-full` 全圆角胶囊、`bg-white` 纯白底色、`text-xs` 12px 小字号），导致在浅灰色卡片容器上产生“白补丁”色块冲突，并且与全平台的 12px 几何圆角规范割裂；
3. 相邻的“收到的邀请”通知按钮此前同样采用 `rounded-full` 胶囊与 12px 小字号，两按钮并排时存在明显的几何落差与排版失衡；
4. 本次重构目标：
   - 将“查看战队信息”按钮全面重构为使用 `.competition-btn` 样式类；
   - 进阶升级“收到的邀请”按钮为 `.competition-btn.competition-btn-info`，使两者在尺寸、圆角、字号、间距与交互触控上达成像素级几何统一，同时保留邀请专属的 Google Blue 次级强调视觉。

---

## 2. 核心改动点

### 2.1 样式层：引入 `.competition-btn` 与 `.competition-btn-info`
在 `src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue` 的 `<style scoped lang="scss">` 中补充通用按钮及其信息强调变体：
- **`.competition-btn` 通用次级按钮**：
  - 几何尺寸：`height: 36px !important;`，`padding: 0 12px !important;`，`border-radius: 12px !important;`，`gap: 6px !important;`；
  - 排版规范：`font-family: Inter, sans-serif !important;`，`font-size: 14px !important;`，`font-weight: 500 !important;`，`line-height: 20px !important;`；
  - 色彩与边框：亮色透明背景，默认边框 `#d7d8da`，文字 `#2b2d31`，Hover 背景 `rgba(60, 64, 67, 0.04)`；暗色默认边框 `#333`，文字 `#fcfcfc`，Hover 背景 `rgba(255, 255, 255, 0.04)`；
  - Naive UI 穿透与轮廓重置：重置 outline/box-shadow，抑制点击波纹与状态边框。
- **`.competition-btn-info` 邀请通知按钮进阶变体**：
  - 继承 `.competition-btn` 完全一致的物理高度、12px 圆角、14px 字号与 6px 图标间距；
  - 色彩定制：亮色采用 Google Blue 轻透底 `rgba(26, 115, 232, 0.05)`、边框 `rgba(26, 115, 232, 0.3)`、文字 `#1a73e8`；暗色采用 Google Blue Dark 轻透底 `rgba(138, 180, 248, 0.08)`、边框 `rgba(138, 180, 248, 0.3)`、文字 `#8ab4f8`；
  - 悬停反馈：亮色 `rgba(26, 115, 232, 0.1)`，暗色 `rgba(138, 180, 248, 0.16)`。

### 2.2 结构层：模板 DOM 替换与语义化对齐
在 `CompetitionDetailView.vue` 的战队卡片右侧操作区域：
- 将“收到的邀请”按钮结构升级为：
  ```html
  <button
    v-if="invitations.length > 0"
    type="button"
    class="competition-btn competition-btn-info"
    @click="teamInvitationsModalVisible = true"
  >
    <n-icon :size="16" :component="MailOutline" />
    <span>收到的邀请 ({{ invitations.length }})</span>
  </button>
  ```
- 将“查看战队信息”按钮结构升级为：
  ```html
  <button
    type="button"
    class="competition-btn"
    @click="teamDetailModalVisible = true"
  >
    <n-icon :size="16" :component="EyeOutline" />
    <span>{{ t('competition.view_team_btn', '查看战队信息') }}</span>
  </button>
  ```
- 两个按钮的内部图标尺寸统一由 15px 对齐为 16px，与 14px 文本达成黄金居中对齐。

---

## 3. 安全与质量验证

- **构建与样式编译**：执行 `npx vite build` 顺利通过，SCSS 样式与 Tailwind CSS 无任何编译告警或冲突，产物正常输出；
- **零注释原则合规**：代码文件保持绝对纯净，无任何冗余解释性行内注释，设计推导及规范细节全量外置归档于本文档。
