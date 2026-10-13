# VChallengeSearch 聚焦状态去色与文本/光标/图标对齐微调方案

## 1. 变更背景与目标

根据交互与视觉对齐微调反馈，对赛题搜索组件 `VChallengeSearch.vue` 进行进一步细节优化：
1. **取消聚焦边框颜色修改**：用户要求在 input 获取 focus 状态时，不要改变输入框外壳的边框颜色；
2. **重置搜索图标颜色与交互响应**：`.search-trigger-icon` 默认颜色改为 `#565b61`，当输入框获得 focus 时动态变为 `#1b1b1e`（暗色下对齐为 `#8c8c8c`，focus 时 `#d4d4d4`）；
3. **修复文字与光标垂直中心对齐问题**：此前 input 采用 `height: 100%` 导致光标过长拉伸且文字 baseline 与左侧图标中心线不一致，需要精确校准 input 内部行高与图标的垂直居中轴线。

---

## 2. 核心改动点

### 2.1 样式优化与对齐微调
- 路径：`src/components/competition/ctf/VChallengeSearch.vue`
- **取消 focus 边框变色**：移除原 `:focus-within` 中针对 `border-color` 及 `box-shadow` 的修改，保持常态下轻浅统一边框（亮色 `#dadce0` / 暗色 `#3c4043`）；
- **图标颜色动态响应**：
  - 常态下 `.search-trigger-icon` 颜色设为 `#565b61`（暗色 `#8c8c8c`）；
  - 当 `.search-input-shell:focus-within` 时，触发 `.search-trigger-icon { color: #1b1b1e; }`（暗色下变为 `#d4d4d4`）；
- **文字与光标绝对居中校准**：
  - 将 `.search-native-input` 明确设定为 `height: 22px; line-height: 22px; padding: 0; margin: 0;`；
  - 杜绝了 40px 高度拉伸产生的大长条光标缺陷，光标高度精确约束在 22px 字体视口内；
  - 左侧图标外框设定为 `width: 24px; height: 24px;` 并保证内部 `n-icon` 居中且 `line-height: 1`，使图标中心轴、输入文字中心轴、光标三者在 40px 容器中完美重合于垂直中心线。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：经 `Select-String` 模式扫描验证，文件中没有任何行内或模板注释；
2. **构建验证**：执行 `npm run build` (`vue-tsc --noEmit && vite build`)，打包与类型检查 0 错误通过。
