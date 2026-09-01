# 施工与审计记录：个人中心原密码国际化 (i18n) 键名补齐

## 1. 基本信息

- **任务名称**：补齐个人中心原登录密码相关国际化文案（中英双语）
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-lite-view`：
    - `src/locales/zh_CN/profile.json`（补齐 `old_password_label`、`old_password_placeholder`、`old_password_required`）
    - `src/locales/en_US/profile.json`（补齐 `old_password_label`、`old_password_placeholder`、`old_password_required`）

---

## 2. 国际化词条对照

| 键名 (Key) | 中文 (zh_CN) | 英文 (en_US) |
|---|---|---|
| `profile.old_password_label` | 原登录密码 | Current Password |
| `profile.old_password_placeholder` | 请输入当前使用的原登录密码 | Enter your current password |
| `profile.old_password_required` | 请输入原登录密码 | Please enter your current password |

---

## 3. 验证结论

- **前端构建**：执行 `npm run build`，编译打包 100% 通过（0 错误）。
