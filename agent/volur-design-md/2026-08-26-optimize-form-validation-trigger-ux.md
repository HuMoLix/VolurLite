# 优化表单交互逻辑（静默空项校验与提交时统一触发）施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/views/auth/ResetView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 优化背景与交互目标

在原表单配置中，必填规则配置了 `trigger: 'blur'`，导致用户初次点击输入框失焦后，即使尚未开始填写就会激进地提示“XXX 不能为空”，打断了平滑的用户输入心流。

### 交互优化目标：
1. **交互时静默原则 (Passive Empty Handling)**：
   - 当输入框为空时，在用户点击、聚焦、失焦期间**绝不主动报错提示“XXX 不能为空”**；
   - 当用户实际键入内容时，实时进行格式校验（如用户名合法字符、密码长度与复杂度等），一旦删空则自动静默；
2. **提交时统一兜底检验 (Submit-Triggered Required Validation)**：
   - 当用户点击「继续」、「注册」、「重置密码」等提交按钮（触发 `formRef.value?.validate()`）时，再统一检查是否有必填项遗漏，并精准定位提示“XXX 不能为空”。

---

## 2. 核心改动点与实施细节

### 2.1 必填项规则移除 `trigger: 'blur'` 声明
根据 Naive UI / `async-validator` 机制，未声明 `trigger` 的 `FormRule` 仅在显式调用 `formRef.validate()` 时才执行校验：
```typescript
username: [
  { required: true, message: t('auth.username_required') }, // 👈 不设 trigger，提交时才触发
  {
    validator(_rule: FormItemRule, value: string) {
      if (!value) return true // 👈 为空时静默通过，不阻碍输入心流
      if (value.length < 3 || value.length > 32) {
        return new Error(t('auth.username_length'))
      }
      if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
        return new Error(t('auth.username_pattern'))
      }
      return true
    },
    trigger: ['blur', 'input'] // 👈 有实际内容时实时辅助检验
  }
]
```

### 2.2 影响组件范围
- **[`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：登录表单、注册表单、找回密码表单；
- **[`ResetView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/auth/ResetView.vue)**：新密码设置表单与确认密码表单。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~5.61s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/views/auth/ResetView.vue`
- **体验提升**：
  - 彻底解决了初次进入页面或在各输入框之间切换时光标失焦即弹出“不能为空”的骚扰性提示，使得交互体验极致丝滑、符合顶级现代化 Web 应用规范。
