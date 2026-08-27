# 修复系统设置邮件配置 (SMTP) Tab 切换崩溃报错（Vue I18n Linked Format 语法错误）问题

## 1. 问题现象与根因定位
### 1.1 问题现象
在管理后台系统设置页面 ([`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)) 中，当点击切换至 **“邮件服务配置 (SMTP)”** Tab 时，界面渲染崩溃无响应，控制台输出报错：
```
[Vue warn]: Unhandled error during execution of render function
  at <FormItem label="发信人邮箱账号" path="smtpUser" >
...
vue-i18n.js:3380 Uncaught (in promise) SyntaxError: Message compilation error: Invalid linked format
1  |  如：service@volur.local
```

### 1.2 根因分析 (Root Cause)
在 `vue-i18n` 消息编译器规范中，`@` 为**链接消息 (Linked Message)** 的保留关键字（语法格式为 `@:key` 或 `@.modifier:key`）。
在 `zh_CN/admin/system.json` 与 `en_US/admin/system.json` 中配置的占位符：
- `"user_placeholder": "如：service@volur.local"`
- `"user_placeholder": "e.g. service@volur.local"`

未对 `@` 字符进行转义。`vue-i18n` 在解析时将 `@volur.local` 误当作未闭合/非法的 Linked Message 语法，导致编译器直接抛出 `SyntaxError`，中断了 Vue 组件渲染函数执行，从而使当前 Tab 与后续点击均失效。

---

## 2. 修复方案与改动细节

根据 Vue I18n 规范，消息中的字面量 `@` 字符必须采用插值转义写法 `{'@'}`：
1. **更新中文语言包** [`zh_CN/admin/system.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/system.json)：
   ```json
   "user_placeholder": "如：service{'@'}volur.local"
   ```
2. **更新英文语言包** [`en_US/admin/system.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/system.json)：
   ```json
   "user_placeholder": "e.g. service{'@'}volur.local"
   ```

---

## 3. 验证结果
- 执行 `npm run build`，编译与类型检查顺利通过；
- 切换至 **“邮件服务配置 (SMTP)”** 及其他任意 Tab 时，表单与所有提示占位符均正常渲染，无任何控制台报错。

---

## 4. 影响范围
- [`src/volur-lite-view/src/locales/zh_CN/admin/system.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/system.json)
- [`src/volur-lite-view/src/locales/en_US/admin/system.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/system.json)
