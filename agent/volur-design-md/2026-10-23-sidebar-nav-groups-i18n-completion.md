# SideBar 默认导航分组多语言 (i18n) 全面补齐与健壮性改造方案

## 1. 变更背景

在 `src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue` 中，系统定义了默认导航分组 `defaultNavGroups: SidebarNavGroup[]`，涵盖三大业务模块（`explore` 探索、`learn` 学习、`more` 更多）以及赛事大厅、题目训练、公告中心、学习中心、资源中心、实验性功能、帮助中心共 7 个路由节点。

然而，在多语言（i18n）本地化字典与侧边栏渲染链路中存在以下缺陷：
1. **多语言词条缺失**：`src/volur-lite-view-NEXT/src/locales/` 下的 `zh-CN/common.json`、`zh-TW/common.json`、`en-US/common.json` 缺少 `common.nav_group_more`、`common.resources`、`common.experimental`、`common.help_center` 词条，导致切换语言或渲染新分组时出现未命中或直接回显 key 字符串的问题；
2. **公告词条语义不统一**：`defaultNavGroups` 中的公告导航项声明为 `label: '公告中心'`，但字典中历史遗留的 `common.announcements` 译名为 `"全站公告"`，未能与平台的大厅/中心化语义（赛事大厅、学习中心、资源中心、帮助中心）保持一致；
3. **标签解析兜底策略脆弱**：`SideBar.vue` 与 `VSideBar.vue` 中的 `transformItemsToOptions` / `transformNavItemsToMenuOptions` 及 `resolveGroupTitle` 之前无条件调用 `t(item.titleKey)`，在字典加载时序延迟或词条缺失场景下缺乏 `te(...)` 存在性校验，无法安全降级至默认中文字面量；
4. **底层组件回退数据不一致**：`VSideBar.vue` 内部降级声明的 `defaultNavGroups` 仅有 2 个分组且缺少公告中心与“更多”分组，导致在脱离 `SideBar.vue` 单独使用时展现不一致。

---

## 2. 改动点

### 2.1 多语言字典完整补齐 (`common.json`)
针对平台支持的全部 3 种语言（`zh-CN`、`zh-TW`、`en-US`），补充并规范了所有导航分组与菜单项字典：

| Key | zh-CN (简体中文) | zh-TW (繁体中文) | en-US (英文) |
|---|---|---|---|
| `common.nav_group_explore` | 探索 | 探索 | EXPLORE |
| `common.competitions` | 赛事大厅 | 競賽大廳 | Competitions |
| `common.challenges` | 题目训练 | 題目訓練 | Challenges |
| `common.announcements` | 公告中心 | 公告中心 | Announcements |
| `common.nav_group_learn` | 学习 | 學習 | LEARN |
| `common.learning` | 学习中心 | 學習中心 | Learning |
| `common.nav_group_more` | 更多 | 更多 | MORE |
| `common.resources` | 资源中心 | 資源中心 | Resources |
| `common.experimental` | 实验性功能 | 實驗性功能 | Experimental |
| `common.help_center` | 帮助中心 | 幫助中心 | Help Center |

### 2.2 `SideBar.vue` 导航组规范与健壮性改造
1. **字符串字面量规范化**：将 `defaultNavGroups` 中 `experimental` 与 `help` 的键名由双引号统一重构为单引号；
2. **`te` 存在性检查与多重降级机制**：
   引入 `const { t, te } = useI18n()`，在 `transformItemsToOptions` 中：
   - 优先判断 `item.titleKey && te(item.titleKey)`，命中后调用 `t(item.titleKey)`；
   - 否则优先回退至 `item.label`（若为函数则执行，若为字符串则直接返回）；
   - 最终兜底调用 `t(item.titleKey)` 或空字符串。

### 2.3 `VSideBar.vue` 底层同步与鲁棒性加固
1. 引入 `@vicons/ionicons5`（`MegaphoneOutline`, `CubeOutline`）、`@vicons/material`（`BubbleChartOutlined`）与 `@vicons/antd`（`BulbOutlined`）；
2. 同步 `VSideBar.vue` 的 `defaultNavGroups` 降级结构，保持与 `SideBar.vue` 100% 一致的分组与项目；
3. 为 `resolveGroupTitle` 与 `transformNavItemsToMenuOptions` 引入 `te` 动态校验防崩机制。

---

## 3. 安全隐患与缺陷修复说明

1. **规避 UI 键名裸露与翻译闪烁**：
   通过在字典中全面注入所有导航分组及菜单键名，并配合 `te` 检查机制，彻底消除了网络延迟或异步语言包加载时用户界面出现类似 `"common.nav_group_more"` 裸 key 的不良交互体验；
2. **杜绝隐式 Mock 与伪造兜底**：
   所有文案由官方标准字典提供，不采用任何在前端代码内写死的不可翻译临时兜底。

---

## 4. 影响范围

1. `src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue`
2. `src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue`
3. `src/volur-lite-view-NEXT/src/locales/zh-CN/common.json`
4. `src/volur-lite-view-NEXT/src/locales/zh-TW/common.json`
5. `src/volur-lite-view-NEXT/src/locales/en-US/common.json`

全量构建 `vue-tsc --noEmit && vite build` 校验通过，各语言切换与侧边栏渲染均正常。
