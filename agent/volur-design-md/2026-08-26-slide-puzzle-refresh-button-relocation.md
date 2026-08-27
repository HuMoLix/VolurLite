# 滑块验证码弹窗重置按钮右下置与文案交互优化

## 1. 变更背景与需求
- 原滑块验证码弹窗顶部右上角包含一个圆形刷新图标按钮，与弹窗关闭按钮容易产生视觉挤压且不够直观；
- 按照用户要求与现代验证码交互设计规范，将刷新操作移至**验证码图片下方右侧**，文案调整为“看不清？换一张试试”，并配以标准 SVG 刷新矢量图标。

---

## 2. 改动清单

### 2.1 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue)
1. **移除顶部 Extra 刷新按钮**：清理 `#header-extra` 中的圆形刷新按钮，保持弹窗头部简洁清爽；
2. **新增图片下方右侧刷新条**：
   ```html
   <div class="puzzle-refresh-bar">
     <n-button
       text
       size="tiny"
       :loading="loading"
       :disabled="isDragging || verifying || isSuccess"
       @click="fetchChallenge"
       class="puzzle-refresh-btn"
     >
       <template #icon>
         <n-icon size="14">
           <RefreshOutline />
         </n-icon>
       </template>
       {{ t('auth.slide_puzzle_refresh_tip') }}
     </n-button>
   </div>
   ```
3. **视觉与样式收敛**：
   - 采用 `#444444` 描述色与 hover 聚焦过渡；
   - 拖动及核验中禁用刷新，避免竞态条件。

### 2.2 国际化语言包补全
- **[`zh_CN/auth.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json)**：
  `"slide_puzzle_refresh_tip": "看不清？换一张试试"`
- **[`en_US/auth.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json)**：
  `"slide_puzzle_refresh_tip": "Can't see clearly? Try another"`

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
