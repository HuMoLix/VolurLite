# 审计与施工记录：将 auth-card 左侧标题与扩展容器解耦并全量交由 router-view 与子组件控制

## 1. 变更背景与需求目标

根据用户的最新架构指令：
此前在认证总布局容器 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 中，认证卡片下半部分被拆解为固定的 `auth-card__left` 与 `auth-card__right`。其中：
- 左侧容器硬编码了 `<h1 class="auth-card__title">{{ cardTitle }}</h1>`、`<p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>` 以及预留插槽 `<div id="auth-header-extra" class="auth-card__header-extra" />`；
- 右侧容器承载 `<router-view>`。

**存在的问题**：
1. **标题依赖外部静态路由元数据**：左侧标题必须通过 `route.meta.cardTitle` 与 `route.meta.cardSubtitle` 被动读取，子视图组件无法根据自身业务状态、子类型路由（如挑战类型的动态变化）自主、灵活地响应式驱动标题与副标题；
2. **Teleport 侵入式黑客写法**：在密码验证页（`AuthenticateView`）和挑战页（`ChallengeView`）中，为了将已识别的账号身份标签芯片放入左侧标题下方，不得不使用 `<Teleport to="#auth-header-extra" v-if="mounted">`，引入了不必要的 DOM 传送与生命周期竞态隐患；
3. **页面过渡动效断层**：当在登录各步骤间切换时（例如从输入账号切换至输入密码），由于只有右栏在 `<router-view>` 的 `fade-slide` 内部，导致右侧表单有平滑渐变位移动画，而左侧的标题与芯片却是生硬突变，破坏了 Google Accounts Glif 体系的全卡片平滑过渡体验。

**本次需求目标**：
将 `auth-card` 左侧容器 `"auth-card__left"` 及其内部的标题、副标题与扩展区域全量交由 `<router-view>` 控制，所有标题信息均由各子业务组件（`IdentifierView`、`AuthenticateView`、`ChallengeView`、`SignUpView`）完全自主决定。

---

## 2. 核心架构重构与改动详情

### 2.1 布局组件容器职责提炼 (`AuthLayout.vue`)
1. **模板结构简化**：
   移除硬编码的 `auth-card__body`、`auth-card__left` 及其标题、扩展插槽，将卡片下半部分主体完全交付给 `<router-view>`：
   ```html
   <div class="auth-card">
     <div class="auth-card__header">
       <img src="/r/static/svg/favicon-simple.svg" alt="Volur" class="auth-card__logo" />
     </div>

     <!-- 下方主体由 router-view 控制：由路由视图组件完全决定左侧标题和右侧表单内容 -->
     <router-view v-slot="{ Component }">
       <transition name="fade-slide" mode="out-in">
         <component :is="Component" v-if="Component" />
         <slot v-else />
       </transition>
     </router-view>
   </div>
   ```
2. **逻辑清理**：
   移除了未使用的 `useRoute` 以及计算属性 `cardTitle`、`cardSubtitle`。
3. **样式穿透支持**：
   在 `<style scoped lang="scss">` 中，通过 SCSS 深度选择器 `:deep(...)`（`&__body, :deep(.auth-card__body)`、`&__left, :deep(.auth-card__left)`、`&__title, :deep(.auth-card__title)`、`&__subtitle, :deep(.auth-card__subtitle)` 等），确保子组件内部渲染的左右分栏网格、媒体查询断点（$\ge 840\text{px}$ 左右分栏 / $< 840\text{px}$ 上下垂直排列）与字体排印规范依然得到统一保障。

### 2.2 视图组件完全自主驱动标题与布局
1. **[`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)**：
   - 根节点直接定义 `<div class="auth-card__body">`；
   - 内部包含 `auth-card__left`（大标题 `登录`、副标题 `使用您的 Volur 账号`、扩展区域）与 `auth-card__right`（邮箱/账号输入表单及操作栏）。
2. **[`AuthenticateView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AuthenticateView.vue)**：
   - 彻底废除 `<Teleport to="#auth-header-extra">` 与 `mounted` 临时挂载开关；
   - 账号身份芯片直接以内联形式编写在左侧 `auth-card__header-extra` 容器中，标题自主声明为 `欢迎`，组件内部状态管理更加纯粹和安全。
3. **[`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue)**：
   - 彻底废除 `<Teleport>`；
   - 标题由组件根据当前的 `challengeType` 响应式自主计算（`cardTitle` 与 `cardSubtitle`）；
   - 账号标签芯片内联渲染在左栏副标题下方。
4. **[`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue)**：
   - 统一收敛为标准 `auth-card__body`，左侧自主承载标题 `创建您的 Volur 账号` 与副标题，右侧承载注册多输入字段表单。

---

## 3. 安全隐患说明与影响范围

1. **安全隐患评估**：
   - 移除了跨 DOM 层级 Teleport 操作，消除了 Vue 客户端渲染时因 DOM 容器尚未挂载或时序竞争引发的潜在 Hydration 失败与空引用问题，安全性与健壮性显著提高；
   - 既有的流转锁票据（TL）验证与账号校验逻辑 100% 保持完好。
2. **影响范围与构建结果**：
   - 执行 `npm run build`：`vue-tsc --noEmit` 静态类型核验与 Vite 打包通过，零错误零告警；
   - 认证页左右视图在路由过渡（如从 identifier 到 authenticate）时由 `<transition name="fade-slide">` 作为一个整体进行平滑微动效切换，视觉体验丝滑连贯。
