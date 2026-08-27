# 修复 Vue I18n 邮箱占位符 `@` 符号编译解析异常施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/locales/zh_CN/auth.json`、`src/volur-lite-view/src/locales/en_US/auth.json`  
**责任主体**：Antigravity AI Agent

---

## 1. 缺陷现象与根本原因排查

### 现象描述：
用户在认证页面点击切换到「注册」或「找回密码」模式时，控制台抛出严重渲染错误：
```
SyntaxError: Message compilation error: Invalid linked format
1 | name@example.com
  |      ^
```
导致页面组件崩溃并出现 `Unhandled error during execution of render function at <FormItem path="email">`。

### 根本原因排查：
在 `vue-i18n` 规范中，`@` 字符为保留的 **Linked Message 引用前缀语法**（如 `@:message.key` 或 `@.modifier:key`）。
当 `auth.json` 中的占位符字符串为 `"email_placeholder": "name@example.com"` 时，`vue-i18n` 解析器会将 `@example.com` 误当做 Linked 语法解析，由于 `.com` 不是合法链接分隔符而直接抛出语法解析异常。

---

## 2. 修复方案与实施细节

根据 `vue-i18n` 官方字面量转义规则，使用 `{'@'}` 对 `@` 进行转义：
- **`src/locales/zh_CN/auth.json`**：
  ```json
  "email_placeholder": "name{'@'}example.com"
  ```
- **`src/locales/en_US/auth.json`**：
  ```json
  "email_placeholder": "name{'@'}example.com"
  ```

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~2.37s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/locales/zh_CN/auth.json`
  - `src/volur-lite-view/src/locales/en_US/auth.json`
- **修复效果**：
  - 注册与找回密码表单中 `email_placeholder` 渲染恢复正常，切换模式不再发生异常崩溃。
