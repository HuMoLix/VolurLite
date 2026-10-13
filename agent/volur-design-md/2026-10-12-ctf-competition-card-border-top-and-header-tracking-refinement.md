# CTF 竞技场卡片分割线、Hover 边框与竞赛标题字间距优化方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端工程中，根据赛题卡片与顶部导航栏最新的视觉一致性与交互反馈要求，进行以下优化与缺陷修复：

1. **赛题卡片未渲染边框与分割线的根本原因审计**：
   - 本工程的 `tailwind.config.ts` 中为了保护 Naive UI 原生按钮、输入框及表单控件的样式纯洁性，配置了 `corePlugins: { preflight: false }`；
   - 禁用 Preflight 后，Tailwind CSS 不会注入全局属性重置（`*, ::before, ::after { border-style: solid; border-width: 0; }`）；
   - 在标准浏览器模型中，普通 HTML `<div>` 元素的默认 `border-style` 初始值为 `none`；
   - 单纯在模板中使用 Tailwind 的 `.border`（仅生成 `border-width: 1px;`）、`.border-t`（仅生成 `border-top-width: 1px;`）以及 `.hover:border-google-blue`（仅改变 `border-color`），由于缺少 `border-style: solid;` 声明，浏览器默认将其视为无边框样式（`none`），导致**整个卡片的外边框、卡片下方的 border-top 以及鼠标 hover 状态的高亮边框均完全无法在页面中视觉呈现**。
2. **赛题卡片底部统一边框分割线 (Border-Top)**：
   - 此前在 `CtfCompetitionChallengeCard.vue` 中，仅在存在前三解题战队（`topTeamsList.length > 0`）时才渲染底部分割线与队伍行；未解出状态下整个底部区域被条件性移除，导致网格中的卡片缺乏统一的视觉结构；
   - 需对无论是否有人解出的所有赛题卡片，均在底部呈现标准上边框（`border-top: 1px solid #dadce0` / 暗色 `#3c4043`），并且未解出时呈现对齐的占位文案（`暂无队伍解出`），保持网格整齐统一。
3. **赛题卡片悬停反馈边框 (Hover Border)**：
   - 为卡片显式配置符合 Google Material 主题的 Hover 边框动效（亮色 `#1a73e8` / 暗色 `#8ab4f8`），并带有平滑过渡。
4. **顶部导航栏标题字间距微调 (Letter-Spacing)**：
   - 在 `VCompetitionHeader.vue` 中，此前比赛主标题使用 Tailwind 的 `tracking-tight`（`-0.025em`），排版较为紧凑；
   - 需将字间距显式调整为 `0.025em`（即 `tracking-[0.025em]` / `letterSpacing: 0.025em`），提升标题辨识度与舒展度。

---

## 2. 核心改动点与技术实现

### 2.1 赛题卡片 Scoped CSS 架构重构 (`CtfCompetitionChallengeCard.vue`)

1. **显式补全 Scoped Border 样式铁律**：
   - 针对 Preflight 禁用导致的 `border-style: none` 缺陷，在 `CtfCompetitionChallengeCard.vue` 底部引入 `<style scoped lang="scss">`：
     ```scss
     .ctf-challenge-card {
       border: 1px solid #dadce0;
       transition: border-color 0.2s cubic-bezier(0.2, 0, 0, 1), box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1);

       &:hover {
         border-color: #1a73e8;
       }
     }

     :global(.dark) .ctf-challenge-card,
     .dark .ctf-challenge-card {
       border-color: #3c4043;

       &:hover {
         border-color: #8ab4f8;
       }
     }

     .challenge-card-footer {
       border-top: 1px solid #dadce0;
     }

     :global(.dark) .challenge-card-footer,
     .dark .challenge-card-footer {
       border-top-color: #3c4043;
     }
     ```
   - 彻底摆脱纯原子化类名对浏览器默认 `border-style` 的不确定依赖，确保外边框、Hover 高亮边框和底部 `border-top` 100% 渲染生效。
2. **统一上边框与分支渲染**：
   - 底部容器脱离单一 `v-if` 条件，改为持久渲染的 `.challenge-card-footer` 容器；
   - 在其内部进行分支判断：
     - 若 `topTeamsList.length > 0`：正常渲染前三血战队徽标、截断名称及精准锚定的 `VTooltip`；
     - 若 `topTeamsList.length === 0`：渲染 `text-xs text-google-text-muted dark:text-google-text-darkMuted` 级别的文本提示 `暂无队伍解出`（`t('competition.no_teams_solved')`），并通过 `h-[14px]` 严格保障未解出状态与已解出战队行的基准行高完全一致，彻底消除卡片内容排版高度抖动。

### 2.2 竞赛顶部导航栏标题字间距调整 (`VCompetitionHeader.vue`)

1. **移除紧凑字间距**：
   - 移除原 `tracking-tight`（`-0.025em`）；
2. **设置扩展字间距**：
   - 在 `h1` 类名中配置 `tracking-[0.025em]`，并在内联样式 `:style` 中注入 `letterSpacing: '0.025em'`，确保无论在任何响应式断点与自定义样式合并中均精准呈现 `0.025em` 字间距。

---

## 3. 规范与安全边界审查

1. **纯粹代码与零注释原则**：所有被修改的 `.vue` 文件中严格保持代码自解释，杜绝任何行内解释性注释与设计思路备注；
2. **主题设计体系对齐**：Hover 边框严格采用 Google Blue（亮色 `#1a73e8` / 暗色 `#8ab4f8`），底部分割线采用 Google Gray Border（亮色 `#dadce0` / 暗色 `#3c4043`），严格遵循 Google Material Design Workspace 颜色规范；
3. **国际化支持**：空状态文本使用既有字典 `t('competition.no_teams_solved')`，支持中英双语和繁体自动适配。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/competition/components/CtfCompetitionChallengeCard.vue`
  - `src/volur-lite-view-NEXT/src/components/competition/VCompetitionHeader.vue`
- **验证结果**：
  - 执行 `npm run build`（Vite + vue-tsc）：成功编译打包（`built in 12.02s`），产物中 Scoped CSS 正确注入 `.ctf-challenge-card` 与 `.challenge-card-footer` 样式规则，零 TypeScript 报错。
