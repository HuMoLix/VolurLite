# VCompetitionCard 标题悬停、暗夜模式配色与竞赛时间排版调整

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT` 工程的竞赛卡片通用组件 [VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 中，原实现存在以下交互与视觉排版细节需优化：
1. **标题悬停高亮逻辑解绑**：原卡片标题配置了 `group-hover:text-google-blue dark:group-hover:text-google-blue-dark`，导致用户悬停在卡片任一区域时标题颜色发生突变，不符合当前整体沉浸式极简卡片交互理念，需去除标题 hover 变色效果；
2. **暗夜模式色彩对比度强化**：
   - 标题文本在暗夜模式下原为柔和暗灰（`#e3e3e3`），需提升至高对比亮白 `#FCFCFC`；
   - 竞赛描述容器（`.comp-desc`）在暗夜模式下原为 `#bdc1c6`，调整为更加协调的 `#D4D4D4`；
3. **竞赛起止时间规范化**：
   - 格式统一为 `YYYY-MM-DD hh-mm-ss - YYYY-MM-DD hh-mm-ss`（时间单位间以连字符 `-` 连接，时间区间采用短横线 ` - ` 分隔）；
   - 暗夜模式文字颜色设为 `#FCFCFC`；
   - 字体指定为 `Inter, sans-serif`，保证数字与字母等宽对齐和现代排版质感。

---

## 2. 核心改动点与实现方案

### 2.1 标题悬停效果移除与暗夜高对比色配置
在 [VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 中：
- 移除了 `h3` 元素上的 `group-hover:text-google-blue dark:group-hover:text-google-blue-dark transition-colors` 悬停变色样式；
- 将暗夜模式文本颜色类更新为 `dark:text-[#FCFCFC]`，亮色模式保持 `text-google-text-primary`。

### 2.2 竞赛描述暗夜颜色精细化微调
- 模板层的描述容器更新为 `dark:text-[#D4D4D4]`；
- 针对降维扁平化渲染的 Markdown 内容，同步将 `<style scoped lang="scss">` 中的 `html.dark .comp-desc` 文字颜色由 `#bdc1c6` 更新为 `#D4D4D4`，确保多行截断及富文本标签继承统一色彩。

### 2.3 竞赛时间格式化函数与样式重构
- **时间格式化函数** (`formatDateTime`)：重构输出字符串逻辑，将时、分、秒的分隔符调整为连字符 `-`，输出精确的 `YYYY-MM-DD hh-mm-ss` 格式；
- **起止区间连接符**：由原波浪号 ` ~ ` 替换为短横线连接 ` - `；
- **排版与字体**：
  - 抽离专有类名 `.comp-time-val` 并移除原 `font-mono`；
  - 在 Scoped SCSS 中显式声明 `font-family: Inter, sans-serif`；
  - 声明 `html.dark .comp-time-val { color: #FCFCFC; }`，并配合 Tailwind `dark:text-[#FCFCFC]` 形成暗夜高亮保障，亮色模式下维持柔和深色以确保白底下的高对比可读性。

---

## 3. 安全与规范核查

1. **零注释铁律**：[VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 代码内未添加任何行内注释，保持代码纯粹自解释；
2. **构建验证**：执行 `vue-tsc --noEmit && vite build` 生产打包全部通过，无类型报错与样式冲突。

---

## 4. 影响范围

- 修改文件：[src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue)
- 关联视图：[src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue)
