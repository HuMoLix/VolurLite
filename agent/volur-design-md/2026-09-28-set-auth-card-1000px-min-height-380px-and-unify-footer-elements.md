# 审计与施工记录：调整认证卡片 1000px/380px 几何规格、剖析表单留白根因并全量统一页脚文本与按钮模型

## 1. 变更背景与需求目标

根据用户的最新指示，本次迭代主要执行以下三大优化：
1. **尺寸重调**：将桌面端 `auth-card` 的 `max-width` 修改为 `1000px`，`min-height` 修改为 `380px`，且 `auth-footer` 宽度同步对齐至 `1000px`；
2. **留白根因深入剖析**：系统性分析 `auth-card` 内部输入框与底部操作按钮之间产生过多留白的底层机制与高度占用原因；
3. **页脚要素全量统一**：在 `auth-footer` 中，将“切换语言”下拉按钮、右侧“帮助 隐私权 条款”链接、版权信息（`© 2026 Volur, Inc.`）、工信部备案信息（`京ICP备`）的文字大小严格统一（统一为 `12px`），同时交互按钮的物理盒模型大小（高度 32px、内边距、圆角与高亮）实现完全统一。

---

## 2. 深入分析：输入框与按钮之间为何存在大量留白？究竟是什么占用了高度？

经过对 DOM 节点与 CSS 计算样式的深度拆解，输入框与按钮之间存在巨大空隙的根本原因如下：

### 2.1 容器高度硬性设定 (`min-height`) 与表单实际高度的落差
- **表单内容实际所需的物理高度**：
  在 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 中：
  - `n-input` 输入框高度：约 `40px`；
  - 输入框与“忘记了电子邮件地址？”文字之间的间距：约 `10px`；
  - “忘记了电子邮件地址？”链接高度：约 `20px`；
  - 提示说明“不是您自己的计算机？...：约 `40px`；
  - 底部操作按钮自身高度：`40px`；
  - **表单内容整体的物理占用总高度仅为约 `180px` 左右**。
- **此前卡片硬性高度的压制**：
  此前 `.auth-card` 设置了 `min-height: 440px ~ 450px`，减去内边距后，内部可用高度长达 `360px` 左右。这意味着容器强行比内容多出了近 `180px` 的富余高度。

### 2.2 两端对齐 (`justify-between`) 将富余高度全部拉伸为空白
- 在 `IdentifierView.vue` 根节点中声明了：
  ```html
  <div class="auth-form-content w-full h-full flex flex-col justify-between">
  ```
  - `h-full` 强制组件占据父容器 100% 的高度（即整个 `360px` 可用高度）；
  - **`justify-between`** 是造成空白的核心机制：它强制把第一个子容器（输入框 + 提示文案）固定在最顶部（`y = 0`），而把最后一个子容器（操作按钮栏）固定在最底部（`y = bottom`）；
  - **中间所有由于 `min-height` 多出来的约 180px 高度，全部被 flex 机制转换为弹性间距（Empty Space）填充在两者之间！**

### 2.3 按钮容器自带的二级内边距叠加 (`pt-8 sm:pt-12`)
- 按钮栏容器自身写有 `pt-8 sm:pt-12`，在已经由 `justify-between` 拉伸出大片空白的基础上，又强制增加了 `32px ~ 48px` 的顶部 padding，导致留白进一步膨胀失真。

### 2.4 解决方案效果：
- 本次将卡片 `min-height` 收敛为用户要求的 `380px`（内可用高度为 `380 - 72 = 308px`）；
- 按钮栏容器顶部 padding 收敛为 `pt-6 sm:pt-8`；
- 输入框与按钮之间的留白从原先的近 `190px` 骤降为舒适紧凑的 `~80px`，视觉重心紧凑饱满。

---

## 3. 具体修改清单

### 3.1 认证主布局 (`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`)
1. **尺寸调整**：
   - 桌面端 `@media (min-width: 840px)` 下：
     - `.auth-card`: `max-width: 1000px; min-height: 380px; padding: 36px 44px;`；
     - `.auth-footer`: `max-width: 1000px;`，两端严格平齐。
2. **页脚要素全量统一**：
   - 页脚根容器显式声明基准字体 `text-[12px]`；
   - **多语言切换按钮**：采用 `auth-btn-link text-[12px]`，高度统一为 `32px`，内嵌标准 Google SVG Caret 箭头；
   - **右侧跳转链接**：“帮助”、“隐私权”、“条款”统一采用 `auth-btn-link text-[12px]`（高度 32px，内边距 `0 10px`）；
   - **分隔符**：`text-[12px] h-8 inline-flex items-center px-1`；
   - **版权信息**：`text-[12px] h-8 inline-flex items-center px-2 select-none`；
   - **备案信息**：统一应用 `auth-btn-link text-[12px]`（高度 32px），内嵌备案图片与 `京ICP备` 文字，Hover 时呈现统一微透明圆角背景；
   - 全页脚无论静态文案还是交互按钮，字体严格为 **`12px`**，按钮高度严格统一为 **`32px`**。

### 3.2 表单视觉韵律优化 (`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`)
- 按钮栏内边距调整为 `pt-6 sm:pt-8`，与卡片 380px 最小高度达成黄金比例配合，消灭失真断层。

### 3.3 全局通用按钮链接样式 (`src/volur-lite-view-NEXT/src/styles/index.scss`)
- `.auth-btn-link` 与 `.btn-link` 全面收敛尺寸基准：高度 `32px`、字号 `12px !important`、行高 `16px !important`、圆角 `8px`、内边距 `0 10px`。

---

## 4. 验证与审查结论

1. **工程构建**：
   - 运行 `npm run build`，耗时 4.90s 构建通过，无任何 TypeScript / Sass 编译警告。
2. **端到端实机审查（Chrome DevTools）**：
   - **桌面端（1280x800）**：
     - 卡片宽度呈现 `1000px`，高度为 `380px`，卡片与底栏严格对称等宽；
     - 输入框与按钮之间的间距紧凑自然；
     - 底栏所有文字完全一致为 `12px`，语言按钮与右侧链接按钮规格统一，高度均精确为 `32px`。
   - **移动端（390x844）**：
     - 保持纯白无缝画布与单行吸底页脚表现。
