# 个人中心移除用户名称旁身份胶囊施工审计文档

## 1. 变更背景与需求
在个人中心页面 [`ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue) 的顶部 Hero 卡片区域，原设计在用户名称（`profile.username`）旁边渲染了一个表示用户角色（如 `USER` / `ADMIN`）的身份胶囊标签（`<n-tag class="role-badge">`）。

根据界面视觉优化需求，用户名称旁不再需要展示该身份胶囊，需将该标签及其无用样式完整移除。

---

## 2. 详细改动点说明

### 2.1 模板改动
- **文件**：[`src/volur-lite-view/src/views/profile/ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)
- **改动位置**：在 `.hero-info > .username-row` 容器中，移除了 `<n-tag>` 身份标签组件：
  ```html
  <!-- 移除前 -->
  <div class="username-row">
    <h1 class="username-text">{{ profile.username }}</h1>
    <n-tag
      size="small"
      :bordered="false"
      :type="userStore.isAdmin ? 'warning' : 'info'"
      class="role-badge"
    >
      {{ profile.role }}
    </n-tag>
  </div>

  <!-- 移除后 -->
  <div class="username-row">
    <h1 class="username-text">{{ profile.username }}</h1>
  </div>
  ```

### 2.2 样式清理
- **改动位置**：同步清理 SCSS 样式块中不再使用的 `.role-badge` 规则，保持样式表精简无冗余：
  ```scss
  .username-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .username-text {
      font-size: 24px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      margin: 0;
      line-height: 1.2;
    }
  }
  ```

---

## 3. 安全隐患修复与影响范围说明

1. **安全性说明**：
   - 角色信息仅是在展示层移除了只读胶囊展示，后端鉴权体系与前端路由守卫/`userStore` 权限判断逻辑均不受影响，无任何安全漏洞或隐患。
2. **影响范围**：
   - 仅影响个人中心页面（`ProfileView.vue`）头部英雄卡片中的用户名展示区域，其他个人信息、隐私设置、安全中心、通知中心等业务功能及组件均保持原有行为。

---

## 4. 验证与审计结果

- **类型检查与构建测试**：在 `src/volur-lite-view` 下执行 `npm run build`（包含 `vue-tsc --noEmit && vite build`）：
  - TypeScript 类型检查 0 错误；
  - Vite 打包构建成功完成，无任何警告与异常。
