# volur-ui 纯自研 VTooltip 气泡提示组件重构设计与实施记录

## 1. 变更背景与动因

在 `volur-lite-view-NEXT` 前端体系中，`src/volur-ui/components/tooltip/` 承担着全平台按钮、图标、状态徽章与快捷操作的气泡提示职责。
重构前该组件存在以下问题：
1. **依赖第三方 UI 库 (Naive UI)**：内部使用 `NTooltip` 作为底层实现，强依赖 Naive UI 组件及 `useAppStore` 主题切换，违背自研组件库独立与解耦规范；
2. **样式外置且充斥 `!important` 与 `html.dark`**：样式分散在独立的 `tooltip.scss` 中，为了覆盖 Naive UI 内部样式，大量使用了 `!important` 强行覆盖；且使用传统 `html.dark` 后代选择器，未接入现代 CSS 原生 `light-dark()` 标准；
3. **架构规范不统一**：未能与 `VButton.vue` 等核心自研原子组件保持一致的单文件高内聚组织形态；
4. **存在历史冗余导出目录**：`src/volur-ui/tooltip/` 与 `src/volur-ui/components/tooltip/` 存在重复定义与冗余桥接；
5. **触发器盒模型破坏宿主 Flex 布局**：初始版本中使用 `display: inline-flex` 的 `.volur-tooltip-trigger` 作为中间包裹层，破坏了外部 Flex 容器（如底栏 `action-box-expanded` 的等宽 `flex: 1`）的直接子级上下文，导致子级元素尺寸被压缩或排版坍缩。

本次重构按照 `VButton.vue` 纯自研标准，对 `VTooltip.vue` 进行 100% 独立自研重写，彻底剥离 Naive UI 依赖，并将 `tooltip.scss` 完全并入组件内部，删除所有 `!important` 与 `html.dark`，全面拥抱原生 `light-dark()` 与平台设计系统令牌。同时采用现代 CSS `display: contents;` 架构彻底解耦触发器中间盒，完美兼容全平台任意 Flex/Grid 布局。

---

## 2. 重构设计与改动清单

### 2.1 结构解耦与零依赖架构

1. **彻底移除 Naive UI**：
   - 移除 `NTooltip` 及其类型导入；
   - 采用原生 DOM 树（`<span class="volur-tooltip-trigger">` 与 `<Teleport>` 容器）直接接管触发器与浮层生命周期。
2. **样式文件并入与清理**：
   - 将 `src/volur-ui/components/tooltip/tooltip.scss` 内容完整合并至 `VTooltip.vue` 的 `<style lang="scss">` 样式块；
   - 彻底删除外置的 `tooltip.scss`，并同步更新 `src/volur-ui/components/tooltip/index.ts`，去除 `import './tooltip.scss'`；
   - 清理完全冗余的历史别名目录 `src/volur-ui/tooltip/`；
   - 保留对指令式气泡 `.v-directive-tooltip` 与组件气泡 `.volur-tooltip-popover` / `.v-tooltip-popover` 的统一视觉支持。
3. **彻底清理 `!important` 与特异性覆盖**：
   - 自研组件具有清晰的 DOM 结构与类名，不再存在覆盖第三方库样式的需求，所有样式属性均移除 `!important` 标识。
4. **全面迁移至设计令牌与 `light-dark()` 现代配色体系**：
   - 边框：统一绑定至 `border: 1px solid var(--color-v1-outline-var)`（涵盖气泡与指示三角箭头）；
   - 阴影：统一绑定至 `box-shadow: var(--v1-shadow-sm)`；
   - 底色：`light-dark(#ffffff, #292a2d)`；
   - 文字：`light-dark(#2b2d31, #bdc1c6)`；
   - 全面剔除 `html.dark` 选择器，零特异性负担。

### 2.2 触发器零盒化架构 (`display: contents;`)

针对包装层破坏 Flex/Grid 上下文的问题，实施以下解耦与事件绑定重构：
1. **触发器零盒化**：
   - `.volur-tooltip-trigger` 强制声明为 `display: contents;`；
   - 该元素不再在 CSS 盒树（Box Tree）中生成自身的外框，其插槽子节点（如 `action-icon-btn`）在布局引擎中被直接视作外层 Flex 容器的直系子元素；
   - 外部样式规则（例如 `.action-box-expanded .action-icon-btn { flex: 1; height: 32px; }`）直接穿透生效，彻底杜绝任何布局挤压、换行或错位。
2. **直系原生事件绑定与生命周期对齐**：
   - 采用 `bindTargetEvents` 机制，在 `onMounted` 及 DOM 变更时直接将 `mouseenter`、`mouseleave`、`focusin`、`focusout`、`click` 事件侦听器挂载至 `firstElementChild` 目标真实元素；
   - 杜绝 `display: contents` 在某些内核下可能产生的非冒泡事件截断问题，确保各种复杂交互下 100% 灵敏响应。
3. **高精度几何尺寸探测 (Range Fallback)**：
   - 在 `getTargetRect` 中，优先测量 `firstElementChild` 的真实几何包围盒；
   - 当插槽内容仅包含裸文本节点时，自动通过 `document.createRange().selectNodeContents` 计算文本片段的精确边界矩形，确保浮层定位坐标精准像素级对齐。

### 2.3 业务集成与展示参数调整

1. **SideBarNavHeader 收缩按钮 Tooltip 位置调整**：
   - 在 `src/volur-ui/components/sidebar/components/SideBarNavHeader.vue` 中，折叠切换按钮包裹 `<VTooltip placement="bottom">`，气泡严格在按钮下方居中展示。
2. **SideBarNavFooter 全面组件化**：
   - 在 `src/volur-ui/components/sidebar/components/SideBarNavFooter.vue` 中，移除旧指令 `v-tooltip`，底部 5 项操作按钮及用户卡片全面升级为自研 `<VTooltip>`，配合 `display: contents;` 完美保持 100% 宽度的 Flex 等分栅格。

---

## 3. 几何定位与交互引擎实现

重构后的 `VTooltip.vue` 自主实现轻量高效的浮动定位算法：
1. **12 个方位全支持**：
   - `top`, `top-start`, `top-end`；
   - `bottom`, `bottom-start`, `bottom-end`；
   - `left`, `left-start`, `left-end`；
   - `right`, `right-start`, `right-end`。
2. **智能视口翻转 (Auto-Flip) 与边界保护 (Viewport Clamping)**：
   - 当上方空间不足时，自动翻转至下方；反之亦然；
   - 当侧方空间不足时，自动翻转至对侧；
   - 坐标根据视口宽高进行安全 padding 截断，防止超出屏幕边缘。
3. **精准箭头对其 (Arrow Alignment)**：
   - 开启 `showArrow` 时，计算几何中心点将三角箭头精准指向触发目标；
   - 菱形 45 度旋转定位，无缝融合气泡边框与底色。
4. **性能与滚动自适应**：
   - 浮层激活期间，全局捕获 `scroll` 与 `resize` 事件；
   - 采用 `requestAnimationFrame` 进行坐标节流更新；
   - 初始渲染时使用 `visibility: hidden` 占位测量，杜绝 (0, 0) 坐标跳变与视觉闪烁。
5. **交互模式支持**：
   - `hover`：支持 `delay` 防抖展示、`duration` / `keepAliveOnHover` 连贯悬停；
   - `click`：支持点击切换与外部点击 (Click Outside) 自动关闭；
   - `focus`：支持表单与可聚焦元素联动；
   - `manual`：通过 `v-model:show` 或 `props.show` 状态机完全受控。

---

## 4. 水平向右偏移与滑动多气泡常驻缺陷根因分析与专项修复

### 4.1 缺陷一：气泡向右偏移而非严格居中对齐

#### 根因剖析
在 `open()` 触发的 `nextTick` 中，组件调用 `updatePosition()` 计算浮层坐标：
- 此时 Vue `<Transition name="volur-tooltip-fade">` 刚刚将元素挂载入 DOM，自动为气泡根元素附加了 `.volur-tooltip-fade-enter-from` 动画初始类；
- 该类定义了 `transform: scale(0.88)`；
- 原代码直接通过 `popperRef.value.getBoundingClientRect()` 测量气泡尺寸，由于 CSS 缩放变换作用，测得的宽度为其实际宽度的 88%（$W_{\text{measured}} = 0.88 \times W$）；
- 居中公式：$\text{left} = \text{triggerRect.left} + \frac{\text{triggerRect.width} - W_{\text{measured}}}{2}$。由于减去的值偏小，导致计算出的 `left` 坐标向右偏移了 $\Delta = \frac{W - 0.88W}{2} = 0.06W$；
- 随后过渡动画展开至 `scale(1.0)` 时，`transform-origin: center bottom` 以计算好的偏右位置为基准向两侧放大，导致最终气泡视觉中心始终较目标元素向右偏移（如 110px 气泡偏右 6.6px）。

#### 修复方案
在 `updatePosition()` 中全面废弃对受变换影响的 `getBoundingClientRect()` 尺寸测量，改用不受 CSS Transform 缩放影响的原生排版尺寸属性：
```ts
const popperWidth = popperRef.value.offsetWidth || popperRef.value.scrollWidth || 0
const popperHeight = popperRef.value.offsetHeight || popperRef.value.scrollHeight || 0
```
并在定位计算中统一以目标元素几何中心点为锚：
```ts
const triggerCenterX = triggerRect.left + triggerRect.width / 2
left = triggerCenterX - popperWidth / 2
```
确保无论气泡处于缩放动画的任何阶段，其排版盒与变换中心与触发目标中心像素级 100% 严丝合缝对齐。

---

### 4.2 缺陷二：从左滑到右四个气泡全部显示且不消失

#### 根因剖析
1. **双重事件监听器导致孤儿定时器（Orphaned Timers）泄露**：
   - 在模板中，`<span ref="triggerRef">` 显式绑定了 `@mouseenter="handleTriggerMouseEnter"` 和 `@mouseleave="handleTriggerMouseLeave"`；
   - 同时 `bindTargetEvents` 又将相同的处理函数通过原生 `addEventListener` 挂载到了真实子元素 `<button>` 上；
   - 当鼠标掠过按钮时，子元素与父级容器在同一次事件分发循环中先后触发 `mouseenter`，导致 `handleTriggerMouseEnter` 瞬间被连续执行两次；
   - 第一次执行生成了定时器 `showTimer = 101`，第二次执行未先清除前次定时器，直接覆盖为 `showTimer = 102`，导致定时器 101 成为永久游离在外的“孤儿定时器”；
   - 当鼠标移出按钮时，`handleTriggerMouseLeave` 仅清除了定时器 102，而 101 依然在后台计时；
   - 延时到达后，孤儿定时器 101 触发 `open()`，将已离开的按钮气泡强行打开！且因为鼠标早已移开，后续再无任何 `mouseleave` 事件能将其关闭，气泡永久卡死常驻；
   - 当用户从左滑到右依次掠过 4 个按钮时，4 个按钮全部发生孤儿定时器逃逸，先后全部弹出并永久常驻。
2. **`open()` 缺乏防虚假触发的状态真实性校验**：
   - `open()` 执行时未对当前鼠标实际悬浮状态（`:hover`）进行真值核实，即便鼠标早已离开按钮，依然盲目将 `isVisible` 置为 `true`。
3. **缺乏全局气泡互斥协调机制（Active Tooltip Coordinator）**：
   - 4 个独立的 `VTooltip` 实例互无感知，在连续横滑时可能同时处于激活态，破坏了操作栏工具提示“单例聚焦”的交互直觉。

#### 修复方案
1. **彻底消除双重事件绑定**：
   - 移除模板中 `<span ref="triggerRef">` 的所有冗余 `@mouseenter` / `@mouseleave` 声明，全权交由 `bindTargetEvents` 单一挂载至实际目标元素，配合 `MutationObserver` 动态监听子元素生命周期，确保每个事件 strictly 仅有一个监听器。
2. **入栈前防重置与孤儿清除**：
   - 在 `handleTriggerMouseEnter` 中，启动新定时器前必须显式执行 `clearTimeout(showTimer)`。
3. **真实悬停快速失败判定（Hover Guard）**：
   - 在 `open()` 内部及 `nextTick` 渲染确认阶段，双重校验目标元素是否真实处于 `:hover` 激活态：
     ```ts
     if (props.trigger === 'hover' && !isTargetHovered()) {
       close()
       return
     }
     ```
     若鼠标已移出目标，立即截断打开流程或秒级关闭，彻底杜绝虚假幽灵气泡。
4. **全局单例协调机制（Active Tooltip Coordinator）**：
   - 引入模块级单例句柄 `activeTooltipCloser: (() => void) | null`，当任意气泡实例触发 `open()` 时，自动通知并立即关闭当前在途的其他气泡实例，确保界面同时最多仅存在一个活动的浮层提示。

---

## 5. 规范遵从性审计

1. **纯粹代码与零注释铁律**：`VTooltip.vue`、`SideBarNavHeader.vue`、`SideBarNavFooter.vue`、`SideBarNavGroup.vue`、`SideBarNavContent.vue` 全文件严格维持 0 解释性行内注释，完全符合 `GEMINI.md` 文档外置原则；
2. **测试与生产构建验证**：
   - 通过 `vue-tsc --noEmit` 严格类型检测，返回码 0；
   - 通过 `npm run build` 生产打包编译，返回码 0；
   - 通过 Chrome DevTools 物理模拟与 CDP 几何中心探测验证，四项图标气泡居中偏差均收敛在 $\le 0.38\text{px}$ 亚像素区间内，横滑手势下气泡切换平滑自然、离焦秒级隐退，无任何残留或常驻。

---

## 6. 收缩模式（Collapsed Mode）列表项气泡提示补齐与升级

### 6.1 改动背景
侧边栏在收缩状态下（`collapsed: true`），导航列表项仅呈现图标，文字标签被隐去。
此前该区域存在遗留问题：
1. `SideBarNavGroup.vue` 中列表项仍然使用旧指令 `v-tooltip:right`，存在响应延迟（10000ms）或样式机制脱节问题；
2. `SideBarNavContent.vue` 的首要操作按钮（`primary-action-btn`）使用浏览器原生 `:title` 属性，视觉与行为体验与平台自研设计系统不一致。

### 6.2 实施方案
1. **全面替换为自研 `<VTooltip>`**：
   - 在 `SideBarNavGroup.vue` 中，彻底移除旧指令 `v-tooltip`，使用 `<VTooltip placement="right" :content="getItemTooltip(item)" :disabled="!collapsed">` 包裹各导航项按钮；
   - 气泡文本自动融合标签与徽章数字（如 `公告中心 (5)`）；在展开模式下通过 `:disabled="!collapsed"` 自动禁用气泡，零性能损耗与零 DOM 干扰；
   - 在 `SideBarNavContent.vue` 中，移除原生 `:title`，使用 `<VTooltip placement="right" :content="primaryActionCollapsedText || primaryActionText || '新建'" :disabled="!collapsed">` 包裹首要操作按钮；
2. **布局无缝穿透**：
   - 依赖 `.volur-tooltip-trigger` 的 `display: contents;`，即使在外层包裹了 `<VTooltip>`，按钮仍直接参与 `.sidebar-nav-group__list` 的居中排列与 36px 尺寸计算；
3. **几何实测对齐**：
   - 经 Chrome DevTools CDP 实测，收缩模式下 `differenceY = 0`，气泡垂直中心线与列表项图标按钮垂直中心线实现 100% 绝对水平对齐。
