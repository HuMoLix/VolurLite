# 审计与施工记录：登录标识表单细节精细化——对齐按钮尺寸规格、定制创建账号 Hover 色彩体系、重构右侧动作栏布局及文案对齐

## 1. 变更背景与需求目标

根据用户的具体 UI/UX 指令，对 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 视图进行了细节优化：
1. **动作按钮尺寸完全统一**：修改“创建账号”按钮与“下一步”按钮具有完全相同的物理尺寸与盒模型（高度 40px、圆角 9999px 胶囊、最小宽度 104px、对称内边距与 14px Medium 字重）；
2. **“创建账号”定制 Hover 背景色**：悬浮（hover）状态下背景颜色设置为 `#0b75d0`，文字切换为高对比度纯白 `#ffffff`；
3. **按钮布局重构**：改变原先 `justify-between` 分置左右两侧的反模式，重构为右对齐组合布局，使“创建账号”按钮紧贴并位于“下一步”按钮的左侧；
4. **链接色彩与无下划线背景标签**：
   - 将“忘记了邮箱？”与“了解详情”的 `<a>` 链接文字颜色统一设置为 Google Blue 规范色 `#0b57d0`；
   - 将“忘记了邮箱？”链接定制为带有软质背景色（`rgba(11, 87, 208, 0.08)`，hover 时 `0.14`）、无文本下划线（`text-decoration: none !important`）的胶囊标签；
5. **文案与提示更新**：
   - 将“忘记了电子邮件地址？”更新为“忘记了邮箱？”；
   - 将“不是您自己的计算机？请使用无痕浏览窗口进行登录”更新为“您用的不是自己的电脑？请使用访客模式无痕登录。“；
   - 输入框 `placeholder` 与校验提示同步更新为“邮箱或账户名称”。

---

## 2. 核心改动清单

### 2.1 模板与表单节点重构 (`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`)

- **输入框与文案调整**：
  ```html
  <n-input
    v-model:value="identifier"
    size="large"
    placeholder="邮箱或账户名称"
    clearable
    class="rounded-lg text-base"
    :status="errorMsg ? 'error' : undefined"
    autofocus
    @keydown.enter="handleNext"
  />
  ```
- **忘记邮箱标签**：
  ```html
  <a
    href="#"
    class="auth-forgot-link"
    @click.prevent="handleForgotAccount"
  >
    忘记了邮箱？
  </a>
  ```
- **公共设备登录提示**：
  ```html
  <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
    您用的不是自己的电脑？请使用访客模式无痕登录。<a
      href="#"
      class="text-[#0b57d0] dark:text-google-blue-dark hover:underline font-medium"
    >了解详情</a>
  </p>
  ```
- **操作按钮组布局（右侧聚合排列）**：
  ```html
  <div class="flex items-center justify-end gap-3 pt-6 sm:pt-8">
    <button
      type="button"
      class="auth-action-btn btn-create-account"
      @click="handleCreateAccount"
    >
      创建账号
    </button>
    <n-button
      type="primary"
      size="large"
      class="auth-action-btn btn-next-step"
      :loading="loading"
      @click="handleNext"
    >
      下一步
    </n-button>
  </div>
  ```

### 2.2 样式规范与状态响应

1. **统一按钮尺寸规则 (`.auth-action-btn`)**：
   - `height: 40px !important;`
   - `min-width: 104px !important;`
   - `padding: 0 20px !important;`
   - `border-radius: 9999px !important;`
   - `font-size: 14px !important; font-weight: 500 !important;`
2. **“创建账号”按钮色彩逻辑 (`.btn-create-account`)**：
   - 默认状态：透明背景、文字 `#0b57d0`（暗色模式 `#8ab4f8`）；
   - Hover 状态：`background-color: #0b75d0 !important; color: #ffffff !important;`；
   - Active 状态：`background-color: #095ebd !important;`。
3. **“忘记了邮箱？”链接样式 (`.auth-forgot-link`)**：
   - `background-color: rgba(11, 87, 208, 0.08);`（暗色 `rgba(138, 180, 248, 0.12)`）；
   - `color: #0b57d0 !important;`；
   - `text-decoration: none !important;`（包含 `:hover` 伪类均去除下划线，改以背景色渐变为反馈交互）。

---

## 3. 测量数据与验证测试

在 Chrome DevTools 实测渲染结果：

1. **按钮物理盒模型检测**：
   ```json
   {
     "rectCreate": { "x": 876, "y": 491, "width": 104, "height": 40 },
     "rectNext": { "x": 992, "y": 491, "width": 104, "height": 40 }
   }
   ```
   - 两按钮宽高均为 `104px × 40px`，尺寸 100% 严丝合缝；
   - 两按钮垂直坐标均为 `y = 491px`；
   - “创建账号”位于左侧，与“下一步”间距保持精准 12px (`gap-3`)。
2. **“忘记了邮箱？”计算样式检测**：
   - 文字颜色：`rgb(11, 87, 208)` (`#0b57d0`)；
   - 背景颜色：`rgba(11, 87, 208, 0.08)`（hover 时为 `rgba(11, 87, 208, 0.14)`）；
   - 下划线属性：`text-decoration: none`。
3. **“了解详情”计算样式检测**：
   - 文字颜色：`rgb(11, 87, 208)` (`#0b57d0`)。
4. **编译构建**：
   - 执行 `npm run build`，耗时 5.12s 成功构建，无任何编译及类型错误。
