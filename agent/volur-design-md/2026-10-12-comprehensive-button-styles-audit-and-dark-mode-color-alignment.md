# 全系统前端页面按钮样式排查与 Google Material 3 暗黑色彩对齐施工记录

本文档记录对 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中全量页面与组件按钮样式的系统性校验、隐患修复与 Google Material 3 规范化对齐过程。

---

## 1. 变更背景与治理目标

在先前的迭代中，平台确立了以 **Google Material Design 3 (M3) 与 Google Workspace** 为核心的前端视觉系统，并封装了统一按钮组件 `VBtn`（`@/components/common/VBtn.vue`）。

然而，经全端 32 个包含按钮的文件深度扫描排查，发现以下几类视觉与样式隐患：
1. **暗黑模式下主要操作按钮反差色不规范 (`dark:text-google-surface-dark`)**：
   在 Google M3 体系中，暗色模式下的主色背景为 Google Blue 200 (`#a8c7fa` / `#8ab4f8`)。在浅蓝色背景上，文字必须采用高反差深墨蓝 `#062e6f`（On-Primary Container）。但部分视图历史残留了 `dark:text-google-surface-dark`（解析为 `#202124`）或缺失对比度，导致部分环境下字形辨识度不足。
2. **样式混编文件中的历史遗留黑白 Fallback**：
   `src/styles/mixins.scss` 中的 `@mixin v-primary-button` 仍保留了旧前端纯黑 `#000000` / `#222222` / `#333333` 的 fallback 值，与 Google Blue 规范相冲突，且包含多行行内解释性注释。
3. **部分业务页面中按钮组件使用不规范**：
   - `AdminDashboardView.vue` 顶部返回按钮使用了未做样式约束的裸 `n-button type="default"`；
   - `CompetitionListView.vue` 底部的“导入竞赛”按钮自由拼凑了手写的 `.competition-btn.competition-import-btn`，甚至声明了 `border: none !important;` 与 `background-color: transparent !important;`，导致次要按钮缺乏外轮廓容器边界；
   - `HomeView.vue` 中的按钮存在遗留 HTML 说明注释且未全部统一为 M3 药丸圆角。

---

## 2. 核心改动点与实施明细

### 2.1 样式基建修正 (`mixins.scss`)
- 文件：`src/volur-lite-view-NEXT/src/styles/mixins.scss`
- 改动：
  - 将 `@mixin v-primary-button` 中的默认 fallback 颜色从 `#000000`、`#222222`、`#333333` 彻底更替为 `$google-blue` (`#1a73e8`)、`$google-blue-hover` (`#1765cc`)、`$google-blue-active` (`#1557b0`)；
  - 彻底清理文件中全部解释性注释，严格遵守 `GEMINI.md` 的零注释铁律。

### 2.2 竞赛模块暗黑模式主按钮色彩校准
统一将所有在暗黑背景 `#a8c7fa` / `#8ab4f8` 上渲染的按钮文本更替为 Google M3 推荐的标准高对比深墨蓝 `#062e6f`：
- **`CompetitionDetailView.vue`**：
  - 前往登录按钮（未登录提示栏）：`dark:text-[#062e6f]`；
  - 立即报名参赛按钮（未组队控制台）：`dark:text-[#062e6f]`；
  - 进入竞赛工作台按钮（比赛进行中巨幕卡片）：`dark:text-[#062e6f]`。
- **`VCompetitionCard.vue`**：
  - 卡片右侧动作按钮（进入竞赛 / 立即报名 / 查看详情）：`dark:text-[#062e6f]`。
- **`TeamInvitationsModal.vue`**：
  - 接受组队邀请按钮：`dark:text-[#062e6f]`。
- **`TheoryCompetitionView.vue` 关联组件**：
  - `TheoryResultModal.vue`：考核成绩报告确认按钮（查看并复核答卷）暗黑模式文字统一为 `dark:text-[#062e6f]`；
  - `TheoryQuestionCard.vue`：单选与多选题选中小徽章暗黑模式文字统一为 `dark:text-[#062e6f]`；
  - `TheoryPaperSheet.vue`：提交答卷按钮与答题卡当前题号指示器暗黑模式文字统一为 `dark:text-[#062e6f]`。

### 2.3 异常视图与安全验证模块色彩校准
- **`NotFoundView.vue`**：
  - 404 页面返回上一页主按钮暗黑模式文字更替为 `dark:text-[#062e6f]`。
- **`RecaptchaModal.vue`**：
  - 验证码弹窗底部提交按钮 `.rc-submit-btn` 暗黑模式文字从 `#202124` 校准为 `#062e6f`。
- **`VNotice.vue`**：
  - 访客提示框中的登录按钮 `.notice-signin-btn` 暗黑模式文字校准为 `#062e6f`；
  - 弹窗详情底部的主要操作按钮 `.modal-action-primary-btn` 暗黑模式文字校准为 `#062e6f`。

### 2.4 业务组件收敛与坏味道清理
- **`AdminDashboardView.vue`**：
  - 移除裸露的 `<n-button>`，升级引入 `<VBtn variant="secondary" :round="true">返回主站</VBtn>`，实现完全自适应的 Google 边框与明暗过渡。
- **`HomeView.vue`**：
  - 移除所有 HTML 冗余注释；
  - 为所有演示按钮补齐 `round` 药丸属性，维持全局视觉语言一致。
- **`CompetitionListView.vue`**：
  - 将手写拼凑且隐式缺失边框的 `<n-button class="competition-btn competition-import-btn">导入竞赛</n-button>` 升级重构为 `<VBtn variant="secondary" :round="true" class="mx-auto" @click="handleImportClick">导入竞赛</VBtn>`；
  - 彻底删除 SCSS 样式块中过时、冗余的 100 多行 `.competition-btn` 与 `.competition-import-btn` 规则，消除样式死代码；
  - 同步清理掉未被引用的 `NButton` 导入，消除 TypeScript 编译告警。

---

## 3. 构建与验证结果

执行全量类型校验与生产构建测试：
```bash
npm run build
# 执行 vue-tsc --noEmit && vite build
```

**验证结果**：
- `vue-tsc` 0 错误、0 警告；
- Vite 打包耗时 13.94s，所有页面与 CSS 分包（包含 `CompetitionDetailView`, `CompetitionListView`, `TheoryCompetitionView`, `VBtn`, `PasskeyModal`, `RecaptchaModal` 等）均编译成功，Exit Code 为 0。
