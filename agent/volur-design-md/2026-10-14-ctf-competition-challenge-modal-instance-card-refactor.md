# CTF 赛事题目详情弹窗靶机环境卡片重构文档

## 1. 变更背景
在 CTF 赛事题目详情弹窗（`CtfCompetitionChallengeModal.vue`）中，原有的动态靶机容器运行信息展示卡片存在以下待优化交互与排版问题：
1. 第一行靶机连接信息存在冗余的标签文字“靶标网络连接:”及独立的复制按钮，视觉重心分散；
2. 第二行操作按钮使用了自定义样式的原生 `<button>`，未对齐项目全局通用设计组件规范（`VBtn.vue`），且色彩语义不够统一；
3. 容器剩余租期提示文案为“租期剩余”，与系统整体的容器资源生命周期术语（“容器剩余”）存在语义差异。

## 2. 改动详情

### 2.1 靶机卡片首行排版与 URL 居中
- 移除了第一行的“靶标网络连接:”标签文字与右侧的复制按钮（包括未再使用的 `CopyOutline` 图标导入及 `copyHostPort` 辅助函数）；
- 将容器连接地址（`instanceConnectionAddress`，如 `http://10.145.251.94:26968`）在第一行居中显示（`flex items-center justify-center text-center font-mono font-semibold text-google-blue dark:text-google-blue-dark`）；
- 增加了 `select-all` 与 `break-all` 属性，方便参赛选手一键点击快速选中连接串，且防止窄屏下长域名溢出。

### 2.2 操作按钮重构与组件对齐
- 将第二行原有的靶机续期与销毁原生按钮替换为平台通用 `@/components/common/VBtn.vue` 组件：
  - 续期按钮：采用 `VBtn` 并配置 `clz="primary"` 与 `size="small"`，绑定 `handleExtendInstance`；
  - 销毁容器按钮：采用 `VBtn` 并配置 `clz="warning"` 与 `size="small"`，绑定 `handleStopInstance`；
- 两侧按钮均保持外层 `VTooltip` 悬浮提示，并在 `instanceActionLoading` 状态下受控禁用。

### 2.3 术语标准化与国际化（i18n）更新
- 将“租期剩余”修改为“容器剩余”：
  - `src/locales/zh-CN/competition.json`: `instance_remaining_time` 从“租期剩余”调整为“容器剩余”；`instance_stop` 调整为“销毁容器”；
  - `src/locales/zh-TW/competition.json`: `instance_remaining_time` 从“租期剩餘”调整为“容器剩餘”；`instance_stop` 调整为“銷毀容器”；
  - `CtfCompetitionChallengeModal.vue`: 模板 fallback 文案同步更新为 `容器剩余` 与 `销毁容器`。

## 3. 影响范围
- `src/views/competition/components/CtfCompetitionChallengeModal.vue`
- `src/locales/zh-CN/competition.json`
- `src/locales/zh-TW/competition.json`

## 4. 验证结果
- 执行前端完整构建与类型检查（`vue-tsc --noEmit && vite build`），全部通过且无任何类型错误与警告。
