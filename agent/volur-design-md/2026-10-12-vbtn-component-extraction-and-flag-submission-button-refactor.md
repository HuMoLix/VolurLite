# 通用按钮组件 VBtn 研发与 Flag 提交交互及导入按钮样式重构方案

## 1. 变更背景与目标

为规范与统一 Volur Lite NEXT 前端工程中各类操作按钮的视觉语言与交互规范：
1. **CTF Flag 提交输入与按钮对齐**：
   - 在 CTF 赛题详情弹窗（`CtfCompetitionChallengeModal.vue`，由点击 `CtfCompetitionChallengeCard.vue` 展开）中，原先的 Flag 提交输入框为原生 `<input>`，需替换为平台通用的基础输入框组件 `VInput.vue`；
   - 提交 Flag 按钮原先采用局部圆角样式，需全面对齐 `VCompetitionCard.vue` 中“进入竞赛”按钮的高辨识度 Google Blue 胶囊药丸风格；
2. **抽象通用基础按钮组件 `VBtn.vue`**：
   - 在 `@/components/common` 下提炼通用按钮组件 `VBtn.vue`，原生支持 `leftIcon` 与 `rightIcon` 图标插槽/属性；
   - 支持通过 `clz` 属性控制按钮配色主题：
     - `clz = 'primary'`：对齐 `VCompetitionCard.vue` 中“进入竞赛”的高辨识度主品牌色实体胶囊按钮；
     - `clz = 'normal'`：对齐 `CompetitionListView.vue` 中“导入竞赛”的极简线框浅色交互按钮；
3. **视图调用处无缝对接**：
   - `CtfCompetitionChallengeModal.vue` 中的提交 Flag 按钮接入 `VBtn`（`clz="primary"`）；
   - `VCompetitionCard.vue` 中的“进入竞赛”按钮重构为使用 `VBtn`（`clz="primary"` 与 `rightIcon`）；
   - `CompetitionListView.vue` 中的“导入竞赛”按钮重构为使用 `VBtn`（`clz="normal"`），并清理上百行冗余的 Naive UI `<n-button>` 样式穿透覆写。

---

## 2. 核心改动点与技术实现

### 2.1 基础按钮组件 `VBtn.vue` 研发
- **文件路径**：`src/components/common/VBtn.vue`；
- **类型定义**：在 `src/components/common/types.ts` 中定义 `VBtnClz = 'primary' | 'normal'`、`VBtnSize = 'small' | 'medium' | 'large'` 及 `VBtnProps` 接口；
- **组件集中导出**：在 `src/components/common/index.ts` 中导出 `VBtn`；
- **核心功能与视觉规则**：
  - **`clz = 'primary'`**：
    - 亮色背景采用 Google Blue `#1a73e8`，Hover `#1765cc`，Active `#1557b0`，文字为纯白；暗色背景采用 Google Blue Dark `#a8c7fa`，深曜炭黑文字 `#1e1f20`，无边框微阴影（`shadow-sm`）；
    - 默认采用胶囊全圆角 `rounded-full` 与高度 40px（`h-10 px-5`），内敛紧凑；
  - **`clz = 'normal'`**：
    - 亮色背景透明 `bg-transparent`，细微边框 `#d7d8da`，正文文本 `#2b2d31`，Hover 背景微灰（`bg-black/[0.04]`）；暗色背景透明，细微边框 `#333`，正文文本 `#fcfcfc`，Hover 背景浅灰透明（`bg-white/[0.05]`）；
    - 默认采用 12px 圆角 `rounded-xl` 与高度 36px（`h-9 px-4`）；
  - **双向图标布局**：
    - 支持通过属性 `:left-icon` / `:right-icon` 或具名插槽 `#left-icon` / `#right-icon` 灵活插入图标；
    - 支持 `:icon-size` 定制或根据按钮 `size` 自动计算（small: 14px, medium: 16px, large: 18px）；
  - **加载态与禁用控制**：
    - `loading === true` 时自动以旋转图标替换左侧图标，并自动禁用点击事件；
    - `disabled === true` 或 `loading === true` 时半透明置灰（`opacity-50 cursor-not-allowed`）并阻断原生事件。

### 2.2 Flag 提交交互与样式对齐
- **文件路径**：`src/views/competition/components/CtfCompetitionChallengeModal.vue`；
- **输入框升级**：将原原生 `<input class="flag-input">` 替换为 `<VInput>`：
  ```vue
  <VInput
    v-model:value="flagInput"
    :placeholder="t('competition.flag_input_placeholder')"
    clearable
    size="large"
    :radius="12"
    class="flex-1"
    @keydown.enter="handleFlagSubmit"
  />
  ```
- **提交按钮升级**：将原手写 `<button>` 升级为通用 `<VBtn>`：
  ```vue
  <VBtn
    clz="primary"
    :disabled="flagSubmitting || !flagInput.trim()"
    :loading="flagSubmitting"
    class="flex-shrink-0"
    @click="handleFlagSubmit"
  >
    {{ flagSubmitting ? t('common.submitting', '校验中...') : t('competition.submit_flag', '提交 Flag') }}
  </VBtn>
  ```

### 2.3 赛事卡片进入按钮迁移至 VBtn
- **文件路径**：`src/components/competition/VCompetitionCard.vue`；
- 将原本手写内联的 Tailwind `<button>` 替换为：
  ```vue
  <VBtn
    clz="primary"
    :right-icon="ArrowForwardOutline"
    @click.stop="handleClick"
  >
    {{ actionButtonText }}
  </VBtn>
  ```

### 2.4 赛事大厅导入按钮迁移与冗余样式瘦身
- **文件路径**：`src/views/competition/CompetitionListView.vue`；
- 将原 `<n-button class="competition-btn competition-import-btn">` 替换为：
  ```vue
  <VBtn
    clz="normal"
    class="competition-import-btn"
    @click="handleImportClick"
  >
    {{ t('competition.import_competition') }}
  </VBtn>
  ```
- 清理冗余样式：
  - 彻底移除了原针对 Naive UI `<n-button>` 编写的上百行内部样式覆写规则（`:deep(.n-button__content)`、`:deep(.n-button__border)`、`:deep(.n-base-wave)` 等）；
  - 简化 `.competition-import-btn` 仅保留居中边距 `margin: 0 auto;`，移除与普通边框冲突的 `border: none !important;`。

### 2.5 “导入竞赛”按钮样式差异归因分析与像素级校准
1. **边框渲染机制失效（最直观的外观落差）**：
   - 本项目 `tailwind.config.ts` 配置了 `corePlugins: { preflight: false }` 以保护 Naive UI 基础组件外观；
   - Tailwind 的 `.border` 类在编译后**仅生成 `border-width: 1px;`**，原本依赖 Preflight 注入 `border-style: solid;`；
   - 在原生 `<button>` 标签且缺少 Preflight 的环境下，浏览器 User-Agent 样式表默认生效为 `border-style: outset`，直接导致原本平整细腻的 1px 实线线框呈现出立体凸起/凹陷的不自然质感；
2. **字体与字符渲染落差（Inter vs. Lato）**：
   - 原 `.competition-btn` 显式强制为 `font-family: Inter, sans-serif !important;` 并锁定 `line-height: 20px !important;`；
   - 初版 `VBtn` 使用了 Tailwind `font-sans`，而工程配置将其映射为 `Lato`，导致字形轮廓与字重呈现出明显偏差；
3. **尺寸与内边距参数（Padding & Gap）**：
   - 原样式规格为 `height: 36px; padding: 0 12px; gap: 6px; border-radius: 12px;`；
   - 初版 `VBtn` 计算为 `px-4`（16px）及 `gap-2`（8px），导致按钮两侧横向留白增大约 33%，整体偏宽显胖；
4. **悬浮色精准对齐（#EAEAEB）**：
   - 遵照设计要求，`clz="normal"` 模式下的 Hover 悬浮背景颜色统一指定为 `#EAEAEB`（暗色适配为 `#2a2b2d`）；
5. **像素级架构校准方案**：
   - 将原 `.competition-btn` 的完整纯正 CSS 规范直接沉淀进 `VBtn.vue` 的 `<style scoped lang="scss">`；
   - 彻底锁定 `font-family: Inter, sans-serif;`、`border: 1px solid #d7d8da;`（暗色 `#333`）、`height: 36px; padding: 0 12px; border-radius: 12px; gap: 6px;`，并将日间模式悬浮背景色精确配置为 `#EAEAEB`（暗黑模式 `#2a2b2d`）；
   - 使 `VBtn` 在不需要上层穿透 hack 的前提下，彻底恢复与原设计稿 100% 像素级一致的视觉表现。

---

## 3. 规范审计与验证结果

1. **纯粹代码与零注释原则**：所有新增与重构的 `.vue`、`.ts` 文件经过 `Select-String` 严格检验，未引入任何注释与无用代码；
2. **构建与类型验证**：执行 `npm run build`（包含 `vue-tsc --noEmit` 与 Vite 打包），生产构建用时 8.44 秒，全量通过，0 错误；
3. **样式一致性**：`VBtn` 主次样式与原有设计稿、系统暗黑主题色阶完美吻合。
