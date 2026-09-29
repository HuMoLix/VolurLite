# 审计与施工记录：AuthLayout 架构重构——独立置顶 Logo 并实现左侧大标题与右侧表单顶端精准同行基准

## 1. 变更背景与需求目标

根据用户的明确指示，对登录认证卡片 (`auth-card`) 布局进行架构级重构：
1. **Logo 独立解耦置顶**：Logo 不再内嵌于左侧文本列，而是提升为整个卡片内部顶部的独立容器组件 (`auth-card__header`)；
2. **左右区域单 Flex 协同管理**：卡片下半部分由统一的 Flex 容器 (`auth-card__body`) 统领管理，左侧区域包含主标题（"登录"）、副标题（"使用您的 Volur 账号"）及预留扩展芯片，右侧区域承载挂载的动态表单（`router-view`）；
3. **顶端精准同行对齐**：在桌面端 (`min-width: 840px`) 视口下，左侧的大标题与右侧表单的头部（输入框）必须在同一水平行（垂直坐标首线对齐），消除因此前 Logo 挤压左侧导致的左右视觉基准错位。

---

## 2. 核心架构与改动点

### 2.1 DOM 骨架重构 (`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`)

将原先左右平铺（Logo 位于左侧文本上方）的结构拆分为“顶层独立 Logo + 下层统一 Body Flex 布局”：

```html
<div class="auth-card">
  <!-- 1. Logo 单独置顶于卡片内 -->
  <div class="auth-card__header">
    <img
      :src="appStore.theme === 'dark' ? '/r/static/svg/favicon-dark.svg' : '/r/static/svg/favicon.svg'"
      alt="Volur"
      class="auth-card__logo"
    />
  </div>

  <!-- 2. 下方主体由同一个 flex 容器协同管理 -->
  <div class="auth-card__body">
    <!-- 左侧区域：大标题、副标题与扩展芯片 -->
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <!-- 右侧区域：动态表单组件 -->
    <div class="auth-card__right">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" v-if="Component" />
          <slot v-else />
        </transition>
      </router-view>
    </div>
  </div>
</div>
```

### 2.2 响应式 Flex 布局与首行基准对齐规范

1. **桌面端 (`@media (min-width: 840px)`) 协同设计**：
   - `.auth-card__body` 设为 `display: flex; flex-direction: row; align-items: stretch;`；
   - `.auth-card__left` 与 `.auth-card__right` 均从 `y = 0` 处起始；
   - 左侧大标题 `.auth-card__title` 移除外部多余 `margin-top`（设为 `margin: 0 0 12px 0`，行高与字体经过 Google M3 Typography 严格对齐）；
   - 右侧表单首个元素 `n-input` 亦紧贴顶部 `y = 0` 起始，消除因元素默认外边距造成的垂直落差。
2. **手机端自适应**：
   - `.auth-card__body` 降级为 `flex-direction: column;`；
   - 保持垂直流式布局：Logo -> 标题与副标题 -> 间距 -> 输入表单 -> 动作按钮，保持自适应体验。

---

## 3. 测量数据与验证测试

在 Chrome 开发者模式下（视口：`1280x800`），通过 `evaluate_script` 抓取左侧主标题与右侧输入框的真实渲染像素坐标：

```json
{
  "titleRect": { "top": 287, "height": 40, "bottom": 327 },
  "inputRect": { "top": 287, "height": 40, "bottom": 327 },
  "difference": 0
}
```

- **验证结论**：
  - 左侧标题 `top` 为 `287px`，高度为 `40px`；
  - 右侧输入框 `top` 为 `287px`，高度为 `40px`；
  - 左右起始坐标偏差 `difference = 0px`，完全精准实现**左侧大标题与右侧表单的头同行**。

---

## 4. 影响范围与后续指引

- **受影响文件**：
  - `src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`
- **构建状态**：`npm run build` 耗时 4.97s，全量 TypeScript 静态类型检查与 Vite 打包通过，零警告零报错。
- **后续视图拓展建议**：后续增加注册流程（`signup`）、双因子验证（`challenge`）等表单时，表单顶部均无需再包裹多余的顶距，直接渲染表单控件即可天然保证与左侧说明文字精准平齐。
