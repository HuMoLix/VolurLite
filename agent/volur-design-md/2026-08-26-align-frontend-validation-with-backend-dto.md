# 前端表单校验与后端 DTO 约束 1:1 对齐规范施工记录

**施工日期**：2026-08-26  
**施工模块**：`agent/volur-design-skills/view-Rules.md`、`GEMINI.md`、`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/views/auth/ResetView.vue`、`src/volur-lite-view/src/locales/zh_CN/auth.json`、`src/volur-lite-view/src/locales/en_US/auth.json`  
**参考来源**：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java`  
**责任主体**：Antigravity AI Agent

---

## 1. 规范背景与对齐目标

为彻底消除“前端校验通过但后端 DTO 报 400 校验错误”的坏味道反模式，在 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 和 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 中正式确立：
**前端表单校验必须 100% 严格参考后端 DTO 中的 Jakarta Validation 注解约束**。

### 核心约束对照：
以 [`RegisterRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java) 为基准：
1. **用户名 (`username`)**：
   - 必填：`@NotBlank`
   - 长度：`@Size(min = 3, max = 32)`
   - 正则：`@Pattern(regexp = "^[a-zA-Z0-9_-]+$")`
2. **密码 (`password`)**：
   - 必填：`@NotBlank`
   - 长度：`@Size(min = 6, max = 32)`
   - 正则（复杂度）：`@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32}$")`
3. **邮箱 (`email`)**：
   - 必填：`@NotBlank`
   - 格式：`@Email`

---

## 2. 核心改动点与实施细节

### 2.1 规范文档固化 ([`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md))
- 在 `view-Rules.md` **第 6.4 节** 新增「前端表单校验与后端 DTO 校验约束严格 1:1 对齐规范」，并提供详细注解映射矩阵与以 `RegisterRequestDTO.java` 为例的正确代码范例；
- 在 **第 12 节合规速查清单** 中增加 DTO 表单校验合规核验项。

### 2.2 根协作指令同步 ([`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md))
- 在 `GEMINI.md` 第 1.2 节补充第 8 条前端核心规范准则。

### 2.3 业务组件校验规则全面落地
- **[`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：更新 `registerRules`，加入 3~32 字符限制、用户名合法字符正则、密码 6~32 字符及包含字母、数字和特殊字符的复杂度正则校验；
- **[`ResetView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/auth/ResetView.vue)**：更新重置密码规则，加入密码复杂度正则校验；
- **中英文多语言字典**：补齐双语详细错误提示文案。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~5.36s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `agent/volur-design-skills/view-Rules.md`
  - `GEMINI.md`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/views/auth/ResetView.vue`
  - `src/volur-lite-view/src/locales/zh_CN/auth.json`
  - `src/volur-lite-view/src/locales/en_US/auth.json`
- **体验与安全性**：
  - 杜绝了前后端校验规则不一致导致的不良交互反馈，确保所有入参在前端与后端保持高度统一的标准与鲁棒性。
