# 修复表单校验多条错误信息重叠堆叠展示施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/views/auth/ResetView.vue`、`src/volur-lite-view/src/locales/zh_CN/auth.json`、`src/volur-lite-view/src/locales/en_US/auth.json`、`agent/volur-design-skills/view-Rules.md`  
**责任主体**：Antigravity AI Agent

---

## 1. 缺陷现象与根本原因分析

### 现象描述：
用户在注册表单中输入短密码（如 `test`）时，输入框下方同时弹出两段堆叠的红色报错：
> 1. 密码长度必须在 6 到 32 个字符之间  
> 2. 密码必须包含字母、数字和至少一个特殊字符，且长度在 6 到 32 个字符之间

### 根本原因排查：
Naive UI / `async-validator` 在配置了多个独立的静态规则对象（`min/max` 规则和 `pattern` 正则规则）时，校验引擎会并行执行数组中的所有规则并将所有不满足的错误信息**同时返回并垂直堆叠展示**，导致用户在长度尚未达标时就同时接收到字符复杂度的冗余提示。

---

## 2. 修复方案与实施细节

### 2.1 采用顺序阶梯校验器 (Sequential Step Validator)
重构 `registerRules` 与重置密码 `rules`，将字段校验逻辑收敛在单一 `validator` 函数中进行阶梯判断：
1. **第一步（长度校验）**：检查字符串长度是否在 `[6, 32]`（用户名 `[3, 32]`），不满足则立即返回 `t('auth.password_length')` 并中断；
2. **第二步（复杂度校验）**：当且仅当长度已达标时，才执行正则匹配，不满足返回 `t('auth.password_pattern')`；
3. 确保在任何时刻，一个输入框下方**仅显示一条最关键、最优先级的错误提示**。

### 2.2 规范与文案优化
- 在 `zh_CN/auth.json` 与 `en_US/auth.json` 中将正则文案简化为“密码必须包含字母、数字和至少一个特殊字符”，剥离冗余的重复长度描述；
- 在 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md#L530) 第 6.4 节补充了此项避免多重错误堆叠的最佳实践规范。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~2.40s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/views/auth/ResetView.vue`
  - `src/volur-lite-view/src/locales/zh_CN/auth.json`
  - `src/volur-lite-view/src/locales/en_US/auth.json`
  - `agent/volur-design-skills/view-Rules.md`
- **体验提升**：
  - 彻底解决了注册及密码重置时多条错误提示重叠冗余的问题，交互层级清晰、反馈精准。
