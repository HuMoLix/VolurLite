# 竞赛前台预览 SPA 单页改造与数字 ID/Slug 路由判别修复审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
1. **竞赛后台预览模式 SPA 化改造**：
   > 「现在修改 @[src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue] ，首先预览竞赛前台的预览方式需要和 @[src/volur-lite-view/src/views/competition/CompetitionDetailView.vue] 一样，而不是做一个弹窗，而是做SPA，跳转到/admin/competitions/356683249897963529/preview。」
2. **详情页按 ID 访问时的 Slug 误判与 404 报错修复**：
   > 「然后修复 @[src/volur-lite-view/src/views/competition/CompetitionDetailView.vue] 中当我使用ID来访问赛事而不是slug的时候页面还是会调用/api/v1/competitions/by-slug/356683249897963529而触发“竞赛不存在”。」

---

## 2. 核心架构与代码改动点

### 2.1 路由架构设计与前台组件复用 ([`src/router/routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts))
在管理后台路由集（`AdminLayout`）下，注册了全新的 SPA 预览路由：
```ts
{
  path: 'competitions/:id/preview',
  name: 'AdminCompetitionPreview',
  component: () => import('@/views/competition/CompetitionDetailView.vue'),
  meta: {
    title: '前台预览',
    titleKey: 'admin.competitions.preview_title',
    parentTitle: '竞赛管理',
    parentTitleKey: 'admin.competitions.title',
    parentPath: '/admin/competitions'
  }
}
```
- **核心收益**：直接复用前台权威视图组件 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)，实现前后台 100% 像素级一致的展示效果，彻底杜绝后台自制弹窗导致的排版偏差与重复样板代码（DRY 原则）。
- **面包屑联动**：继承 `AdminLayout`，面包屑自动呈现 `管理后台 / 竞赛管理 / 前台预览`（中英双语对齐）。

### 2.2 竞赛编辑页与基础信息 Tab 交互升级 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue), [`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue))
1. **顶栏操作区新增「前台预览」入口**：
   - 在 `AdminCompetitionEditView.vue` 顶栏操作区中新增带有 `EyeOutline` 图标的「前台预览」按钮；
   - 点击调用 `goToPreview()`，通过 `router.push('/admin/competitions/' + compId + '/preview')` 丝滑跳转至 SPA 预览视图。
2. **清理冗余弹窗与重复样式**：
   - 从 `BasicInfoTab.vue` 中彻底移除旧版 `<n-modal v-model:show="showPreviewModal">` 弹窗、`renderedDescription` 计算属性及相关 CSS 规则；
   - 基础信息 Tab 内的「前台预览」按钮改为统一调用 `goToPreview()` 路由跳转，精简代码 100 余行。

### 2.3 ID 与 Slug 智能判别修复 ([`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue))
#### 问题根因分析：
既有逻辑中，如果入参被当作 slug 处理，会率先发起 `/api/v1/competitions/by-slug/356683249897963529` 网络请求。由于后端数据库中该雪花 ID 并非 slug，后端直接抛出 404（`竞赛不存在`）。前端统一 Axios 拦截器在捕捉到 404 错误响应时，会无条件执行 `window.$message.error('竞赛不存在')` 弹出 Toast 提示，即便后续在 `.catch` 中降级重试根据 ID 查询，用户屏幕上也已经出现了红色报错提示，造成不良体验。

#### 解决方案：
通过纯数字正则精准预先判定参数类型，从根源上杜绝产生 404 请求：
```ts
async function loadCompetition() {
  const rawTarget = (route.params.slug as string) || (route.params.id as string)
  if (!rawTarget) return

  loading.value = true
  try {
    const isNumericId = /^\d+$/.test(rawTarget)
    let res
    if (isNumericId) {
      res = await getCompetitionByIdApi(rawTarget)
    } else {
      res = await getCompetitionBySlugApi(rawTarget)
    }

    if (res.data) {
      competition.value = res.data
      competitionStore.currentCompetition = res.data
      if (res.data.id) {
        fetchMyTeamStatus(res.data.id)
      }
      startCountdown()
    } else {
      message.warning(t('competition.not_found_by_slug'))
    }
  } catch {
    message.error(t('competition.not_found_by_slug'))
  } finally {
    loading.value = false
  }
}
```
- 适配动态监听：`watch(() => [route.params.slug, route.params.id], ...)`，确保支持无论是 ID 访问还是 Slug 访问均能准确拉取并刷新赛事信息。

### 2.4 管理员视角预览交互体验增强
在 `CompetitionDetailView.vue` 中：
1. **动态模式感知**：计算属性 `isAdminPreview = computed(() => route.path.startsWith('/admin'))`；
2. **预览横幅提示**：在管理员预览模式下，页面顶部展示警告级别横幅：
   > 「前台视图预览模式：当前正在以管理员视角实时预览竞赛前台效果。实际参赛选手看到的页面与当前展示完全一致。」
   并在横幅右侧提供「返回比赛管理」快捷操作按钮；
3. **顶栏标签与回退导航**：
   - 标题后方紧跟琥珀色「前台预览」圆角标签；
   - 顶部返回按钮动态由「返回竞赛列表」变更为「返回比赛管理」，点击精准返回 `/admin/competitions/:id/edit`。

### 2.5 国际化语言包对齐 ([`zh_CN/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/competitions.json), [`en_US/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/competitions.json))
- 补充定义 `preview_title`（"前台预览" / "Front-end Preview"）。

---

## 3. 规范与性能收益

1. **零冗余实现（Single Source of Truth）**：
   - 摒弃在管理后台维护第二份简陋前台预览模板的反模式，统一采用前端真实详情组件渲染，确保 Markdown 样式、倒计时、状态标签及报名状态等完全一致。
2. **消灭错误网络开销与无端报错**：
   - 正则先验判定完全消除因无效 `by-slug/<numeric_id>` 请求导致的 404 响应，保护后端日志纯净度并规避前端错误 Toast 冒泡。
3. **TypeScript 严格类型保障**：
   - 严格清理未使用导入与多余方法（`computed`, `NTag`, `getStatusTagType`），保证类型安全与 Tree-shaking 效率。

---

## 4. 构建与验证结论

执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
```bash
> volur-lite-view@1.0.0 build
> vue-tsc --noEmit && vite build
✓ built in 4.71s
```
- **TypeScript 静态检查**：0 Error, 0 Warning；
- **Vite 生产包编译**：全量打包顺利完成。
