# VWriteupModal 模态框头部结构、说明卡片布局与输入组件规范重构方案

## 1. 变更背景与需求分析
在 CTF 竞赛 Writeup 提交模态框（`VWriteupModal.vue`）的交互走查与视觉审计中，发现以下几项细节需要进一步对齐平台统一的设计系统与用户体验规范：
1. **模态框头部规范化**：此前模态框采用了隐藏原生 Header（`:show-header="false"`）并在卡片内部首行自建 Flex 行容器的方式实现标题与关闭操作，带有额外的圆底 Icon 和自定义按钮结构，与平台其他标准模态框（如 `CtfCompetitionChallengeModal.vue`、`TeamFormModal.vue`）的 Header 视觉标准不一致。需将模态框标题上移至原生 Header 层，移除标题侧图标，并清除内部手写关闭按钮；
2. **提交说明卡片布局重构**：原“提交说明”区块缺少结构化卡片标题，需重构为明确的 Flex 布局，第一行为标题“提交说明”，其后按行依次展示结构化说明条目；
3. **字重精细化对齐**：“提交结果”与“已提交 0 / 5 次”状态展示行此前采用了 `font-semibold`（600 字重），需调整为常规字重 `font-weight: 400`（`font-normal`），保持文字排版清爽，避免过多视觉干扰；
4. **通用基础组件替换与对齐**：文件选择按钮需收敛使用标准 `VBtn` 组件中的 normal 形态（`clz="normal"`），验证码输入框需统一使用平台通用基础组件 `VInput.vue` 替换原生 input 标签，确保暗色模式交互与边框焦点动效 100% 对齐。

---

## 2. 核心架构设计与改动点

### 2.1 模态框 Header 层重构
- **开启原生 Card Header**：移除 `<n-modal>` 上硬编码的 `:show-header="false"`，配置 `title="提交 Write Up"` 与 `closable` 属性，并在 `@close` 事件中绑定 `handleClose`；
- **清理冗余内部头部 DOM**：彻底移除原来位于卡片内容区顶部的图标（`DocumentTextOutline`）、自绘标题 `h3` 以及附带的自定义圆角关闭 `<button>`；
- **图标依赖精简**：从 `@vicons/ionicons5` 中移除了不再使用的 `DocumentTextOutline` 与 `CloseOutline` 引用。

### 2.2 提交说明卡片 Flex 布局重构
- **卡片容器**：重构为 `flex flex-col gap-2.5` 弹性盒模型结构；
- **卡片首行**：新增第一行标题节点 `<div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary text-xs sm:text-sm">提交说明</div>`；
- **说明条目收敛**：内容区块采用 `flex flex-col gap-2` 纵向流式排版，清晰展示提交时间、提交形式与限次要求等条款。

### 2.3 “提交结果”整体卡片化与布局统一
- **卡片容器统一**：参照上方“提交说明”卡片设计，“提交结果”整体封装入独立的 Card 容器（`flex flex-col gap-2.5 p-3.5 sm:p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 text-xs sm:text-sm`）；
- **卡片首行标题行**：第一行通过 Flex 两端对齐呈现卡片标题“提交结果”（`font-semibold text-google-text-primary dark:text-google-text-darkPrimary text-xs sm:text-sm`）与右侧提交次数徽标（`font-mono text-xs font-normal text-google-text-muted dark:text-google-text-darkMuted`）；
- **内嵌记录表格**：卡片内部按列流式容纳提交历史表格，形成清晰、规整的卡片层级体系。

### 2.4 通用组件接入与替换
- **选择文件按钮**：
  - 去除覆盖高度的 `size="small"`，恢复 `VBtn` 默认中号标准高度（36px），与 `clz="normal"` 的设计系统规范完全对齐；
  - 显式绑定 `:left-icon="AttachOutline"` 属性，使代码结构紧凑优雅；
- **验证码输入框**：
  - 移除原生的 `<input type="text">`；
  - 接入封装的 `VInput` 组件，绑定 `v-model:value="captchaCode"`、`maxlength="6"`、`placeholder="验证码"` 及回车提交事件监听；
  - 验证码图像容器高度调整为 `h-9`（36px），与 `VBtn normal` 及 `VInput` 实现等高视觉对齐。

### 2.5 清除删除按钮默认样式与已上传文件 VTooltip 接入
- **删除按钮默认样式原因分析**：工程因防止破坏 Naive UI 基础组件样式，在 `tailwind.config.ts` 中显式配置了 `corePlugins: { preflight: false }`，即未引入全局 CSS Reset。原代码中的 `<button>` 未声明 `border-none` 与 `bg-transparent`，导致直接回退并继承浏览器原生 User-Agent 预设样式（`border: 2px outset buttonface` 及 `background-color: buttonface` 灰色立体底色）；
- **图标化纯净样式改造**：对删除按钮应用 `bg-transparent border-none p-0 inline-flex items-center justify-center text-google-text-muted hover:text-google-red transition-colors cursor-pointer outline-none shrink-0`，彻底剥离外层背景与边框，仅保留图标本身；
- **VTooltip 统一接入**：
  - 上传选定文件标签：接入 `VTooltip`，在其悬停时完整回显文件名及格式化大小，解决文件名超长截断（`truncate`）问题；
  - 提交结果列表项：对已提交的 Writeup 文件链接与名称统一接入 `VTooltip`，替换原有原生 `:title` 属性，提供一致的 Google Workspace 风格气泡交互。

### 2.6 Writeup 示例下载图标与文本行高对齐修复
- **行高与中英文视觉重心修复**：在下载链接 `<a>` 容器与 `<span>` 上显式声明 `leading-none`（`line-height: 1`），去除由 `sm:text-sm` 默认带来的 `20px` 行高所形成的上下半行距空间；
- **图标尺寸与抗挤压防护**：为 `<n-icon>` 添加 `shrink-0`，确保与 14px 文字在 `inline-flex items-center` 弹性盒模型下实现像素级精准垂直居中对齐。

---

## 3. 安全与规范保障
1. **纯粹代码与零注释铁律**：`VWriteupModal.vue` 严格遵守代码零注释规范，所有设计与变更逻辑收敛沉淀于本文档；
2. **TypeScript 强类型校验**：全量执行 `npx vue-tsc --noEmit` 静态检查，确保零类型错误与零构建缺陷。

---

## 4. 影响范围
- `src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue`
- `agent/volur-design-md/2026-10-13-vwriteup-modal-header-layout-typography-and-components-refactor.md`
