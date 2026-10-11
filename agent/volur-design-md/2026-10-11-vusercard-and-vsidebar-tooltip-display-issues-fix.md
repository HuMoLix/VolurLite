# VUserCard 与 VSideBar 用户提示 Tooltip 显示问题全面修复审计记录

## 1. 变更背景与问题根因

在对平台侧边栏（`VSideBar.vue`）底部个人信息栏与独立抽离的用户卡片（`VUserCard.vue`）的交互排查中，发现 Tooltip 存在多项显示冲突与体验缺陷：

1. **Popover 与 Tooltip 竞争冲突与幽灵气泡**：
   - 侧边栏用户区域以 `<n-popover>` 包裹 `<n-tooltip>`；
   - 用户在点击触发大卡片（`isUserCardPopoverShow = true`）前，光标悬停已预先激活动态 Tooltip；
   - 在展开态下，Popover 与 Tooltip 的 `placement` 均为 `'top'`，两层弹出层重叠遮挡；且仅依靠 `:disabled` 无法在已激活时瞬时关闭气泡，卡片关闭后光标停留还会导致 Tooltip 闪烁重现。
2. **折叠态下巨型 240px 矩形空宽**：
   - 此前重构将 `.n-popover.n-tooltip.user-card-tooltip` 强制指定为 `--v-user-card-tooltip-width: 240px`；
   - 折叠态（`isCollapsed === true`）下 `updateUserCardWidth` 直接跳过更新，保留了展开态的 240px 宽度；当折叠态下 Tooltip 显示在右侧（`placement="right"`）时，头像旁弹出了一个宽达 240px 的矩形大框，留白严重失调。
3. **Tooltip 文本重复与死代码分支**：
   - 首行输出 `displayName`（无昵称时 fallback 为 `username`）；
   - 次行仅判断 `userName !== userEmail`，未检查 `userName !== displayName`，导致无昵称用户同时显示两行完全相同的用户名；
   - 随后的 `v-else-if="userName && userName === userEmail"` 处于 `v-if="userEmail"` 之后，逻辑恒为假，属于死逻辑分支。
4. **多语言硬编码**：
   - `Volur 账号：` 文本硬编码在模板与计算属性中，未接入 i18n 资源文件。
5. **`VUserCard.vue` 卡片自身提示能力缺失**：
   - 右上角关闭按钮仅有 `aria-label`，未提供 Hover Tooltip；
   - 用户名与邮箱设置了 `text-overflow: ellipsis` 截断，但在长文本溢出时用户无法查看被省略的完整字符串。

---

## 2. 核心修复与改动清单

### 2.1 引入受控状态与 Tooltip 显隐同步机制 (`VSideBar.vue`)
- 新增 `isUserTooltipShow = ref<boolean>(false)` 受控状态，绑定至 `<n-tooltip v-model:show="isUserTooltipShow">`；
- 增加 `watch(isUserCardPopoverShow)` 监听，并在 `handleUserClick` 与 `closeUserCardPopover` 时主动重置 `isUserTooltipShow.value = false`，彻底消除大卡片与气泡重叠与关闭闪烁问题；
- 在 `defineExpose` 中对外导出 `isUserTooltipShow`。

### 2.2 响应式折叠态宽度与样式适配 (`VSideBar.vue` & `src/styles/index.scss`)
- 动态计算属性 `userTooltipStyle`：在 `isCollapsed` 模式下将 `--v-user-card-tooltip-width` 设为 `'auto'`，展开态下与 `userCardWidth` 等宽；
- 为折叠态 Tooltip 注入 `.is-collapsed` 类名；在 `VSideBar.vue` 与 `src/styles/index.scss` 中补充：
  ```scss
  &.is-collapsed {
    width: max-content !important;
    min-width: 140px !important;
    max-width: 260px !important;
  }
  ```
  使折叠态下的用户气泡紧凑自适应排版。

### 2.3 修正用户信息展示逻辑与死逻辑清理 (`VSideBar.vue`)
- 首行渲染：`Volur 账号：displayName`；
- 次行渲染：仅当 `userName && userName !== displayName` 时展示 `（userName）`，彻底规避双重用户名展示；
- 第三行渲染：仅当 `userEmail` 存在时展示 `（userEmail）`；
- 彻底剔除无效的死代码 `v-else-if` 分支；
- 同步更新无障碍计算属性 `userTooltipText` 的拼接逻辑。

### 2.4 全面接入多语言 (`common.json`)
- 在 `zh-CN`、`en-US`、`zh-TW` 的 `common.json` 中统一新增 `"volur_account"` 词条：
  - `zh-CN`: `"Volur 账号："`
  - `en-US`: `"Volur account: "`
  - `zh-TW`: `"Volur 帳號："`
- 替换模板及计算属性中的所有硬编码字符串。

### 2.5 `VUserCard.vue` 内部补齐工具提示与文本省略浮层
- 为顶部关闭按钮添加 `<n-tooltip placement="bottom" trigger="hover" :show-arrow="false">`；
- 引入 `<n-ellipsis :tooltip="{ placement: 'top' }">` 包裹 `.card-user-name` 与 `.card-user-email`，确保长名称与超长邮箱在截断时支持 Hover 浮动完整查看；
- 优化 `.card-user-name` 与 `.card-user-email` 的 SCSS 样式（声明 `width: 100%; min-width: 0;`），确保 `<n-ellipsis>` 内部尺寸计算准确。

### 2.6 解决折叠态切回展开态宽度坍塌至 38px 的动画时序竞争问题 (`VSideBar.vue`)
- **根因分析**：
  - 折叠态 `.user-card.is-collapsed` 的固定尺寸为 `38px`；
  - 展开与折叠包含 `0.25s`（250ms）的 CSS 过渡动画；
  - 在切换至展开时，`nextTick` 在微秒级执行，此时底层动画处于 0ms 阶段，`getBoundingClientRect().width` 测量到了过渡起始帧的脏数据 `38px`；
  - 动画结束后未再次校准，导致 `userCardWidth` 被永久锁死在 `38`。
- **修复方案**：
  1. **防呆阈值兜底**：在 `updateUserCardWidth` 中，若 `rect.width < 120`（处于折叠态或动画中），拒绝采纳该脏尺寸，安全回退到默认展开尺寸（`props.width - 16` 或 `240`）；
  2. **动画完成延时校准**：封装 `scheduleUpdateUserCardWidth()`，在 `nextTick` 触发初始计算的同时，启动 `setTimeout(..., 260)` 在动画结束帧精准二次校准真实尺寸；
  3. **ResizeObserver 自适应监听**：引入原生 `ResizeObserver` 动态监听 `userCardRef` 的尺寸变化，组件销毁时同步卸载，确保无论窗口缩放还是动画过渡均能精确同步。

---

## 3. 验证情况

1. **类型检查与全量编译**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 15,554 个模块全部编译通过，TypeScript 类型校验 0 错误，打包耗时 6.83 秒。
2. **零注释铁律遵守**：
   - 本次修改的所有代码文件（`VUserCard.vue`、`VSideBar.vue`、`index.scss`、`common.json`）均严格保持零注释。
