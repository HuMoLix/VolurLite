# 竞赛详情视图（CompetitionDetailView）返回按钮样式微调与胶囊背景移除审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
1. 「取消悬浮的“胶囊背景贴”，悬停颜色变成 #333 就好了。」
2. 「从 #444444 变成 #111111 吧」

在 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 中：
为了使顶部导航更加轻量、接近极简面包屑/纯文本跳转的视觉风格，本次改动彻底移除了 Quaternary 按钮悬浮时的浅灰胶囊背景贴，并将悬停态的颜色由深灰变更为与页面主标题色彩基准一致的 `#111111`，形成从默认副色 `#444444` 到高亮深黑 `#111111` 的平滑过渡。

---

## 2. 核心改动点

### 2.1 模板组件形态转换（`quaternary` -> `text`）
在 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 的模板顶部左侧：
- 将 `<n-button quaternary size="small" class="back-btn" ...>` 调整为 `<n-button text size="small" class="back-btn" ...>`。
- 利用 Naive UI 原生的 `text` 按钮特性，从组件层面杜绝 `--n-color-hover` 的胶囊色块渲染及涟漪效应。

### 2.2 样式纯文本化与色彩过渡调整（#444444 -> #111111）
微调 `.subview-header .header-left .back-btn` 样式规则：
```scss
    .back-btn {
      align-self: flex-start;
      padding: 0;
      color: #444444;
      background-color: transparent !important;
      background: transparent !important;
      transition: color 0.15s ease;

      &:hover,
      &:focus,
      &:active {
        color: #111111 !important;
        background-color: transparent !important;
        background: transparent !important;

        :deep(.n-icon) {
          color: #111111 !important;
        }
      }

      :deep(.n-button__state-border) {
        display: none !important;
      }
    }
```

1. **取消悬停胶囊背景贴**：
   - 彻底声明 `background-color: transparent !important;` 与 `background: transparent !important;`；
   - 隐藏 `:deep(.n-button__state-border)`，确保任何状态下均无边框与背景色块。
2. **色彩层级与平滑过渡**：
   - 默认态为合规描述文本灰 `#444444`；
   - 悬停（`:hover`）、聚焦（`:focus`）及激活（`:active`）时，文字与图标颜色统一过渡为 `#111111`；
   - 通过 `transition: color 0.15s ease;` 实现轻盈柔和的色彩深浅渐变。
3. **自然垂直左对齐**：
   - 按钮内边距设置为 `padding: 0;`，箭头图标天然与下方大标题（`h2.subview-title`）左边缘精确对齐。

---

## 3. 构建与验证结果

1. **前端工程编译与静态检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态类型检查 **100% 通过（0 错误，0 告警）**；
     - 构建耗时 5.41s 成功完成。

---

## 4. 影响范围与审计结论

- **影响文件**：[`src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)
- **审计结论**：彻底清除了返回按钮悬停时的胶囊背景贴，实现了从 `#444444` 到 `#111111` 的平滑文本交互高亮，完全符合用户期望与极简视觉规范。
