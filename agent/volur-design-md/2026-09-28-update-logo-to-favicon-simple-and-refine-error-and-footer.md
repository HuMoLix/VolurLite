# 审计与施工记录：替换左侧 Logo 为 favicon-simple.svg、设定 48px/4px 规格、输入错误提示色对齐 --n-caret-color-error 并移除页脚点分隔符

## 1. 变更背景与需求目标

根据用户的具体设计指令，对认证主布局（[`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)）与输入表单视图（[`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)）进行以下三项调整：
1. **替换 Logo 资源与尺寸微调**：
   - 将卡片左侧置顶 Logo 文件替换为 `/r/static/svg/favicon-simple.svg`；
   - 将其物理尺寸设定为 `48px × 48px`，并添加 `padding: 4px`；
2. **输入框下方错误提示文字色彩对齐**：
   - 将输入框下方的错误提示字样（如“请输入邮箱或账户名称”）颜色严格设定为 `--n-caret-color-error: #ea4335`（暗色模式自适应为 `#f28b82`）；
3. **移除页脚点分隔符**：
   - 从 `auth-footer` 的元信息区域中彻底删除原先存在的“·”点分隔符。

---

## 2. 核心代码变更清单

### 2.1 认证主布局变更 (`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`)

- **Logo 模板引用更新**：
  ```html
  <div class="auth-card__header">
    <img
      src="/r/static/svg/favicon-simple.svg"
      alt="Volur"
      class="auth-card__logo"
    />
  </div>
  ```
- **删除点分隔符**：
  移除了此前桌面端展示的分隔节点：
  ```html
  <!-- 已彻底删除 -->
  <span class="hidden md:inline-flex items-center text-google-border-light dark:text-google-border-dark select-none px-1 text-[12px] h-8">·</span>
  ```
- **Logo 样式规则**：
  ```scss
  &__logo {
    width: 48px;
    height: 48px;
    padding: 4px;
    box-sizing: border-box;
    object-fit: contain;
  }

  @media (min-width: 840px) {
    &__logo {
      width: 48px;
      height: 48px;
      padding: 4px;
    }
  }
  ```

### 2.2 表单提示色彩变更 (`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`)

- **节点声明**：
  ```html
  <p v-if="errorMsg" class="mt-1.5 text-xs auth-input-error-msg">
    {{ errorMsg }}
  </p>
  ```
- **样式规则**：
  ```scss
  .auth-input-error-msg {
    color: var(--n-caret-color-error, #ea4335) !important;
  }

  :global(.dark) .auth-input-error-msg {
    color: var(--n-caret-color-error, #f28b82) !important;
  }
  ```

---

## 3. 测量数据与验证测试

在 Chrome 开发者模式下通过 CDP 实测验证结果：

1. **Logo 节点属性与盒模型**：
   - 资源路径：`/r/static/svg/favicon-simple.svg`；
   - 宽度与高度：`48px × 48px`；
   - 内边距：`padding: 4px`；
2. **输入框下方错误提示颜色**：
   - 计算属性 `color`：`rgb(234, 67, 53)`（对应 Hex 色值为 `#ea4335`），与 Naive UI 输入框错误状态边界色完全对齐；
3. **页脚内容检测**：
   - `footerMeta.textContent` 实测结果中已无任何“·”符号；
4. **编译与打包**：
   - 执行 `npm run build`，耗时 5.04s 成功打包，TypeScript 静态类型检查零警告零报错。
