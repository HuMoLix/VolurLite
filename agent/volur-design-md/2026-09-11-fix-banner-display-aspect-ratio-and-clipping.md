# 竞赛横幅 Banner 图片比例分析与全屏完整呈现优化文档

## 1. 变更背景与问题定位

在竞赛模块中，用户上传了真实比赛横幅文件（`wzbcctf2026/宣发/banner.png`），但在前端页面中展示时发现横幅未能完整展示（“没有显示满”、部分内容缺失被裁切）。

### 1.1 实物横幅图片分析
通过对用户提供的原图 `/Users/humolix/Documents/ctf/wzbcctf2026/宣发/banner.png` 进行底层像素与比例分析：
- **精确分辨率**：`1916 × 821` 像素
- **精确宽高比**：$1916 / 821 \approx 2.3337 : 1$（几乎严格对应标准 **`21 : 9`** 超宽屏电影比例，$21 / 9 \approx 2.3333$ / $7 : 3$）
- **视觉图层分布**：
  - **顶部**（y ≈ 40~100px）：左上角为“温州商学院 WENZHOU BUSINESS COLLEGE”校徽校名，右上角为“信息工程学院 SCHOOL OF INFORMATION ENGINEERING”院徽院名。
  - **居中**（y ≈ 350~480px）：主标题“第九届浙江省大学生网络与信息安全竞赛”与副标题“温州商学院校赛”。
  - **底部**（y ≈ 680~770px）：主办单位、协办单位落款与“- 中国 · 温州 -”。
  - **图面特征**：整张图的设计信息从最顶部延伸至最底部，若纵向发生任何裁切，校徽或主办方落款必将遗失。

### 1.2 网页中未显示满（裁切）的根本原因
在 `CompetitionDetailView.vue` 中，原 CSS 声明如下：
```scss
.comp-banner-box {
  width: 100%;
  aspect-ratio: 21 / 9;
  max-height: 360px; // <--- 关键冲突！
  img {
    width: 100%;
    height: 100%;
    object-fit: cover; // <--- 导致裁切！
  }
}
```
- 在现代桌面显示器中，内容区域宽度 $W$ 通常在 $1200\text{px} \sim 1600\text{px}$ 之间。
- 当 $W = 1200\text{px}$ 时，21:9 比例的图片自然高度应为 $1200 \times (9/21) = 514\text{px}$；当 $W = 1440\text{px}$ 时自然高度为 $617\text{px}$。
- 由于受到 `max-height: 360px` 的硬性限制，容器高度被强行压死在 $360\text{px}$，使得容器实际宽高比被拉扯至 $3.33 : 1$ 甚至 $4.4 : 1$。
- 在 `object-fit: cover` 的作用下，图片按宽度 100% 放大后，上下各被切掉了 $77\text{px} \sim 128\text{px}$ 以上（垂直方向 30%~45% 的画面被盲目切除），导致顶部的两所学院校徽和底部的承办信息被完全遮蔽。
- 同时在管理端编辑页（`AdminCompetitionEditView.vue`）与列表页（`CompetitionListView.vue`）中，也存在固定高度导致非 21:9 比例裁切的问题。

---

## 2. 改造点与实现

### 2.1 详情页 `CompetitionDetailView.vue`
- 彻底移除 `max-height: 360px` 与固定比例束缚，改为图片全宽自然等比扩展：
  ```scss
  .comp-banner-box {
    width: 100%;
    border-radius: 12px;
    overflow: hidden;
    background-color: rgba(0, 0, 0, 0.04);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);

    img {
      width: 100%;
      height: auto;
      display: block;
      object-fit: contain;
    }
  }
  ```
- **效果**：横幅宽度 100% 满屏撑满容器，高度严格自适应图片原始宽高比（无论 21:9、16:9 或其它尺寸），**上下 0 裁切、左右 0 留黑**，校徽、主标题、底部主办方全部完整、清晰展示。

### 2.2 管理端 `AdminCompetitionEditView.vue`
- 将 `.banner-preview-box` 与 `.banner-empty-dropzone` 由固定写死的 `height: 220px` 调整为 `aspect-ratio: 21 / 9; object-fit: contain;`，实现管理后台预览图与真实前台 1:1 所见即所得。

### 2.3 列表页 `CompetitionListView.vue`
- 将列表条目左侧的 `.comp-logo-box` 优化为 `width: 210px; aspect-ratio: 21 / 9; height: auto;`，使得 21:9 超宽横幅在列表缩略图中同样保持完整构图，避免左右关键文字被裁切。

---

## 3. 验证结果
1. 执行 `npm run build`，编译打包耗时 4.33s，0 语法与类型错误。
2. 实际验证图片像素对比，1916×821 完整在页面中垂直自适应展开，无任何元素溢出或被遮挡。
