# 审计与施工记录：调整认证主卡片 auth-card 最小高度为 390px

## 1. 变更背景与需求目标
根据最新交互视觉微调需求，针对新一代前端（`volur-lite-view-NEXT`）中通用认证布局组件 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 的桌面端卡片几何规格进行精准微调：
- 将桌面端（`@media (min-width: 840px)`）下认证主卡片容器 `.auth-card` 的最小高度 (`min-height`) 由 `380px` 调整为 `390px`。

---

## 2. 改动详情

### 2.1 涉及文件
- [`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)

### 2.2 样式微调
```scss
  // 电脑端：@media 切换为白底 (暗色为深灰卡片底) 无边框圆角卡片，Logo 置顶，左侧标题与右侧表单由 flex 管理且同行对齐
  @media (min-width: 840px) {
    flex-direction: column;
    max-width: 1000px;
    min-height: 390px;
    padding: 36px 44px;
    background-color: var(--v-bg-card, #ffffff) !important;
    border: none !important;
    border-radius: 28px !important;
    box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.08) !important;
    ...
```

---

## 3. 安全隐患说明与影响范围

1. **安全隐患**：本次修改仅调整 CSS 样式属性，不涉及任何网络请求、认证凭据或 DOM 结构变动，零安全风险。
2. **影响范围**：
   - 电脑端视口（宽度 $\ge 840\text{px}$）下所有基于 `AuthLayout` 的页面（如 `/v1/signin/identifier` 账号识别页、`/v1/signin/challenge` 密码挑战页等）的卡片最小高度将维持在 `390px`，视觉纵向呼吸空间更自然；
   - 移动端视口（宽度 $< 840\text{px}$）继续保持 `min-height: unset` 自适应无缝排列，不受任何影响。
