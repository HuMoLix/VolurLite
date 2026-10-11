# 移除 VNotice 底部图层上边框 (border-top) 审计记录

## 1. 变更背景与需求
根据用户 UI 样式微调指令，需要彻底删除 `src/components/sidebar/VNotice.vue` 中 `.card-bottom-layer` 的顶部分割线：
- 原规则为 `border-top: 1px solid #d7d8da80;`（在浏览器计算样式中表现为 `rgba(215, 216, 218, 0.5019607843)`）；
- 此前已按要求移除了暗黑模式下的覆盖色 `border-top-color: #3c4043;`，本次变更进一步移除基准规则中的 `border-top` 声明，使卡片底部彻底无边框分割。

---

## 2. 核心改动点

### 2.1 样式清理 (`src/components/sidebar/VNotice.vue`)
- 在 `<style scoped lang="scss">` 中的 `.card-bottom-layer` 类中，彻底移除：
  ```scss
  border-top: 1px solid #d7d8da80;
  ```
- 保持其 Flex 排版、内边距与文本颜色样式不变。

---

## 3. 验证与影响范围

1. **构建与语法验证**：
   - 执行 `vue-tsc --noEmit && vite build`，在 5.62 秒内成功编译打包，无任何诊断错误。
2. **规范遵守**：
   - 严格遵循零注释代码铁律。
3. **视觉效果**：
   - `VNotice` 卡片底部与内容区域之间不再存在任何边框线条，与已去除边框的 `VCard` 极简风格保持一致。
