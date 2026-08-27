# 国际化与 Naive UI i18n 规范配置施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/i18n.ts`、`src/volur-lite-view/src/locales/`、`src/volur-lite-view/src/App.vue`、`src/volur-lite-view/src/layout/AuthLayout.vue`、`agent/volur-design-skills/view-Rules.md`  
**参考规范**：[Naive UI 国际化 (i18n) 官方标准](https://www.naiveui.com/zh-CN/os-theme/docs/i18n)  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与需求目标

根据 Naive UI 官方国际化指南及多语言全栈规范要求，在 Volur Lite 前端工程中建立强类型、响应式的多语言国际化系统：
1. **Naive UI 组件级语言桥接**：
   - 根组件 `NConfigProvider` 动态绑定 `:locale`（如 `zhCN` / `enUS`）与 `:date-locale`（如 `dateZhCN` / `dateEnUS`）；
   - 保证应用业务多语言与 Naive UI 内部组件（分页器、日历组件、确认模态框等）保持 100% 响应式同步。
2. **`vue-i18n` 现代化架构集成**：
   - 基于 Composition API 与 `legacy: false` 模式初始化；
   - 支持通过 Vite 原生 `import.meta.glob('./locales/**/*.json', { eager: true })` 自动化按模块加载各语言包。
3. **规范文档固化**：
   - 在 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 中新增 **第 9 节「国际化与多语言集成规范」**。

---

## 2. 核心改动点与实施细节

### 2.1 依赖安装与 i18n 实例构建
- 安装 `vue-i18n` 依赖；
- 编写 [`src/volur-lite-view/src/i18n.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/i18n.ts)，实现命名空间动态加载与本地语言持久化（`localStorage.getItem('locale')`）。

### 2.2 语言包目录结构建立
在 `src/volur-lite-view/src/locales/` 下建立了结构化双语词典：
- `locales/zh_CN/auth.json` & `locales/en_US/auth.json`
- `locales/zh_CN/common.json` & `locales/en_US/common.json`

### 2.3 `App.vue` Naive UI 响应式语言桥接
```typescript
import { computed } from 'vue'
import { zhCN, dateZhCN, enUS, dateEnUS } from 'naive-ui'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

// 动态映射 Naive UI 的组件语言与日期语言
const naiveLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? enUS : zhCN))
const naiveDateLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? dateEnUS : dateZhCN))
```

### 2.4 UI 交互与组件多语言适配
- **[`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue)**：增加顶部右侧一键双语切换按钮（`EN` / `中`），切换时实时更新 `locale.value` 与 `localStorage`；
- **[`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：全量接入 `useI18n()` 的 `t(...)` 函数，实现标题、表单占位符、必填校验信息与反馈提示的双语自适应。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~2.70s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/package.json`
  - `src/volur-lite-view/src/main.ts`
  - `src/volur-lite-view/src/i18n.ts`
  - `src/volur-lite-view/src/locales/`
  - `src/volur-lite-view/src/App.vue`
  - `src/volur-lite-view/src/layout/AuthLayout.vue`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `agent/volur-design-skills/view-Rules.md`
- **合规性**：
  - 严格遵循 Naive UI 官方 i18n 指南与 Volur Lite 极客极简设计理念。
