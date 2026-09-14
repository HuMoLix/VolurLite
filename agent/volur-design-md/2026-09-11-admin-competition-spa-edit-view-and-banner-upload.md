# 管理后台竞赛编辑升级为独立 SPA 路由页面及横幅 Banner 上传组件重构审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「修改 @src/volur-lite-view/src/views/admin/competition/AdminCompetitionView.vue 中的编辑竞赛，也跟 @src/volur-lite-view/src/views/competition/CompetitionDetailView.vue 一样做成SPA，然后参考 @src/volur-lite-view/src/views/profile/ProfileView.vue 中实现上传横幅 Banner。」

在先前的实现中，管理员在竞赛管理中创建或编辑竞赛使用的是 `viewMode` 单组件内嵌页内页方式，不仅导致 `AdminCompetitionView.vue` 体积臃肿（近 1000 行）、职责混杂，且页面状态无法通过 URL 地址独立分享、回退与持久化。同时，横幅封面仅提供纯文本输入框，未提供可视化上传与裁剪功能。

本次任务对后台竞赛编辑模块进行了彻底解耦与 SPA 架构升级，并参考个人资料头像裁剪组件构建了专属的横幅 Banner 可视化裁剪上传组件。

---

## 2. 核心架构改动点

### 2.1 管理后台竞赛编辑解耦为独立 SPA 页面 (`AdminCompetitionEditView.vue`)
1. **视图解耦与独立状态管理**：
   - 新建独立页面组件 [`src/views/admin/competition/AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue)；
   - 支持通过路由参数自动识别编辑态与新建态：
     - `route.params.id` 存在时进入编辑态，通过 `getAdminCompetitionByIdApi(id)` 异步预填表单，赛制模式（一赛一制）在编辑态置灰只读；
     - 访问 `/admin/competitions/create` 时进入创建态，自动初始化默认表单参数；
   - 顶部提供「返回竞赛列表」面包屑式返回按钮、赛制模式徽章以及「保存竞赛」主按钮（黑底白字 `#000000`/`#ffffff`）。

2. **列表视图大幅瘦身 (`AdminCompetitionView.vue`)**：
   - 彻底移除了原组件内的 `viewMode === 'form'` 区域、表单字段模型、日期时间区间状态以及近 30 个表单/布局组件；
   - 点击「创建新竞赛」按钮执行 `router.push('/admin/competitions/create')`；
   - 点击表格行操作栏「编辑」按钮执行 `router.push('/admin/competitions/' + row.id + '/edit')`；
   - 组件打包产物由原本的 18.26 kB 直降为 **5.79 kB**，聚焦于表格查询、过滤筛选、一键封榜/解封与删除功能。

3. **管理后台路由重组 (`src/router/routes.ts`)**：
   - 在 `/admin` 路由树下新增：
     - `/admin/competitions` -> 竞赛列表大厅 (`AdminCompetitionView.vue`)
     - `/admin/competitions/create` -> 创建竞赛 SPA (`AdminCompetitionEditView.vue`)
     - `/admin/competitions/:id/edit` -> 编辑竞赛 SPA (`AdminCompetitionEditView.vue`)
     - `/admin/competitions/edit/:id` -> 重定向至 `/:id/edit`，确保路由兼容性。

---

### 2.2 竞赛横幅 Banner 上传裁剪组件 (`BannerCropperModal.vue`)
参考 [`ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue) 及 `AvatarCropperModal.vue` 的优秀设计，创建了专用于横向海报封面的裁剪组件 [`BannerCropperModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/BannerCropperModal.vue)：
1. **横屏比例适配 (2:1 / 16:9)**：
   - 采用 600px × 300px 画布，配合圆角矩形遮罩层（`crop-banner-overlay`）；
   - 支持多级缩放滑块（`n-slider`）、放大/缩小按钮、滚轮平滑缩放与鼠标拖拽平移；
   - 裁剪时输出 1200px × 600px 高清无损 Canvas Blob。
2. **后端上传接口对接**：
   - 使用 `@/api/system` 中的 `uploadImageApi(formData, 'competitions')`（对应后端 `POST /api/v1/system/upload/image`）；
   - 上传成功后将 Blob URL 存入 `setMemoryImageCache`，避免重复网络加载；
   - 自动回填 `formModel.bannerUrl`。
3. **可视化交互形态**：
   - 未上传横幅时：展示精致的拖拽/点击上传入口虚线框与文件格式/大小提示；
   - 已上传横幅时：展示全宽海报预览，鼠标悬浮呈现半透明遮罩与「更换横幅」、「移除横幅」快捷操作按钮；
   - 同时保留辅助 URL 输入框，支持管理员手动输入外部 CDN 图床地址。

---

### 2.3 前台图片加载与 URL 兼容性优化
1. **URL 解析工具扩展 (`imageCache.ts`)**：
   - 导出了 `formatBannerUrl` 与 `formatImageUrl`，自动将相对后端路径（如 `/api/v1/system/storage/...`）转化为包含后端 Origin 的完整地址或取用本地内存缓存 Blob；
2. **全站横幅统一格式化**：
   - 在 [`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 与 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 中统一将 `:src="comp.bannerUrl"` 升级为 `:src="formatBannerUrl(comp.bannerUrl)"`，杜绝路径拼接异常导致的图片破损。

---

## 3. 规范遵循与设计铁律对照 (GEMINI.md / view-Rules.md)

1. **主操作按钮规范**：
   - 确认上传按钮、保存竞赛按钮、创建竞赛按钮严格遵循 `#000000` 背景与 `#ffffff` 字体，悬浮态为 `#222222`。
2. **描述文本颜色规范**：
   - 页面副标题、表单字段提示、上传说明颜色统一为 `#444444`，严禁纯黑 `#000000`。
3. **严格 TypeScript 强类型**：
   - 杜绝 `any` 类型，完整覆盖请求与响应接口类型推断。
4. **国际化多语言覆盖**：
   - 在 `zh_CN/admin/competitions.json` 与 `en_US/admin/competitions.json` 中完整扩充了横幅上传相关提示词与操作文案。

---

## 4. 构建与验证结果

1. **前端工程编译打包验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态类型推导 **100% 通过（0 错误，0 告警）**；
     - `AdminCompetitionView.vue` 产物体积：**5.79 kB** (gzip: 2.51 kB)；
     - `AdminCompetitionEditView.vue` 产物体积：**19.84 kB** (gzip: 7.25 kB)；
     - 构建用时 4.46s，全部 Chunks 生成正常。
2. **后端工程编译验证**：
   - 执行 `mvn test-compile -pl volur-competition,volur-system`：
     - `BUILD SUCCESS`，各模块通过 0 错误编译。

---

## 5. 影响范围与审计结论

- **影响文件**：
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionView.vue`
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue` (新添)
  - `src/volur-lite-view/src/components/common/BannerCropperModal.vue` (新添)
  - `src/volur-lite-view/src/router/routes.ts`
  - `src/volur-lite-view/src/utils/imageCache.ts`
  - `src/volur-lite-view/src/locales/zh_CN/admin/competitions.json`
  - `src/volur-lite-view/src/locales/en_US/admin/competitions.json`
  - `src/volur-lite-view/src/views/competition/CompetitionListView.vue`
  - `src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`
- **审计结论**：完全达成了将管理后台竞赛编辑改造为独立 SPA 页面、并支持横幅 Banner 可视化拖拽裁剪上传的全部需求，代码结构清晰、视觉规范统一。
