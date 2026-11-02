# 废除模糊语言规范化 (normalizeLocale) 并严格推行标准 BCP 47 (en-US / zh-CN / zh-TW) 架构重构

## 1. 变更背景与需求分析
此前在 `volur-lite-view-NEXT` 工程的国际化体系中，存在 `normalizeLocale` 与 `toUrlLang` 函数。其设计初衷是为了宽容解析各类非标或简写的语言代号（如 `zh`、`zh-cn`、`en`、`en-gb`、`zh-hk` 等），并将 URL 查询参数降级为小写（`?lang=zh-cn`、`?lang=en-us`）。

然而，这种宽容策略与模糊映射在专业级工程中暴露出若干问题：
1. **违背强类型快速失败原则**：对未支持或格式非法的语言代码进行隐式兜底与模糊猜测，掩盖了调用方或外部跳转链接传参的错误；
2. **状态与表达不一致（双重标识）**：内部统一采用标准 BCP 47 语言代码（`zh-CN`, `en-US`, `zh-TW`），而 URL 却被强制转为小写（`zh-cn`, `en-us`），导致在路由守卫、URL 参数同步与 Store 之间需要进行频繁的双向转换；
3. **维护冗余与类型弱化**：`normalizeLocale` 接收 `val: unknown` 并返回宽松映射，降低了 TypeScript 静态类型推断与编译期防护能力。

为此，根据指令要求彻底废除 `normalizeLocale` 与 `toUrlLang`，引入严格类型收敛的 `isSupportedLocale` 类型收窄守卫，确保全端严格使用标准的 BCP 47 语言代号（`zh-CN`, `en-US`, `zh-TW`）。

---

## 2. 核心改动点与重构实现

### 2.1 基础设施层 (`src/i18n/index.ts`)
- 导出 `SUPPORTED_LOCALES` 常量元组：`['zh-CN', 'zh-TW', 'en-US'] as const`；
- 将 `SupportedLocale` 派生为 `(typeof SUPPORTED_LOCALES)[number]`；
- 删除 `normalizeLocale` 与 `toUrlLang`；
- 引入强类型类型守卫 `isSupportedLocale(val: unknown): val is SupportedLocale`；
- 重构 `resolveLocale(urlLang?, storeLocale?)`，仅在满足 `isSupportedLocale` 时才采用对应值，否则严格回退至 `DEFAULT_LOCALE`。

### 2.2 路由守卫与 URL 解析 (`src/router/index.ts`)
- 移除 `normalizeLocale`，引入 `isSupportedLocale`；
- 在 `router.beforeEach` 中对 `to.query.lang` 执行严格类型校验，仅当为有效 `SupportedLocale` 时同步更新至 `appStore.setLocale(rawLang)`。

### 2.3 状态管理层 (`src/store/modules/app.ts`)
- 替换导入，使用 `isSupportedLocale`；
- `setLocale` 与 `switchLanguage` 严格基于 `isSupportedLocale(target) ? target : DEFAULT_LOCALE` 进行校验与赋值，去除任何模糊字符转换。

### 2.4 认证布局语言切换器 (`src/components/layouts/auth/AuthLayout.vue`)
- 移除 `normalizeLocale` 与 `toUrlLang` 导入；
- `currentUrlLang` 直接派生为 `computed(() => appStore.locale)`；
- `syncLangFromQuery` 与 `handleLanguageSelect` 统一通过 `isSupportedLocale` 校验合法性；
- URL 路由替换参数 `lang` 直接使用标准的 `en-US`、`zh-CN`、`zh-TW`。

### 2.5 编译环境声明对齐 (`src/env.d.ts`)
- 补齐 `/// <reference types="vite/client" />`，确保 `vue-tsc` 对 `import.meta.glob` 及 Vite 客户端环境的严格类型解析正常。

---

## 3. 影响范围与验证结果
- **涉及文件**：
  - `src/i18n/index.ts`
  - `src/router/index.ts`
  - `src/store/modules/app.ts`
  - `src/components/layouts/auth/AuthLayout.vue`
  - `src/env.d.ts`
- **验证手段**：
  - 执行 `npx vue-tsc --noEmit`，全量类型检查通过（Exit Code 0）；
  - 执行 `npm run build`，Vite 生产构建成功（Exit Code 0）。
