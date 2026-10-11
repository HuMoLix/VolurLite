# VSearch 搜索弹窗组件实现与 VSideBar 侧边栏对接记录

本文档记录了 Volur Lite NEXT 前端工程（`src/volur-lite-view-NEXT`）中通用搜索弹窗组件 `VSearch.vue` 的实现与极简化视觉优化，以及将其深度接入侧边栏 `VSideBar.vue` / `SideBar.vue` 底栏 Dock 的施工过程。

---

## 1. 变更背景与需求目标

在 Volur Lite NEXT 响应式桌面端侧边栏（`VSideBar.vue`）中，底栏 Dock 工具箱配置有快捷搜索入口图标按钮（`<SearchOutline />`）。为实现平台全局竞赛与内容的极简、纯粹且快速的查找体验，研发了独立的 `VSearch.vue` 组件并接入侧边栏：
1. **轻量毛玻璃遮罩**：遮罩层采用 `backdrop-filter: blur(2px) saturate(180%)`，并彻底去除 `background-color`，使页面底色保持原生通透；
2. **极简初始态展示**：弹窗呼出时，只展示居中的搜索输入框，彻底移除初始态分类推荐标签（CTF/AWD/ISW 等）、引导描述以及底部快捷键/品牌页脚；
3. **移除倒计时字样**：隐藏输入框右侧倒计时 3 秒提示字样与徽章，界面保持极度清爽干净；后台 3 秒无操作自动检索机制保持平稳运行；
4. **提升出现垂直位置**：将弹窗整体位置由绝对居中上移至视口上部黄金视觉焦点区（`align-items: flex-start; padding-top: clamp(60px, 14vh, 140px);`），更符合现代命令行/Spotlight 式交互预期；
5. **精准占位符**：输入框占位符统一修改为 `搜索..`；
6. **输入字符 3 秒防抖执行**：在用户停止输入最后一个字符满 3 秒后，自动触发调用后端 API 进行数据检索并动态展开结果流；
7. **多通道即时搜索**：支持在输入框内按 `Enter` 回车键立即搜索、点击搜索图标立即搜索，或静候 3 秒自动执行搜索；
8. **点击外部取消**：用户点击搜索框卡片外部区域时自动退出并取消搜索；支持 `ESC` 键快捷关闭。

---

## 2. 核心架构与功能设计

### 2.1 黄金视觉位置与轻量毛玻璃背景
- 采用 `<Teleport to="body">` 挂载至顶层 DOM；
- 遮罩层配置 `backdrop-filter: blur(2px) saturate(180%); -webkit-backdrop-filter: blur(2px) saturate(180%);`，移除了原本的深色半透明背景色，兼具轻微毛玻璃虚化与背景原生通透度；
- 弹窗卡片在屏幕垂直方向调整为黄金视觉位置（`align-items: flex-start; padding-top: clamp(60px, 14vh, 140px); justify-content: center;`），上移后视线更加自然，并为下方结果列表流留出充足展开空间。

### 2.2 极简布局与动态展开设计
- **初始态**：仅渲染输入头部卡片（`search-header`），高度紧凑（56px），圆角 20px，呈现胶囊药丸状的高辨识度居中搜索条，无多余装饰与倒计时文本；
- **检索与结果态**：当触发检索（`loading`、`hasSearched` 或已有结果）时，通过 `<template v-if="loading || hasSearched || results.length > 0">` 动态渲染分割线与结果列表容器，无缝平滑向下展开。

### 2.3 遮罩点击防误触与事件拦截
- 遮罩外层绑定 `@click="handleBackdropClick"`，判定 `ev.target === ev.currentTarget` 时触发关闭；
- 内部弹窗卡片使用 `@click.stop` 阻断冒泡，确保内部输入与结果点击不会误触发关闭；
- 监听全局 `keydown` 事件（`Escape` 键），在按下时优雅回收定时器并隐藏弹窗；
- 弹窗呼出时，通过 `nextTick` 自动聚焦原生输入框，实现免鼠标即刻输入。

### 2.4 3 秒智能后台防抖与静默流转
- 维护 `debounceTimer`：当捕获 `@input` 事件时，首先清理历史在途定时器；
- 若输入关键词不为空，在后台静默开启 3 秒延时，不打扰用户界面的极简视觉；
- 若用户在 3 秒期间继续键入，重置 3 秒时钟；
- 3 秒届满后，无缝转入 `loading` 检索状态，向后端发起实际网络请求。

### 2.5 即时触发通道（Enter / 图标）
- 用户在输入框中按下 `Enter` 键（`@keydown.enter="handleEnterSearch"`）或点击左侧搜索放大镜图标（`@click="handleSearchIconClick"`）时，立即清除 3 秒延时定时器，跳过等待瞬间触发 `executeSearch()`；
- 支持输入框一键清空（`handleClear`），清空时重置检索结果集并还原初始状态。

### 2.6 真实后端检索与零 Mock 原则
- 默认调用真实竞赛列表接口 `getCompetitionListApi({ keyword, page: 1, size: 8 })`；
- 亦支持由调用方通过 `searchApi` 属性自定义注入检索函数；
- 结果展示包括竞赛赛制标签（CTF / THEORY / AWD / AWD_PLUS / ISW）、竞赛标题、状态胶囊（RUNNING / REGISTRATION / ENDED）与竞赛简述；
- 点击结果项自动完成路由跳转（`/c/${slug}` 或 `/competitions/${id}`）并关闭弹窗，同时向外触发 `select` 事件；
- 严格遵循 Fail-Fast & Zero-Mock 原则，杜绝前端伪造假数据。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `src/components/sidebar/VSearch.vue` | 新建 / 重构 | 极简居中搜索弹窗组件：位置上移至 `clamp(60px, 14vh, 140px)`，删除倒计时字样，blur(2px) saturate(180%) 无背景色遮罩，初始仅显示搜索框，placeholder '搜索..'，动态结果流展开 |
| `src/components/sidebar/types.ts` | 修改 | 新增 `VSearchProps` 接口定义，并在 `VSideBarProps` 中扩充搜索配置属性 |
| `src/components/sidebar/index.ts` | 修改 | 统一导出 `VSearch` 组件 |
| `src/components/sidebar/VSideBar.vue` | 修改 | 在侧边栏模板中内置 `VSearch` 与对应插槽，重构 `handleSearchClick`，暴露 `openSearch` 与 `closeSearch` 方法 |
| `src/components/sidebar/SideBar.vue` | 修改 | 透传 `searchPlaceholder`、`searchApi` 与搜索相关事件，并在组件暴露接口中转发 `openSearch` / `closeSearch` |

---

## 4. 健壮性与内存防护

1. **定时器安全**：在组件卸载（`onBeforeUnmount`）、弹窗关闭（`close`）、手动回车、清空输入以及重新键入时，统一通过 `clearDebounceTimer()` 严格销毁 `setTimeout`，彻底消除内存泄漏隐患；
2. **事件监听清理**：在 `onBeforeUnmount` 中注销 `Escape` 键盘事件监听；
3. **空字符防御**：对首尾空白字符执行 `.trim()`，空串时不发起任何无效后端网络请求；
4. **纯粹代码与零注释**：组件与类型定义代码中未添加任何解释性行内注释，所有设计推导与说明集中记录于本文档中。

---

## 5. 验证与构建结果

在 `src/volur-lite-view-NEXT` 根目录下执行生产构建校验：
```bash
npm run build
```
输出结果：
- TypeScript 严格模式检查通过（`vue-tsc --noEmit` 无错误）；
- Vite 生产构建顺利打包，无任何依赖与语法错误。
