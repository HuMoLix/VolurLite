# VSearch 全局搜索组件国际化 (i18n) 施工记录

## 1. 变更背景与动因
在 `volur-lite-view-NEXT` 工程中，全局搜索弹窗组件 [`VSearch.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSearch.vue) 此前存在部分硬编码文案（如输入框默认占位符 `'搜索..'`），且搜索结果类型徽标（`item.type`）未接入平台的多语言赛制标签体系。此外，由于竞赛语言包 `competition.json` 为按需动态懒加载模块（仅在 `/competitions` 和 `/c/*` 路由下由全局路由守卫自动按需拉取），当用户在其他非竞赛页面（如个人中心、全局设置、通知中心等）打开全局搜索弹窗时，非简体中文环境（`en-US`、`zh-TW`）可能因缺少竞赛领域语言包而降级为中文兜底或键名。

为确保全局搜索组件在全站任意路由及不同语言环境下均具备完整的本地化体验，本次对其进行了系统性的国际化重构与完善。

---

## 2. 核心改动点

### 2.1 语言包补充与规范对齐
在各语言环境的通用字典文件 [`common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/common.json) 中补充搜索专属键名：
- **`zh-CN` (简体中文)**：
  - `"search_placeholder"`: `"搜索..."`
  - `"search_results"`: `"搜索结果"`
- **`en-US` (English)**：
  - `"search_placeholder"`: `"Search..."`
  - `"search_results"`: `"Search Results"`
- **`zh-TW` (繁體中文)**：
  - `"search_placeholder"`: `"搜尋..."`
  - `"search_results"`: `"搜尋結果"`

### 2.2 `VSearch.vue` 国际化完善
1. **输入框占位符响应式计算**：
   - 将原硬编码占位符 `'搜索..'` 重构为基于 `props.placeholder || t('common.search_placeholder', '搜索...')` 的响应式 `computedPlaceholder`，支持外部覆盖与多语言无缝切换。
2. **搜索结果列表标题与计数**：
   - 将列表头文案从 `t('common.search')` 升级为 `t('common.search_results', '搜索结果')`，呈现更加规范的语义。
3. **赛制类型标签多语言化**：
   - 新增 `getTypeLabel(type?: CompetitionType | null)` 方法，将 `CTF`、`THEORY`、`AWD`、`AWD_PLUS`、`ISW` 对齐 `competition.type_*` 国际化字典，使得赛制徽章在英文与繁体中文环境下均可准确翻译。
   - 在样式中为 `.result-type-badge` 补充 `white-space: nowrap; flex-shrink: 0;`，确保多语言徽章不换行或被压缩。
4. **竞赛状态枚举补齐**：
   - 在 `getStatusLabel` 中补充 `PENDING` 未开始状态分支，映射至 `t('competition.status_pending', '未开始')`。
5. **跨路由竞赛语言包按需自动加载保障**：
   - 引入 `ensureCompetitionLocale` 逻辑，在组件挂载（`onMounted`）、弹窗显隐（`watch(isVisible)`）及全局语言变更（`watch(() => appStore.locale)`）时，对非默认语言自动触发 `loadLocaleMessagesAsync(appStore.locale, 'competition')`，确保弹窗内状态与赛制文案在非竞赛路由下亦能实时获取精准译文。

---

## 3. 安全隐患修复与防御性说明
- **零假数据与快速失败一致性**：继续严格遵循平台 Fast-Fail 原则，所有字段未命中状态或赛制时均返回原始值或基础占位符，不进行隐式伪造。
- **防止重复请求**：`loadLocaleMessagesAsync` 内部具备 `loadedPaths` 缓存去重机制，多次触发不会产生冗余网络开销或重复解析。
- **纯粹代码与零注释约束**：组件代码内不含任何行内解释性注释，全部逻辑均记录于本文档。

---

## 4. 影响范围
- [`src/locales/zh-CN/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/common.json)
- [`src/locales/en-US/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/en-US/common.json)
- [`src/locales/zh-TW/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-TW/common.json)
- [`src/components/sidebar/VSearch.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSearch.vue)
- 全局侧边栏搜索弹窗交互与多语言切换响应
