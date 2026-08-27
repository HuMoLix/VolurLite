# AdminBroadcastView 排版布局与统计卡片对齐施工记录

**施工日期**：2026-08-27  
**施工模块**：`src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`  
**参考来源**：`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与对齐目标

为了保持 Volur Lite 后台管理系统各模块间设计风格的高度统一，针对广播通知管理页面（`AdminBroadcastView.vue`）的排版布局与顶部卡片进行重构：
1. **顶部统计卡片（Metric Statistics Cards）**：
   - 彻底废除旧版较重且占空间的 `n-grid` + `n-card` 结构；
   - 切换为与 `AdminUserView.vue` 100% 对齐的单行紧凑轻量指标带（`.stat-ribbon` + `.stat-mini-card`）；
   - 左侧采用统一圆角图标容器（`38px * 38px`，图标尺寸 `18px`），右侧纵向排列数值（`font-size: 18px; font-weight: 700`）与描述标签（`font-size: 12px; color: #444444`）。
2. **整体容器与间距统一**：
   - 根容器 `.admin-broadcast-view` 的 `gap` 调整为与 `AdminUserView.vue` 一致的 `16px`；
   - 响应式自适应断点保持一致（大屏 4 列、中屏 2 列、小屏 1 列）。
3. **依赖精简与规范对齐**：
   - 移除无用的 `NGrid` 和 `NGridItem` 组件导入；
   - 遵循 `view-Rules.md` 规范中的主操作按钮黑色背景（`#000000`）与文字颜色（`#444444`）标准。

---

## 2. 核心改动点与实施细节

### 2.1 统计卡片结构重构 (stat-ribbon & stat-mini-card)
```html
<!-- Top section: Compact Metrics Statistics Cards in a single row -->
<div class="stat-ribbon">
  <div class="stat-mini-card">
    <div class="stat-icon-wrapper primary">
      <n-icon size="18">
        <MegaphoneOutline />
      </n-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value">{{ statData.totalAnnouncements }}</div>
      <div class="stat-label">全站系统公告</div>
    </div>
  </div>

  <div class="stat-mini-card">
    <div class="stat-icon-wrapper warning">
      <n-icon size="18">
        <FlameOutline />
      </n-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value">{{ statData.totalPromotions }}</div>
      <div class="stat-label">运营活动与推广</div>
    </div>
  </div>

  <div class="stat-mini-card">
    <div class="stat-icon-wrapper error">
      <n-icon size="18">
        <AlertCircleOutline />
      </n-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value">{{ statData.urgentModals }}</div>
      <div class="stat-label">紧急弹窗广播</div>
    </div>
  </div>

  <div class="stat-mini-card">
    <div class="stat-icon-wrapper success">
      <n-icon size="18">
        <RadioOutline />
      </n-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value status-badge-text">正常运行</div>
      <div class="stat-label">SSE 实时推送总线</div>
    </div>
  </div>
</div>
```

### 2.2 样式重构 (SCSS)
```scss
.admin-broadcast-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.stat-mini-card {
  background-color: var(--n-color, #ffffff);
  border-radius: 8px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease-in-out;

  &:hover {
    border-color: rgba(0, 0, 0, 0.12);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  }

  .stat-icon-wrapper {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.primary {
      background-color: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &.warning {
      background-color: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    &.success {
      background-color: rgba(16, 185, 129, 0.1);
      color: #10b981;
    }

    &.error {
      background-color: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    .stat-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      line-height: 1.2;

      &.status-badge-text {
        font-size: 15px;
        color: #10b981;
      }
    }

    .stat-label {
      font-size: 12px;
      color: #444444;
      font-weight: 500;
      white-space: nowrap;
    }
  }
}
```

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建测试：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~8.35s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`
- **视觉一致性**：
  - `AdminBroadcastView` 与 `AdminUserView` 在整体卡片高度、图标风格、响应式网格断点、字体字号与颜色上实现了完全统一，消除了此前卡片样式不一致的问题。
