# VSideBar 个人信息 Hover 提示气泡宽度与多行信息卡片重构

## 1. 变更背景与需求说明

在对侧边栏（`VSideBar.vue`）底部个人信息栏与折叠态工具栏的交互重构中，新增需求明确指出：
1. **Tooltip 宽度必须与 userCard 完全一致（等宽对其）**：Tooltip 展开时不能仅依照纯文本 `max-content` 渲染微型气泡，其物理宽度必须与展开态下的 `userCard` 保持 1:1 等宽（默认约 240px，且支持随侧边栏配置响应式对齐）；
2. **多行账号全景信息展示规范**：
   - 第一行：`Volur 账号：displayName`；
   - 第二行：`（username）`；
   - 第三行：`（email）`；
   - 未登录态下优雅展示为 `Volur 账号：登录`。

---

## 2. 根因与架构设计

### 2.1 宽度同步与 CSS 层叠优先级
- **问题**：全局此前为了解决折叠态单字纵向断行，设置了 `.n-popover.n-tooltip { width: max-content !important; }`，导致所有气泡均强制收缩为内容宽度，无法指定固定或弹性等宽。
- **解法**：
  1. 将全局 `max-content` 限制解耦为 `.n-popover.n-tooltip:not(.user-card-tooltip)`，使普通功能按钮（搜索、设置、通知等）继续保持紧凑横排；
  2. 为个人信息专属气泡赋予专属类名 `.user-card-tooltip`，并由 CSS 变量 `--v-user-card-tooltip-width` 动态驱动宽度（`width: var(--v-user-card-tooltip-width, 240px) !important; box-sizing: border-box;`）；
  3. 在 `VSideBar.vue` 中通过 `userCardRef` 获取底层真实 DOM 容器，并在挂载（`onMounted`）、窗口缩放（`resize`）以及侧边栏展开（`handleExpand` / `toggleCollapse`）时动态计算并同步 `userCardWidth`，确保气泡无论在展开还是折叠态都具有与 `userCard` 完全一致的物理长度。

### 2.2 多行图文信息排版
- 在 `<n-tooltip>` 内部采用语义化 Flex Column 布局容器 `.user-tooltip-box`：
  - 首行 `.user-tooltip-main` 强化字重与层次，前缀标签 `Volur 账号：` 与 `displayName` 清晰分明；
  - 辅助行 `.user-tooltip-sub` 展示用户句柄 `（username）` 与 绑定邮箱 `（email）`，字号设为 11px，文本溢出安全截断（`text-overflow: ellipsis`）；
  - 适配黑夜模式高对比层级（主文字高亮 `#e8eaed`，副文字 `#9aa0a6`）。

---

## 3. 代码修改清单

- **`src/styles/index.scss`**：
  - 将 `.n-popover.n-tooltip` 规则收敛至 `:not(.user-card-tooltip)`；
  - 声明 `.n-popover.n-tooltip.user-card-tooltip` 规则，支持 CSS 变量动态赋宽，`.n-popover__content` 设定为 `width: 100% !important; display: block !important;`。
- **`src/components/sidebar/VSideBar.vue`**：
  - 模板：挂载 `ref="userCardRef"`，Tooltip 配置 `class="user-card-tooltip"`、`:width="userCardWidth"` 与 `:style="{ '--v-user-card-tooltip-width': \`${userCardWidth}px\` }"`；
  - 模板内容：渲染 `.user-tooltip-box` 结构化多行卡片；
  - 脚本：引入 `nextTick`，增加 `userName`、`userCardRef`、`userCardWidth`，编写 `updateUserCardWidth()` 响应式监听方法；为无障碍与代码严谨性绑定 `:aria-label="userTooltipText"` 并加入 `defineExpose`；
  - 样式：在非 Scoped `<style lang="scss">` 块中补齐 `.user-card-tooltip` 与 `.user-tooltip-box` 样式。

---

## 4. 验证情况

- 执行 `npm run build`（`vue-tsc --noEmit && vite build`），14,834 模块全量编译通过，0 错误，打包耗时 15.11 秒；
- 检查代码文件零注释合规。
