# 对齐 Volur 登录页与 AuthForm 标题排版施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/layout/AuthLayout.vue`  
**参考来源**：`H:\Dev\src\VolurView\src\components\auth\AuthForm.vue`、`H:\Dev\src\VolurView\src\features\auth\views\AuthView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与对齐目标

通过与 `VolurView` 源代码深度比对，精确对齐 `AuthForm` 表单标题、排版间距、文字层级及视觉细节：
1. **标题与副标题核心排版 (Header Typography)**：
   - `auth-form__title`：`font-size: 24px`，`font-weight: 400`，`letter-spacing: -0.5px`，`margin: 0`；
   - `auth-form__subtitle`：`font-size: 24px`，`font-weight: 400`，`color: #767676`，`margin-top: -12px`；
   - 形成 Volur 标志性的双行等大、浅色紧随的层次排版美感。
2. **表单间距与组件样式对齐**：
   - 忘记密码链接：`margin: -20px 0 0 0`，`color: #666`，悬浮下划线；
   - 找回密码模式：对齐 `username` 与 `email` 两个字段；
   - 提交按钮：`margin-top: 12px`，`height: 48px`，`font-size: 16px`，`font-weight: 500`；
   - 底部切换链接：`color: #111`，`font-weight: 500`，悬浮纯黑与下划线。

---

## 2. 核心改动点与实施细节

### 2.1 `AuthForm.vue` 标题与排版重构
```scss
&__header {
  margin-bottom: 28px;

  .auth-form__title {
    font-size: 24px;
    font-weight: 400;
    letter-spacing: -0.5px;
    margin: 0;
  }

  .auth-form__subtitle {
    font-size: 24px;
    font-weight: 400;
    color: #767676;
    margin-top: -12px;
  }
}
```

### 2.2 找回密码表单项对齐
- 找回密码表单补齐了用户名（`username`）与邮箱地址（`email`）两个输入项及对应表单校验规则，与 Volur 原型 1:1 对齐。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行编译构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~2.34s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
- **视觉一致性**：
  - 完美实现与 VolurView 原型在标题字号、字重、颜色过渡（主标题与副标题均为 `24px` / `font-weight: 400`，副标题负间距与 `#767676`）以及表单细节上的 100% 镜像对齐。
