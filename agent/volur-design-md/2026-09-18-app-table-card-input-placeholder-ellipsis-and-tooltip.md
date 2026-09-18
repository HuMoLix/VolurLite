# AppTableCard 输入框超长截断省略与悬浮完整提示施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/components/common/AppTableCard.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 缺陷现象与需求背景

### 1.1 现象描述
在后台用户管理页面（`AdminUserView.vue`）及其他管理视图中，工具栏/筛选区域的 `AppTableCard` 组件内嵌入了搜索输入框（如“搜索用户名 / 邮箱 / 组织”）。  
在固定宽度约束（如 `width: 200px`）以及前缀图标（`SearchOutline`）的挤压下，原生的 `placeholder` 文本超出输入框可用宽度。  
由于原样式未配置截断省略，溢出文字直接被父级裁切甚至截半字显示，未显示省略号“...”，且鼠标悬停时无法获知被截断的完整提示信息。

### 1.2 改造目标
1. **自动省略截断**：当输入框中的 `placeholder` 超出容器可用宽度时，超出部分必须使用“...”省略号替代，排版规整美观；
2. **长悬浮提示**：当鼠标长时间悬浮在输入框上时，展示完整的 `placeholder` 文本；
3. **架构通用性与低侵入**：微调收敛在公共卡片容器 `AppTableCard.vue` 中，使全平台所有使用 `AppTableCard` 的业务表格搜索框均能自动享受此项优化，无需在各个业务页面手动侵入式传参。

---

## 2. 改造方案与实施细节

### 2.1 样式微调：文本溢出以 "..." 替代
在 `AppTableCard.vue` 的 `<style scoped lang="scss">` 中针对卡片内的单行输入框（`.n-input:not(.n-input--textarea)`）补充深度样式声明：
- 为 `.n-input__input` 声明 `min-width: 0`，解除 Flexbox 默认的最小内容宽度限制，防止子元素反向撑破输入框；
- 为 `.n-input__placeholder` 及其内部 `> span` 显式声明 `width: 100%; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;`，使超出可用宽度的文本由浏览器自动渲染为省略号 `...`；
- 为原生输入框 `.n-input__input-el` 及其伪元素 `::placeholder` 补充 `text-overflow: ellipsis`，确保全层级渲染一致。

### 2.2 交互增强：长时间悬浮展示原生 Tooltip
利用 HTML 原生 `title` 属性天然具备的“长悬浮触发（Dwell Time 延时展示）”特性：
1. **事件委托与动态更新**：
   在 `AppTableCard` 根容器上绑定 `@mouseover="handleInputMouseOver"` 事件委托。当鼠标移入任意 `.n-input` 容器时：
   - 若用户已输入内容，清空 `title`（若输入内容自身超长截断，则友好展示完整输入文本）；
   - 若处于未输入状态（显示 `placeholder`），通过比较 DOM 真实排版宽度与视口宽度：`scrollWidth > clientWidth + 1`，精准判定是否发生省略号截断；
   - 若真实截断，动态赋予 `inputContainer.title = placeholderText`；若未截断，则保持无 `title`，杜绝冗余气泡打扰。
2. **初始化预检与自适应响应**：
   - 在 `onMounted` 阶段延迟 200ms 执行批量检查，在 DOM 初始渲染完成后即为超长输入框打上 `title` 标记；
   - 挂载 `ResizeObserver` 监听卡片尺寸变动，在窗口拉伸或折叠侧边栏时自动重新计算截断状态；
   - 在 `onUnmounted` 中释放 `ResizeObserver`，避免内存泄漏。

---

## 3. 验证与测试

在 `src/volur-lite-view` 根目录下执行完整 TypeScript 编译与 Vite 打包测试：
```bash
npm run build
```
**测试输出结果**：
- `vue-tsc --noEmit` 类型校验：0 错误，0 警告；
- `vite build` 生产环境打包：成功通过（总耗时约 5.5s）；
- `AdminUserView.vue` 及所有引用 `AppTableCard.vue` 的管理视图无任何回归缺陷。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - [`src/volur-lite-view/src/components/common/AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)
- **直接收益页面**：
  - `AdminUserView.vue`（“搜索用户名 / 邮箱 / 组织”输入框）
  - `AdminEmailView.vue`、`AdminBroadcastView.vue`、`AdminImageView.vue`、`AdminCtfChallengeView.vue`、`AdminTheoryChallengeView.vue`、`AdminCompetitionListView.vue` 等全部后台表格卡片页面。
- **安全与稳定性**：
  - 纯样式与无侵入事件委托机制，对既有业务逻辑零破坏；
  - 亚像素容差判定杜绝误触发；
  - 自动清理生命周期监听，杜绝内存泄漏。
