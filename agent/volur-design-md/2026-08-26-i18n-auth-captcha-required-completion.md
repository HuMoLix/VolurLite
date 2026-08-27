# 国际化语言包补全 auth.captcha_required

## 1. 变更背景
在登录表单中，当开启验证码时，表单校验规则引用了 `t('auth.captcha_required')`。经检查发现 [`zh_CN/auth.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json) 中缺失该词条，需同步补全以保证中英双语对齐。

---

## 2. 改动清单
- **[`zh_CN/auth.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json)**：
  添加 `"captcha_required": "验证码不能为空"`。
- **[`en_US/auth.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json)**：
  确认包含 `"captcha_required": "Captcha is required"`。

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 通过。
