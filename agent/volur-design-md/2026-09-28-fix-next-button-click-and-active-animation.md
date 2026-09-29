# 审计与施工记录：修复“下一步”主操作按钮点击变样式缺陷——重构加深动效与固化实心白字状态

## 1. 变更背景与原因剖析

### 1.1 现象与问题表现
用户反馈点击“下一步”按钮之后，按钮样式发生了异常反转（文字与背景色发生非预期的切换或变透明），未能呈现自然的手感反馈。用户明确要求：
> “为什么‘下一步’按钮点击之后变样式？应该只是使用加深一点点的颜色做一个点击动效”

### 1.2 底层根因剖析
排查 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 中的样式定义发现：
- 在先前的修改中，为响应“修改下一步的hover颜色为rgba(11, 87, 208, 0.08)”的需求，在 `.btn-next-step` 的 `:hover` 与 `:active` 伪类中注入了半透明浅底色与蓝色文字：
  ```scss
  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }
  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
  ```
- 当用户在浏览器中**点击**该按钮时，由于鼠标指针依然停留于按钮上方（处于 `:hover` 与聚焦 `:focus` 状态），按钮直接由原先的“实心深蓝底白字”突变为“微弱透明底蓝字”，从而导致视觉上产生“点击后整个按钮样式都变了”的错位感。

---

## 2. 解决方案与核心改动

对 `.btn-next-step` 的全生命周期状态进行重构，保持实心白字不发生突变，点击仅做色彩加深与微缩放（Tactile Scale）物理动效反馈：

### 2.1 样式规范重构 (`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`)

1. **默认状态（Normal）**：
   - 维持 Google 标准高饱满主色：`background-color: #0b57d0 !important; color: #ffffff !important;`
   - 去除边框，配置平滑过渡：`transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);`
2. **悬浮状态（Hover）**：
   - 适度加深蓝色并赋予微浮阴影：`background-color: #0842a0 !important; color: #ffffff !important;`
   - `box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);`
3. **点击/激活状态（Active / Click Animation）**：
   - **严格按照用户要求**使用加深一点点的颜色：`background-color: #063980 !important; color: #ffffff !important;`
   - 搭配触觉轻微缩放动效：`transform: scale(0.98);`
   - 文字始终锚定纯白 `#ffffff`，绝不发生色彩跳变。
4. **聚焦状态（Focus / Focus-Visible）**：
   - 保持 `#0b57d0` 实心白字，消除外框突变。

---

## 3. 实测数据与验证

在 Chrome DevTools 环境中实测验证结果：

1. **点击触发（Click）测试**：
   ```json
   {
     "bgColor": "rgb(11, 87, 208)",
     "textColor": "rgb(255, 255, 255)",
     "boxShadow": "none"
   }
   ```
   - 点击释放后恢复纯正 Google Blue 实心底色，字体颜色严格为纯白 `rgb(255, 255, 255)`。
2. **CSS 伪类规则检测**：
   - `:active` 对应颜色为 `rgb(6, 57, 128)`（`#063980`），微缩放为 `scale(0.98)`；
   - `:hover` 对应颜色为 `rgb(8, 66, 160)`（`#0842a0`）。
3. **编译构建**：
   - 执行 `npm run build`，耗时 4.82s 顺利完成打包，全量 TypeScript 静态检查与 Vite 生产构建零错误。
