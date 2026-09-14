# 前台竞赛详情 Banner 比例优化与后台横幅直接上传重构审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「执行，并且删除 @src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue 中的裁剪 Banner，直接上传就好了，并且删掉上传之后在Banner下面的“/api/v1/uploads/competitions/e10a611ab017408b82d1eef5e9e9886b.png”路径字样。」

本次任务包含两个核心优化：
1. 在前台竞赛详情视图 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 中落地此前分析推荐的黄金画幅比例 `21 : 9` 与高度响应式弹性控制，彻底杜绝大屏切头去尾与首屏遮挡；
2. 在管理后台竞赛编辑页面 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中移除多余的弹窗裁剪步骤，支持点击选择与文件拖拽直接秒传，并彻底移除原横幅下方暴露的冗余图片内部存储路径输入框。

---

## 2. 核心改动点

### 2.1 前台详情页 Banner 自适应比例落地 (`CompetitionDetailView.vue`)
- **容器比例声明**：引入现代 CSS `aspect-ratio: 21 / 9;`，设置 `max-height: 360px;`，保证在 1080P/2K 宽屏下海报既有沉浸感，又确保下方的报名与进入竞技场核心控制卡片处于第一屏（Above-the-Fold）；
- **主体内容居中保留**：`img` 设置 `object-fit: cover; object-position: center;`，确保原图核心视觉不偏移；
- **移动端自适应**：添加媒体查询 `@media (max-width: 768px)`，将手机端比例微调为 `16 / 9`（`max-height: 220px`），保持竖屏浏览的饱满视觉张力。

### 2.2 管理后台横幅直接上传与多余路径移除 (`AdminCompetitionEditView.vue`)
1. **彻底移除裁剪弹窗流程**：
   - 移除了外部裁剪组件依赖，无需二次弹窗裁切；
   - 引入原生隐藏文件输入控件（`bannerFileInputRef`），支持选择图片即时触发；
   - 未上传横幅时：点击虚线卡片或直接将图片拖拽到区域内（`dragover.prevent / drop.prevent`），即刻调用 `uploadImageApi(formData, 'competitions')` 完成上传，并带有 `n-spin` 加载遮罩；
   - 已上传横幅时：悬浮浮现「更换横幅」与「移除横幅」快捷操作，点击「更换横幅」直接调起文件选择器替换；
   - 上传成功后将本地 Blob 缓存至 `setMemoryImageCache`，无缝即时呈现图片预览。
2. **彻底移除横幅下方的存储路径文本框**：
   - 删除了原有的 `.banner-url-input-row` 及其内部的 `<n-input>`，彻底去除了上传后在横幅下方暴露的内部存储路径字符串（如 `/api/v1/uploads/competitions/...`），保持界面整洁专业。

---

## 3. 构建与验证结果

1. **前端工程编译与类型推断**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态检查 **100% 通过（0 错误，0 告警）**；
     - `AdminCompetitionEditView` 打包体积由 19.84 kB 进一步瘦身至 **15.24 kB** (gzip: 5.59 kB)；
     - 4.35s 构建成功。
2. **后端工程验证**：
   - 执行 `mvn test-compile -pl volur-competition`：`BUILD SUCCESS`。

---

## 4. 影响范围与审计结论

- **影响文件**：
  - `src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue`
- **审计结论**：前台海报展示比例已达到最佳视觉与交互平衡；管理后台横幅上传流程简化为一步直达，冗余路径文本已彻底清除。
