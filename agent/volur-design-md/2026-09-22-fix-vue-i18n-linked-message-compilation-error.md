# 审计记录：修复 Vue I18n 特殊字符编译异常导致页面渲染中断缺陷

## 1. 变更背景与问题定位

在访问管理端风控规则页面 `/admin/risk/rules` 时，浏览器控制台抛出严重的未捕获运行时语法错误，导致前端组件树渲染崩溃，页面无法正常呈现：

```text
[Vue warn]: Unhandled error during execution of render function 
  at <TabPane name="email" tab="邮件策略" key="email" > 
  ...
SyntaxError: Message compilation error: Invalid linked format
1  |  规则说明：匹配输入域名及其所有子域名（如配置 edu.cn 时，*.edu.cn 均可通行）。系统已自动忽略大小写及 @ 前缀。
   |                                                              ^
```

### 根本原因深度分析
1. **Vue I18n 保留标识符冲突**：
   在 `vue-i18n` 规范中，字符 `@` 是**链接消息（Linked Messages）**的专用前缀符号（标准语法为 `@:message.key` 或 `@.modifier:message.key`）。
2. **非转义字符的编译断言失败**：
   在国际化配置文件 `src/volur-lite-view/src/locales/zh_CN/admin/risk.json` 与 `en_US/admin/risk.json` 中，邮箱白名单策略相关的多条文案包含原始字符 `@`（例如：`"@ 前缀"`、`"@{suffix}"`、`"Leading @ and"`）。
   当 Vue 组件挂载执行 `t('admin.risk.policy.email.input_hint')` 或渲染模版插槽时，Vue I18n 消息编译器将 `@` 解析为 Linked Message 语法，在后续未探测到合法前缀格式时判定为 `Invalid linked format` 并直接抛出编译异常，导致当前 `<n-tab-pane name="email">` 渲染函数中断，触发 Vue Router 导航捕获异常。
3. **潜在风险排查**：
   经全局静态 AST 与动态 I18n 编译器深度遍历扫描，在 `src/volur-lite-view/src/locales/zh_CN/competition.json` 与 `en_US/competition.json` 中，`flag_input_placeholder` 同样存在未经转义的花括号字面量（`flag{...}`），导致编译解析器将 `{...}` 判定为非法插值占位符（`Invalid token in placeholder: '...'`）。

---

## 2. 改动点清单

依据 Vue I18n 规范，对所有涉及特殊符号（`@`、`{`、`}`）的国际化字符串使用字面量插值规范（Literal Interpolation：`{'@'}`、`{'{'}`、`{'}'}`）进行安全转义：

1. **`src/volur-lite-view/src/locales/zh_CN/admin/risk.json`**：
   - `policy.email.input_hint`：由 `@ 前缀` 转义为 `{'@'} 前缀`；
   - `policy.email.rule_pattern_text`：由 `@{suffix}` 转义为 `{'@'}{suffix}`；
   - `policy.email.add_success`：由 `@{suffix}` 转义为 `{'@'}{suffix}`。

2. **`src/volur-lite-view/src/locales/en_US/admin/risk.json`**：
   - `policy.email.input_hint`：由 `Leading @ and` 转义为 `Leading {'@'} and`；
   - `policy.email.rule_pattern_text`：由 `@{suffix}` 转义为 `{'@'}{suffix}`；
   - `policy.email.add_success`：由 `@{suffix}` 转义为 `{'@'}{suffix}`。

3. **`src/volur-lite-view/src/locales/zh_CN/competition.json` & `en_US/competition.json`**：
   - `flag_input_placeholder`：由 `flag{...}` 规范转义为 `flag{'{'}...{'}'}`，消除了 CTF 页面 Flag 提示词的潜在解析崩溃隐患。

---

## 3. 验证与审计结果

1. **全量国际化词条编译验证**：
   通过 Node.js 调用 `vue-i18n` 官方编译器对全量中英文（`zh_CN` 与 `en_US`）共计所有 JSON 键名进行严格编译与插值求值校验：
   - 修复前：探测到 8 处保留字语法解析异常；
   - 修复后：**Total errors found: 0**，全量词条 100% 通过语法编译。
2. **前端整体编译构建验证**：
   执行 `npm --prefix src/volur-lite-view run build`，Rollup / Vite 生产构建与 TypeScript 类型检查全部通过（Exit Code 0，无任何类型或编译异常）。
