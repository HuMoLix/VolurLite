# VSettingCard 图标与文本对齐修复记录

## 1. 变更背景与问题分析
在侧边栏设置卡片组件 `VSettingCard.vue` 中，菜单项与子菜单项的图标和文本在垂直方向上存在明显的未对齐与视觉重心漂移现象。
修复过程中，若将 `.item-label` 设为 `line-height: 1` 及 `display: inline-flex`，因配合单行文本省略的 `overflow: hidden`，会导致中英文字符字形（Ascender 上升部）因行高紧缩而被容器边界截断（出现“文字上半部分不显示”的现象）。

经深度排查，核心根因如下：
1. **Naive UI NIcon 默认 inline-block 导致图标偏位**：`n-icon` 默认样式为 `display: inline-block; line-height: 1em;`，其内部 `<svg>` 为 inline 元素，受字体基线对齐机制影响向下方偏移，造成未居中。
2. **文字截断根因（overflow: hidden 与紧凑行高冲突）**：`.item-label` 设置了 `overflow: hidden; text-overflow: ellipsis;`。当中文字符字形高度（约 1.2~1.3em）超出 `line-height: 1`（1em）时，超出部分直接被 `overflow: hidden` 裁剪，导致字形上半部分缺失。因此，文本需保留健康行高（`line-height: 1.4`）以提供充足的垂直排版空间。

## 2. 改动点清单
修改文件：`src/components/sidebar/VSettingCard.vue`
1. **`.item-icon` 样式规范**：
   - 补充 `display: flex; align-items: center; justify-content: center; width: 18px; height: 18px;`，强制内部 `<svg>` 转为 Flex 子项并严格双向几何居中，消除基线下沉。
2. **`.item-label` 样式规范**：
   - 保留标准的 `line-height: 1.4`，确保字形 Ascender/Descender 具备完整渲染空间，彻底解决 `overflow: hidden` 裁剪字形上半部分的问题；
   - 依赖父容器 `.item-left` 的 `display: flex; align-items: center;` 自然完成图标与文本的垂直居中轴对齐。
3. **`.submenu-label` 与 `.submenu-check` 同步规范**：
   - `.submenu-label` 保持 `font-size: 13px; line-height: 1.4;`；
   - `.submenu-check` 补充 `display: flex; align-items: center; justify-content: center; width: 16px; height: 16px;`。

## 3. 安全与质量验证
- **代码纯净性**：严格遵守代码零注释铁律，无冗余行内说明；
- **类型安全与工程构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），构建成功且零 TypeScript 错误；
- **渲染验证**：文字完整显示无裁剪，图标与文字保持严格的垂直中心对齐。
