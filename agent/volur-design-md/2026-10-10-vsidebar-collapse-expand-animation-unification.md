# VSideBar 收缩与拉伸动画对称性统一重构设计审计记录

## 1. 变更背景与现象分析

### 1.1 用户反馈现象
在桌面侧边栏 [`VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 的折叠与展开交互中，用户观察到“收缩动画”和“拉伸动画”的表现完全不同：
- **拉伸（展开）时**：侧边栏宽度从 74px 变为 256px，内部元素像卷帘门一样平滑向右舒展并逐一显现，过渡连贯丝滑；
- **收缩（折叠）时**：在点击收起的瞬间（第 0 毫秒），文字、Logo 与卡片布局直接断崖式瞬间消失并跳变成小图标居中状态，随后仅剩空白背景在缓慢缩回，缺乏收拢动画过程。

### 1.2 根因定位与机制不对称分析
1. **`display: none !important` 破坏 CSS 过渡补间**：
   原有 CSS 在折叠态（`&.is-collapsed` / `&.n-menu--collapsed`）下，对导航项文本 `.n-menu-item-content-header` 声明了 `display: none !important`。在浏览器渲染流中，`display: none` 是不可插值的离散属性，导致点击瞬间文字直接从渲染树抹除，根本无法执行任何渐变或收缩过渡。
2. **多套 DOM 暴力 `v-if` / `v-else` 切换**：
   - 顶部 Header 被拆分为 `header-inner-expanded` 与 `header-inner-collapsed` 两个独立 DOM，收缩瞬间 Logo 瞬间卸载，汉堡按钮由右侧瞬移到居中；
   - 主要操作按钮使用了带有 `mode="out-in"` 的 `<transition>`，导致收缩时需先耗时 0.2s 淡出再耗时 0.2s 淡入，总耗时达 0.4s，与侧边栏外壳 0.25s 收缩完全不同步；
   - 用户信息卡片被拆分为 `user-card-expanded` 与 `user-card-collapsed`，收缩时瞬间切为单图标，高度与文字无平滑过渡。
3. **收缩时内部子元素在 t=0ms 提前变为居中 38px 状态**：
   在收缩开始时，外壳宽度尚为 256px，但内部各容器已立刻被赋予 `width: 38px !important; margin: 0 auto; align-items: center;`，造成在宽外壳中居中缩成小块、右侧留下近 180px 突兀空白背景的不对称视觉缺陷。

---

## 2. 核心重构与双向物理对称设计

### 2.1 统一单结构连续物理过渡（Single-DOM Continuous Transition）
1. **Header 顶部栏结构统一**：
   - 将原先分离的 `header-inner-expanded` 与 `header-inner-collapsed` 统一收敛为单一 `.header-inner` 容器；
   - Logo 容器赋予 `max-width: 180px; opacity: 1; padding: 0 18px;`，折叠态平滑收拢为 `max-width: 0; opacity: 0; padding: 0;`；
   - 汉堡切换按钮同一节点常驻，随着 Logo 宽度的平滑收放自然滑动居中或推至右侧，Tooltip 与 ARIA 动态联动，彻底消除瞬移。
2. **Primary Action 主要动作按钮单节点化**：
   - 移除 `mode="out-in"` 带来的双倍延迟，统一由单一 `.primary-action-btn` 承载；
   - 展开态尺寸为 `width: 100%; height: 40px;`，折叠态平滑收敛为 `width: 38px; height: 38px; padding: 0;`；
   - 按钮标签文字通过 `max-width: 160px` -> `max-width: 0` 与 `opacity: 1` -> `opacity: 0` 平滑同步淡出收缩。
3. **User Profile 用户卡片单节点化**：
   - 统一收敛为单一 `.user-card` 结构，左侧固定头像，右侧为文本信息；
   - 折叠态平滑收敛为 38px 单头像卡片，文本平滑淡出收缩，彻底消除 DOM 重绘闪烁。
4. **Bottom Action 动作按钮单节点化**：
   - 统一为单一 `.bottom-action-btn` 结构，图标保持居中，文本平滑收纳。

### 2.2 菜单项消灭 `display: none` 并赋予对称尺寸与透明度缓动
- 导航菜单项 `.n-menu-item`、内容容器 `.n-menu-item-content` 与文本 `.n-menu-item-content-header` 统一绑定 `0.25s cubic-bezier(0.4, 0, 0.2, 1)` 尺寸补间与 `0.2s` 透明度补间；
- 折叠态下文本通过 `max-width: 0 !important; width: 0 !important; opacity: 0 !important; overflow: hidden !important;` 实现零主轴占位与不可见，彻底消除突兀闪灭；
- 图标边距 `margin-right: 12px` 与 `margin-right: 0` 双向平滑过渡，实现靠左至居中的自然位移。

### 2.3 底部 Action Box 平滑交叉淡入淡出
- 为横排（展开态）与竖排（折叠态）快捷操作坞引入 `<transition name="dock-fade" mode="out-in">`；
- 赋予 `opacity: 0.15s` 与 `scale(0.96)` 极简微动效，并由外层容器限制 `overflow: hidden`，消除高度突增跳变。

### 2.4 暗色模式与边框完全对齐
- `html.dark` 对应的单结构类名（`.primary-action-btn`、`.v-menu`、`.user-card`、`.bottom-action-btn` 等）同步对齐 Google Workspace 暗色配色体系。

---

## 3. 验证情况
- **工程构建与类型验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全流程编译通过，0 错误，0 警告。
- **改动文件**：
  - [`src/components/sidebar/VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue)
