# 审计与施工记录：深入对齐 Google Accounts 卡片比例几何学与多语言按钮规范

## 1. 变更背景与需求问题剖析

在上一轮修复中，用户进一步指出了两个精细层面的视觉体验差异：
1. **多语言切换按钮与右侧链接大小不一致，且下拉图标间距过紧**：
   - 切换语言按钮此前设置了 `text-[13px]` 且使用了字重 `font-medium`，而右侧的“帮助 隐私权 条款”链接则使用了 `text-[12px]`，导致左右两侧在高度、字体、行高和点击响应盒模型上存在视觉失衡；
   - 语言下拉箭头此前使用紧贴在字符后方的小字符（`gap-1.5`，约 6px），缺乏 Google Material 3 标准中清晰、舒展的几何分离度。
2. **卡片框比例与 Google Accounts 存在明显差异（Google Accounts 明显更长以及更窄）**：
   - 之前桌面端卡片设置了 `max-width: 960px`，横向展开过宽（宽高比达到 2.18 : 1），如同横向扁平横幅，导致左右分栏之间留白空洞；
   - 右侧表单输入框此前直接置顶贴在卡片天花板，而左侧 Logo、标题存在自然向下排布，导致表单与标题垂直视线错位，表单下方到按钮区域留白巨大。

---

## 2. Google Accounts 官方底层测量与几何学分析

通过对 Google Accounts 生产环境（`https://accounts.google.com/v3/signin/identifier`）源码中 `.TcuCfd`、`.eXa0v` 及 `.AVAq4d` 的真实样式进行反编译提取，得出以下关键数据：

### 2.1 底栏按钮与图标几何学 (Footer Button & Caret Metrics)
- **文字与盒模型完全同归**：
  - 语言下拉按钮（`.dEoyBf .VfPpkd-uusGie-fmcmS`）与右侧辅助链接（`.AVAq4d`）：
    - 均严格定义为 `font-size: 0.75rem`（即 `12px` / `text-xs`）；
    - 均采用 `font-weight: 400`（常规字重）与一致的内边距；
    - 文本颜色统一为 `var(--gm3-sys-color-on-surface-variant, #444746)`（即 Google 规范的次要描述灰）。
- **下拉小三角间隙与渲染**：
  - Google 官方采用 SVG 矢量多边形 `<svg viewBox="7 10 10 5"><polygon points="7 10 12 15 17 10" /></svg>`；
  - 小三角左侧有显式的 `margin-left: 8px ~ 10px` 间距，使语言文字（如“简体中文”）与下凹三角形具备舒适的视觉呼吸感，绝非紧贴挤压。

### 2.2 卡片容器长宽比与分栏策略 (Card Proportions & 50/50 Split)
- **桌面端卡片宽度基准**：
  - Google Accounts 在宽屏横向桌面模式下的标准卡片宽度为 **`840px`**（非 960px）；在特定中等屏幕/竖屏下退化为 **`480px` 宽 x `528px` 高** 的纵向长条卡片；
  - 之前的 `960px` 比官方足足宽出 120px，在 1080p 屏幕上呈现出明显的“过宽扁平”观感；
  - 本次调整将卡片及关联页脚最大宽度精准收敛至 **`800px` ~ `840px`**，使左右分栏处于极佳的黄金视觉长宽比（宽高比降至约 1.8 : 1），展现出“更长（纵深舒展）以及更窄（横向内敛）”的 Google 标志性视觉特征。
- **50% / 50% 轴向对称与垂直视线流**：
  - 官方左栏 `.zIgDIc` 与右栏 `.UXFQgc` 均显式声明 `flex-basis: 50%; max-width: 50%;`；
  - 官方顶部预留舒适落差，右侧表单输入框起始位置与左侧“登录”主标题自然对齐，输入框宽度控制在 `~340px`，完全消除了此前输入框被过度横向拉伸变形的失真感。

---

## 3. 具体修改清单

1. [`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)：
   - **语言下拉按钮与右侧链接尺寸 1:1 对齐**：
     - 左右均统一为 `text-xs px-2.5 py-1.5 text-google-text-muted`；
     - 引入 Google 官方 SVG Caret 图标（`viewBox="7 10 10 5"`），并配置 `ml-2.5`（10px）显式分离间距；
   - **卡片与页脚尺寸重塑**：
     - 桌面端卡片由 `max-width: 960px` 收敛至 `max-width: 800px`，最小高度保持 `440px`；
     - 分栏策略严格调整为左右各占 `50%`（`flex: 0 0 50%; max-width: 50%`）；
     - 右侧表单区域添加适度 `padding-top: 12px`，与左侧主标题形成水平对齐线；
     - 页脚宽度同步收敛至 `max-width: 800px`，与卡片保持完全对齐。
2. [`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)：
   - 优化表单垂直韵律：输入框与下方“忘记了电子邮件地址？”保持 `10px` 紧凑组合间距；
   - “忘记了电子邮件地址？”与无痕浏览提示保持 `32px` 自然呼吸留白，提示文字优化为 `text-sm leading-relaxed`，使右侧纵深更加丰满舒展。

---

## 4. 验证与审查结果

1. **工程编译验证**：
   - 执行 `npm run build`，耗时 3.78s 构建成功，无任何 TypeScript / Sass 报错。
2. **Chrome DevTools 端到端视觉审查**：
   - **PC 桌面端（1280x800）**：
     - 卡片宽度更加收敛内聚，长宽比协调，彻底消除了中心虚空感；
     - 左侧语言切换按钮文字大小与右侧“帮助 隐私权 条款”完全一致，SVG 下拉三角距离文字具备 10px 清晰间隙；
     - 底部单一页脚与卡片边沿严格平齐。
   - **移动端（390x844）**：
     - 纯白无缝画布与单行全要素吸底页脚表现稳健无异常。
