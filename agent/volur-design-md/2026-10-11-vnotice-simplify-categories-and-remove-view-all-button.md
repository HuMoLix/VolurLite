# VNotice 简化分类与移除“前往通知中心”按钮审计记录

## 1. 变更背景与需求
在 `volur-lite-view-NEXT` 项目中，根据用户交互优化指令，需要对 `src/components/sidebar/VNotice.vue` 站内通知弹层卡片进行视觉与分类的精简优化：
1. 移除卡片底部栏的“前往通知中心”操作按钮（仅保留通知总数统计）；
2. 移除卡片顶部的二级分类药丸标签（“竞赛演练”、“系统”、“组队”），仅保留核心的“全部”与“未读”两个基础分类。

---

## 2. 核心改动点

### 2.1 模板与逻辑层精简 (`src/components/sidebar/VNotice.vue`)
- **底部操作区精简**：
  - 在 `.card-bottom-layer` 中彻底移除了 `<button class="notice-view-all-btn">` 及其子元素（文案“前往通知中心”与 `ChevronForwardOutline` 图标）；
  - 保留 `.notice-total-text`（“共 X 条通知”）；
  - 从 `@vicons/ionicons5` 导入中清理了未使用的 `ChevronForwardOutline` 图标；
  - 移除了未使用的 `handleViewAll` 方法以及 `defineEmits` 中的 `'view-all'` 事件定义。
- **分类标签精简**：
  - 更新 `filterTabs` 计算属性，彻底移除 `COMPETITION`（竞赛）、`SYSTEM`（系统）、`TEAM`（组队）分类；
  - 仅保留 `{ key: 'ALL', label: t('common.all', '全部') }` 与 `{ key: 'UNREAD', label: t('common.unread', '未读') }`；
  - 保持未读状态计数红点标记 (`tab-count-dot`) 与列表拉取防抖联动正常工作。

### 2.2 样式层清理 (`<style scoped lang="scss">`)
- 移除了亮色模式下 `.notice-view-all-btn` 的完整样式规则；
- 移除了暗黑模式下 `.notice-view-all-btn` 的高亮文本与悬浮背景规则；
- 保持底部 `.card-bottom-layer` 分割线与排版结构整洁紧凑。

---

## 3. 验证与影响范围

1. **构建与语法验证**：
   - 执行 `vue-tsc --noEmit && vite build`，编译打包在 9.03 秒内完成，无任何 TS 诊断错误与模板警告。
2. **规范遵守**：
   - 代码文件严格遵守零注释铁律；
   - 保持了对已提取的 `VCard` 基底组件的完整复用。
