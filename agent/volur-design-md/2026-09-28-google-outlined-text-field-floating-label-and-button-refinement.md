# 施工设计与审计记录：Google Accounts 浮动标签大输入框与按钮样式动效对齐

- **文件编号**：`2026-09-28-google-outlined-text-field-floating-label-and-button-refinement`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/components/common/GoogleTextField.vue` (新增组件)
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue` (重构与组件升级)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue` (组件替换与统一)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue` (组件替换与按钮对齐)

---

## 1. 变更背景与设计动机

根据 Google Accounts (`GlifWebSignIn` / Material 3) 官方页面实测与前端工程规范：
1. **输入框形态演进**：
   - 过往认证页面使用 Naive UI 原生 `n-input`，其静态 placeholder 与标准小圆角框体与 Google 经典的 Outlined Text Field（带上浮边框小标签）形态存在视觉落差。
   - 官方 Google Accounts 输入框内部尺寸规范：输入区为舒适大框体，设定 `margin: 2px 2px 0 2px` 与 `padding: 12px 12px`，搭配 56px 整体视觉舒适高度与 16px 无缩放字号。
   - 交互动效：默认静态 placeholder 居中于输入框；当组件被 focus 或已输入内容时，通过 CSS transform 与定位平滑上浮（Float up）至顶端边框水平线，背景与容器底色融合并切断边框线条，聚焦态文字高亮为 Google Blue（`#0b57d0` / 暗色 `#8ab4f8`）。
2. **按钮样式与动效对齐**：
   - Google Accounts 底部采用两种核心按钮类型：
     - **Text Button (文本按钮，如“改用现有账号登录”、“返回”)**：透明底色，全药丸圆角 (`border-radius: 9999px`)，Hover 时触发半透明 State Layer（`rgba(11, 87, 208, 0.08)` / 暗色 `rgba(138, 180, 248, 0.12)`），Active 按下时加深至 `rgba(11, 87, 208, 0.16)`；
     - **Filled Button (实心主按钮，如“注册”、“下一步”、“登录”)**：实心 Google Blue（`#0b57d0` / 暗色 `#8ab4f8`），全药丸圆角，Hover 时加深并叠加 Material 微阴影 (`box-shadow: 0 1px 2px ...`)，Active 按下时伴随关键物理触感缩放动效（`transform: scale(0.98)`）。
   - `SignUpView.vue` 此前直接采用 Naive UI 普通矩形按钮，缺乏上述交互反馈与 Google 品牌质感。

---

## 2. 核心改动点与实现细节

### 2.1 新增通用组件 `GoogleTextField.vue`
位于 `src/volur-lite-view-NEXT/src/components/common/GoogleTextField.vue`：
1. **尺寸与边框约束**：
   - 原生输入元素 `.google-text-field__input` 严格声明 `margin: 2px 2px 0 2px; padding: 12px 12px;`，行高与高度设定为 28px，结合边距与 1px 边框完美契合 56px 大输入框尺寸；
   - 边框盒 `.google-text-field__box` 采用 `border-radius: 4px` 与 `border: 1px solid var(--v-border-base, #dadce0)`；
   - 聚焦时采用 `border-color: #0b57d0; box-shadow: 0 0 0 1px #0b57d0;`（暗色 `#8ab4f8`），提供视觉 2px 粗焦点框，同时杜绝因边框增粗导致的 DOM 重排与抖动；
2. **浮动标签动画机制 (Floating Label)**：
   - 默认状态：居中定位 `top: 50%; transform: translateY(-50%); left: 14px; font-size: 16px; color: #5f6368;`；
   - 激活状态（聚焦或有内容输入）：平滑过渡至 `top: 0; transform: translateY(-50%) scale(0.75); left: 10px;`，背景色与卡片容器匹配（桌面端 `#ffffff` / 暗色 `#292a2d`；手机端 `#ffffff` / 暗色 `#202124`），`padding: 0 4px;`；
   - 动画贝塞尔缓动：`transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);`；
   - 双重兜底：同时结合 Vue 响应式计算属性 `isFloating` 与原生 CSS 伪类 `:placeholder-shown` / `:focus`，保障浏览器原生 Autofill 状态下标签不发生重叠。
3. **尾部操作扩展**：
   - 支持 `clearable` 一键清空；
   - 支持独立密码显示/隐藏 SVG 按钮切换；
   - 支持插槽 `trailing` 自由扩展；
   - 支持 field-level 错误提示文案与 Google Accounts 风格警告圆标。

### 2.2 升级 `SignUpView.vue`
1. **输入框全面替换**：
   - 用户名、邮箱、密码、确认密码 4 个字段全面换用 `GoogleTextField`；
   - 引入“显示密码”统一复选框，可同时切换密码与确认密码的显隐状态；
   - 接入字段级实时错误拦截提示（非空校验、邮箱正则格式、8位以上密码长度校验、双密码一致性判定）。
2. **按钮样式与动效对齐**：
   - 升级“改用现有账号登录”按钮为 `auth-action-btn btn-signin-instead`（Google M3 文本按钮与 State Layer 悬停交互）；
   - 升级“注册”按钮为 `auth-action-btn btn-register-action`（实心 Google Blue，包含 Hover 加深微阴影与 Active `scale(0.98)` 点击缩放物理反馈）；
   - 暗色模式下无缝适配 `#8ab4f8` 与 `#202124` 高对比度反色。

### 2.3 联动优化 `IdentifierView.vue` 与 `ChallengeView.vue`
- 同步在登录首步（输入账号）和次步（输入密码挑战）引入 `GoogleTextField`，使整个认证生命周期保持 100% 视觉风格统一。

---

## 3. 验证与构建结果

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
- TypeScript 类型推导 100% 通过，SCSS 样式编译无告警；
- 生成制品均产出独立 chunk，运行极速顺畅。
