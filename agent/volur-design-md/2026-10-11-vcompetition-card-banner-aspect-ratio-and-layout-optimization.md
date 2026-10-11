# VCompetitionCard 封面图比例适配与卡片布局排版优化

## 1. 变更背景与需求目标

在 Volur Lite NEXT 前端工程中，针对竞赛卡片组件 `VCompetitionCard.vue` 进行了样式与交互层面的深度审计与优化，解决以下问题与需求：
1. **封面 Banner 显示不齐问题根因排查与适配**：实际生产中上传的 Banner 图片原始分辨率为 1916 × 821 像素（长宽比约为 2.3337:1），原有前端样式采用了固定高度（`h-36` 即 144px）与较窄的固定宽度（`md:w-60 lg:w-64` 即 240px ~ 256px），导致在 `object-cover` 模式下图片左右两侧约 25%~30% 的画面被裁剪，内容无法完整显示；
2. **移除冗余的短链接复制按钮**：卡片上原有的 `/c/{slug}` 短链接及一键复制按钮破坏了卡片主视觉的聚焦度，彻底移除；
3. **优化元数据信息层级排版**：将竞赛状态徽章（如“进行中”、“报名中”）与竞赛类型徽章（如“CTF解题赛”）统一调整至标题后方同一行展示，提升信息关联度与紧凑度；
4. **移除多余悬停动效**：根据极简设计规范，移除卡片悬停时的 Banner 图片缩放动效（`scale-105`）以及报名/详情按钮中箭头的平移动效（`translate-x-1`）；
5. **高度严格对齐与 Flex 布局重构**：消除由于禁用 Tailwind preflight（`preflight: false`）导致浏览器默认 `h3`（20px margin）与 `p`（14px margin）产生的纵向多余外边距，使内容区域与左侧 Banner 封面图完全等高（137px），标题顶部与图片顶部对齐，时间底部与图片底部对齐。

---

## 2. 核心分析与改动点

### 2.1 Banner 图片比例分析与前端容器自适应重构
- **图片尺寸分析**：
  - 测试样例：`uploads/images/58408503b120477883b448a79e7705e5.png`；
  - 实际分辨率：`1916px × 821px`；
  - 精确宽高比：`1916 / 821 ≈ 2.333739:1`（接近 7:3 / 21:9 超宽比例）。
- **原样式缺陷**：
  - 原容器为 `w-full md:w-60 lg:w-64 h-36`；
  - 容器比例仅为 `240:144`（1.67:1）或 `256:144`（1.78:1，即 16:9）；
  - `object-cover` 为填满 144px 高度，迫使图片宽度拉伸至 `144 × 2.3337 ≈ 336px`，左右两侧约 80px~96px 的内容因 `overflow: hidden` 被强行裁切，造成“显示不全/显示不齐”。
- **样式解决方案**：
  - 移除固定高度 `h-36`；
  - 引入 Tailwind CSS 任意比例属性 `aspect-[1916/821]`；
  - 容器尺寸重构为 `w-full md:w-64 lg:w-72 xl:w-80 aspect-[1916/821]`；
  - 图片配合 `w-full h-full object-cover`，在移动端和桌面端各断点下均严格保持 1916:821 原始物理比例，画面完整展示，杜绝裁切与形变失真。

### 2.2 移除短链接复制功能
- 移除了卡片中的 `/c/{{ competition.slug }}` 药丸展示块与复制按钮；
- 移除组件内部的 `copiedSlug` 响应式变量、`handleCopyLink` 复制逻辑、`copy-slug` emit 事件定义；
- 清理无用的 `@vicons/ionicons5` 图标导入（`CopyOutline`, `CheckmarkOutline`）以及 `vue` 的 `ref` 引用。

### 2.3 状态与类型徽章移至标题后方
- 将原置于卡片顶部独立一行的状态徽章与类型徽章下移，与竞赛标题 `<h3>` 组合在同一 Flex 容器行内展示：
  ```html
  <div class="flex items-center flex-wrap gap-2.5">
    <h3 class="m-0 text-lg sm:text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary ...">
      {{ competition.title }}
    </h3>
    <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium shrink-0" :class="getStatusBadgeClass(competition.status)">...</span>
    <span v-if="competition.type" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ... shrink-0">...</span>
  </div>
  ```
- 徽章统一增加 `shrink-0` 样式，保证在多端自适应换行时徽章标签永不被挤压变形。

### 2.4 移除图片放大与按钮箭头动效
- **图片缩放动效**：移除 `img` 上的 `group-hover:scale-105 transition-transform duration-300`，悬停时保持图片静态稳定；
- **按钮箭头位移动效**：移除 `ArrowForwardOutline` 上的 `transition-transform duration-200 group-hover:translate-x-1`，以及按钮上的 `active:scale-95` 和 `group-hover:shadow`，使其回归沉稳的经典 Google 扁平操作按钮。

### 2.5 Flex 布局与高度对齐优化（内容区与 Banner 等高）
- **根因发现**：因工程配置 `preflight: false`，浏览器原生样式为 `<h3>` 附加了 `20px` 的上下外边距，为 `<p>` 附加了 `14px` 的上下外边距，导致内容区自然高度（166px）超过了左侧 Banner 容器的高度（137px），上下边缘产生突兀落差；
- **重构方案**：
  - 为 `h3` 和 `p` 显式添加 `m-0`，重置多余外边距；
  - 内容容器调整为 `flex-1 min-w-0 flex flex-col justify-between gap-3 md:gap-0 self-stretch`；
  - Banner 容器显式添加 `md:self-center` 并严格保持 `aspect-[1916/821]`；
  - 最终在桌面端实现了图片容器与右侧信息区域高度完全一致（均为 137px），标题顶部对齐图片顶部，时间底部对齐图片底部，右侧按钮垂直居中，形成视觉统一、层次分明的专业卡片展示。

---

## 3. 规范遵循与代码纯净度核查

1. **零注释铁律与文档外置**：`VCompetitionCard.vue` 中不包含任何解释性注释，所有排版推导与背景统一收敛记录于本文档；
2. **Google Material Design 3 风格对齐**：色彩继承 `google-blue`、`google-green`、`google-text-*` 与深色模式灰度阶梯；
3. **构建验证**：执行 `vue-tsc --noEmit && vite build`，编译与打包无任何错误与警告。
