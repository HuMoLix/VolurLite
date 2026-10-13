# 战队信息模态框（TeamFormModal）按钮样式体系化重构为 competition-btn

## 1. 变更背景与需求概述

在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中：
1. **设计语言割裂问题**：
   - 此前在竞赛模块的列表页（`CompetitionListView.vue`）和详情页（`CompetitionDetailView.vue`）中，已经确立了标准次级与操作按钮样式类 `.competition-btn` 及其变体（包含 36px 物理高度、12px 圆角、14px / 500 字重 Inter 字体、透明底色、Google 风格灰色边框与微渐变 Hover 反馈）；
   - 在战队报名、查看与编辑的综合模态框 `TeamFormModal.vue` 中，底栏操作按钮以及名册列表头部的按钮仍使用内联 Tailwind 类名（如 `h-9 px-4 rounded-xl border ... text-xs font-semibold`）或 Naive UI 原始的 `<n-button size="small" secondary>`，导致出现 12px（`text-xs`）小字号排版与圆角落差；
   - 主操作按钮（“提交报名”、“编辑战队信息”、“提交更新”）与危险操作按钮（“解散战队”）使用散落的内联类名，未收敛进统一的色彩与触控反馈体系；
2. **重构目标**：
   - 全面梳理并分析 `TeamFormModal.vue` 中的所有按钮样式；
   - 将“添加成员行”、“添加领队行”由原始 `<n-button>` 升级为原生 `<button class="competition-btn">`，统一 16px 图标与 14px 文本的黄金垂直对齐；
   - 底栏二级操作（“取消”、“关闭”、“取消修改”）统一为标准 `.competition-btn`；
   - 底栏主操作（“提交报名”、“编辑战队信息”、“提交更新”）升级为 `.competition-btn.competition-btn-primary`，在日间模式应用 Google Blue（`#1a73e8`）白字，在黑夜模式应用反色高对比（`#8ab4f8` / `#202124`）；
   - 底栏解散战队危险操作升级为 `.competition-btn.competition-btn-danger`，与 Google Red 色系严格对齐；
   - 在 `TeamFormModal.vue` 的 `<style scoped lang="scss">` 中完整定义 `.competition-btn`、`.competition-btn-primary` 与 `.competition-btn-danger`，并支持 `:disabled` 禁用态与暗夜模式适配。

---

## 2. 按钮样式现状分析与重构对照

| 按钮角色 / 位置 | 重构前样式与结构 | 重构后样式与结构 | 规范优化点 |
| :--- | :--- | :--- | :--- |
| **添加成员行** (名册区) | `<n-button size="small" secondary>` | `<button type="button" class="competition-btn">` | 统一 36px 高度、12px 圆角与 14px Inter 字体，消除 Naive UI 组件嵌套包裹层 |
| **添加领队行** (领队区) | `<n-button size="small" secondary>` | `<button type="button" class="competition-btn">` | 与成员名册按钮保持完全一致的排版比例与悬浮态反馈 |
| **取消 / 关闭 / 取消修改** (底栏) | 内联 Tailwind 类 (`h-9 px-4 rounded-xl border ... text-xs font-semibold`) | `<button type="button" class="competition-btn">` | 字号从 12px 规范提升为 14px，padding 统一为 16px，边框色适配日夜模式 |
| **提交报名 / 编辑战队 / 提交更新** (底栏主动作) | 内联 Tailwind 类 (`h-9 px-5 rounded-xl bg-google-blue ... text-xs font-semibold`) | `<button type="button" class="competition-btn competition-btn-primary">` | 抽象为主色变体，日间 `#1a73e8`，暗夜高对比反色 `#8ab4f8` / `#202124` |
| **解散战队** (底栏危险操作) | 内联 Tailwind 类 (`h-9 px-4 rounded-xl border border-red-200 ... text-xs font-semibold`) | `<button type="button" class="competition-btn competition-btn-danger">` | 抽象为危险色变体，轻透红色背景搭配 Google Red 边框与文字，悬浮加深反馈 |
| **头像右上角移除角标** | `absolute w-5 h-5 rounded-full ...` | 保持为专用角标结构 | 属于专用圆形徽章，不属于主按钮体系，保留原有内联绝对定位规则 |
| **表格行内操作** (移除/重新邀请) | 表格单元格专用紧凑型按钮 (28px 行高) | 保持为紧凑型操作 | 单元格高度受限，保留专用小尺寸排版避免破坏表格垂直韵律 |

---

## 3. 核心改动明细

### 3.1 模板结构升级 (`TeamFormModal.vue`)
- **成员名册“添加成员行”**：
  ```html
  <button
    v-if="isEditing"
    type="button"
    class="competition-btn"
    :disabled="isMaxMembersReached"
    @click="addMemberRow"
  >
    <n-icon :size="16" :component="AddOutline" />
    <span>{{ t('competition.add_member_row', '添加成员行') }}</span>
  </button>
  ```
- **领队名册“添加领队行”**：
  ```html
  <button
    v-if="isEditing"
    type="button"
    class="competition-btn"
    :disabled="isMaxLeadersReached"
    @click="addLeaderRow"
  >
    <n-icon :size="16" :component="AddOutline" />
    <span>{{ t('competition.add_leader_row', '添加领队行') }}</span>
  </button>
  ```
- **底栏“解散战队”**：
  ```html
  <button
    type="button"
    class="competition-btn competition-btn-danger"
    :disabled="disbanding"
  >
    <n-icon :size="16" :component="TrashOutline" />
    <span>{{ t('competition.disband_team', '解散战队') }}</span>
  </button>
  ```
- **底栏二级与主操作**：
  - 二级按钮统一使用 `class="competition-btn"`；
  - 主要动作统一使用 `class="competition-btn competition-btn-primary"`，加载微标使用 `<n-spin v-if="submitting" :size="14" stroke="currentColor" />`，自适应当前文字前景色。

### 3.2 样式层收敛 (`<style scoped lang="scss">`)
在模态框 scoped SCSS 中完整补齐：
- **`.competition-btn` 基础类**：
  - 几何与字体：高度 36px，圆角 12px，内边距 `0 16px`，`font-size: 14px`，`font-weight: 500`，`line-height: 20px`，Inter 字体；
  - 边框与背景：亮色默认边框 `#d7d8da`，文字 `#2b2d31`，透明背景；暗色默认边框 `#333`，文字 `#fcfcfc`；
  - 悬停反馈：亮色 `rgba(60, 64, 67, 0.04)`，暗色 `rgba(255, 255, 255, 0.04)`；
  - 禁用状态：`:disabled, &[disabled]` 时 `cursor: not-allowed` 且 `opacity: 0.55`；
  - Naive UI 兼容层：抑制波纹及默认边框冲突。
- **`.competition-btn-primary` 主要操作变体**：
  - 亮色：Google Blue 实色填充 `#1a73e8`，文字纯白，悬浮 `#1765cc`，按压 `#1557b0`；
  - 暗色：高对比度 `#8ab4f8` 填充，文字 `#202124`，悬浮 `#9ec1fa`。
- **`.competition-btn-danger` 危险操作变体**：
  - 亮色：轻透红色背景 `rgba(234, 67, 53, 0.05)`，边框 `rgba(234, 67, 53, 0.3)`，文字 `#ea4335`，悬浮时背景 `0.1` / 边框 `0.45`；
  - 暗色：轻透底色 `rgba(242, 139, 130, 0.08)`，边框 `rgba(242, 139, 130, 0.3)`，文字 `#f28b82`，悬浮时背景 `0.16` / 边框 `0.45`。

---

## 4. 质量与安全验证

- **零注释原则合规**：代码文件保持绝对纯净，未包含任何冗余行内解释性注释；
- **全量构建验证**：执行 `npm run build`（`vue-tsc -b && vite build`）通过，无任何语法、类型及 SCSS 编译警告，产物正确生成；
- **前后端对齐验证**：各按钮触发的业务方法、表单校验流及 API 调用均保持原样生效，无任何交互降级或功能遗漏。
