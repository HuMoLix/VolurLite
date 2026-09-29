# Volur Lite NEXT 前端开发与架构设计规范 (view-NEXT-rules.md)

本文档定义 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）的核心架构约束、设计规范、主题色彩与 TypeScript / ES2026 编码标准。所有针对 `src/volur-lite-view-NEXT` 的模块新增、组件编写、路由定义、状态管理及样式重构均必须严格遵循本规范。

---

## 1. 技术栈与架构基准 (Tech Stack & Architecture)

Volur Lite NEXT 采用极速轻量、高可用、高扩展的现代前端工程体系：
- **基础框架**：Vue 3 (全面基于 `<script setup lang="ts">` 与 Composition API)；
- **语言标准**：**ECMAScript 2026 (ES2026)**（强制基准，严禁降级）；
- **构建工具**：Vite (极速 HMR，构建目标 `target: 'esnext'`)；
- **开发语言**：TypeScript 5.7+ (严格模式 `strict: true`，杜绝 `any` 泛滥)；
- **原子化样式**：**Tailwind CSS** (无冲突隔离配置 `corePlugins: { preflight: false }`，负责排版、栅格、间距与响应式断点)；
- **组件库**：**Naive UI** (全面接管受控表单、数据表格、模态弹窗与全局消息体系)；
- **样式预处理**：SCSS (负责复杂深层样式穿透、BEM、高级动画与预编译变量)；
- **状态管理**：**Pinia** (统一强制采用 Composition API Setup Store 语法风格)；
- **排版字体**：`vfonts` (Lato, Fira Code, 契合现代排版美学)；
- **网络通信**：Axios (强类型 `ApiResponse<T>` 封装，无感 Token 刷新队列，SSE 实时流订阅)。

---

## 2. Google 风格主题色彩与设计系统规范 (Google Material Theme System)

Volur Lite NEXT 全面弃用过往纯冷黑硬切风格，深度对齐 **Google Material Design 3 (M3) 与 Google Workspace** 的经典现代色彩规范：兼具高可读性、优雅亲和力、分层表面高度 (Elevation) 与沉浸式深色模式。

### 2.1 核心语义色彩系统 (Google Semantic Accents)

| 语义角色 | 亮色模式 (Light Mode) | 暗色模式 (Google Dark Mode) | 场景与说明 |
| :--- | :--- | :--- | :--- |
| **Primary (Google Blue)** | `#1a73e8` (Google Blue 600) | `#8ab4f8` (Google Blue 200) | 核心主色、主要操作按钮、激活菜单项、焦点边框 |
| **Primary Hover** | `#1765cc` | `#aecbfa` | 主色悬停态 |
| **Primary Active** | `#1557b0` | `#669df6` | 主色按下态 |
| **Success (Google Green)** | `#34a853` (Google Green 600) | `#81c995` (Google Green 200) | 挑战通过、评测成功、安全状态徽章 |
| **Warning (Google Yellow)** | `#fbbc04` (Google Yellow 600) | `#fdd663` (Google Yellow 200) | 风险警告、未提交提示、临时通知 |
| **Danger / Error (Google Red)** | `#ea4335` (Google Red 600) | `#f28b82` (Google Red 200) | 危险动作、密码错误、解题失败、封禁 |
| **Info / Accent (Google Cyan)** | `#24c1e0` | `#78d9ec` | 引导标签、状态提示、特殊链接 |

### 2.2 表面与文字阶梯体系 (Surface & Elevation Hierarchy)

#### 亮色模式 (Google Light Workspace)
- **基底背景 (Page Base)**：`#f8f9fa` (Google Gray 50，极其柔和护眼的浅灰底色，非刺眼纯白)；
- **认证/容器表面 (Surface Container / `sys-color-surface-container`)**：`#f0f4f9` (Material 3 Surface Container 浅蓝灰底色，用于登录/注册与认证整体布局)；
- **卡片/容器表面 (Surface Card)**：`#ffffff` (纯白卡片，营造层级对比)；
- **次级卡片/悬浮背景 (Hover Surface)**：`#f1f3f4` (Google Gray 100)；
- **边框与分割线 (Borders & Dividers)**：`#dadce0` (Google Gray 300，清晰细腻的面域划分)；
- **主要文本 (Primary Text)**：`#202124` (Google Gray 900，深炭色高对比正文，非粗暴 `#000000`)；
- **次级描述文本 (Description Text)**：`#3c4043` (Google Gray 700) 与 `#5f6368` (Google Gray 600，表单说明与小字)；
- **弱化文本 (Disabled / Muted)**：`#80868b` (Google Gray 500)。

#### 暗色模式 (Google Dark Workspace)
- **基底背景 (Page Base)**：`#202124` (Google Dark 900，沉浸深灰黑，非压抑死黑)；
- **认证/容器表面 (Surface Container / `sys-color-surface-container-dark`)**：`#1e1f20` (Material 3 Dark Surface Container)；
- **卡片/容器表面 (Surface Card)**：`#292a2d` (Google Dark 800)；
- **次级/浮层卡片 (Elevated Surface)**：`#303134` (Google Dark 700)；
- **边框与分割线 (Borders & Dividers)**：`#3c4043` (Google Dark 600)；
- **主要文本 (Primary Text)**：`#e8eaed` (高对比柔和灰白)；
- **次级描述文本 (Description Text)**：`#bdc1c6` (次级信息清晰易读)；
- **弱化文本 (Disabled / Muted)**：`#9aa0a6`。

### 2.3 按钮与组件视觉约束
1. **主要操作按钮**：
   - 亮色模式：采用 Google Blue `#1a73e8` 底色，搭配纯白文本，`border-radius: 8px` 至 `9999px`（支持 Pill 药丸按钮）；
   - 暗色模式：采用 Google Blue `#8ab4f8` 底色，搭配深色 `#202124` 文本，确保在暗色环境下保持顶级反差与视觉醒目；
2. **文本颜色铁律**：严禁在正文中使用刺眼的全黑 `#000000`；所有描述文本、副标题与表单 label 统一采用 Google Gray 体系（`#3c4043` / `#5f6368`，暗色 `#bdc1c6`）；
3. **全局控制器定位**：中英文多语言切换与夜间/亮色主题切换按钮继续固定在页面视口左下角（`bottom: 20px; left: 20px; z-index: 1000;`）。

---

## 3. 路由架构与版本控制规范 (Router Architecture & On-demand Versioning)

在新一代前端 `volur-lite-view-NEXT` 中，**前端路由采用按需渐进式版本控制（并不强制所有业务路由都具备版本前缀，后续根据具体业务需求与明确要求进行按需扩展与添加即可）**。
- 普通通用业务路由可使用常规语义化路径（例如 `/competitions` 赛事大厅）；
- 涉及阶段化重构或多版本并存的模块可按指令引入版本前缀（例如 Google 风格认证流 `/v1/signin/identifier`、`/v1/signin/challenge` 等）。

### 3.1 路由示例参考对照表

| 业务领域 | 路由路径设计 | 说明与适用场景 |
| :--- | :--- | :--- |
| **登录：输入身份** | `/v1/signin/identifier` | 阶段化认证第一步：输入用户名/邮箱，检测风险与账号状态 |
| **登录：验证挑战** | `/v1/signin/challenge` | 阶段化认证第二步：输入密码、两步验证(2FA)、安全挑战 |
| **注册** | `/v1/signup` | 新用户账号注册引导页 |
| **密码找回** | `/v1/recovery/identifier` | 账号安全找回与重置第一步 |
| **赛事大厅** | `/competitions` | 平台核心公开赛事列表与筛选（无需强制 `/v1/`） |
| **赛事详情/工作台** | `/competitions/:id` | 比赛答题界面、实时榜单、战队状态 |
| **练习靶场** | `/challenges` | 练习赛题与独立容器实例管理 |
| **个人中心** | `/user/profile` | 账号资料、安全密钥、绑定设置 |
| **管理端控制台** | `/admin/dashboard` | 平台运营与监控中枢 |

### 3.2 根路径重定向与权限守卫规范 (Authentication Guard)
1. **根路径重定向**：访问根路径 `/` 时，统一重定向至公共主页（默认 `/competitions` 赛事大厅）；
2. **显式权限控制 (`requiredAuth`)**：
   - 路由守卫**仅根据路由元信息 `meta` 中显式配置的 `requiredAuth: true`（或兼容 `requiresAuth: true`）**来判定是否需要登录拦截；
   - 未显式配置或配置为 `false` 的公开路由（如 `/competitions`）任何人均可直接访问；
   - 命中需要鉴权且当前无有效登录凭证时，自动引导跳转至登录页面（`/v1/signin/identifier` 或指定的登录入口），并携带 `query: { redirect: to.fullPath }` 便于登录后原路返回。

---

## 4. Tailwind CSS 与 Naive UI 协同规范

在新一代工程中，Tailwind CSS 与 Naive UI 各司其职，严禁交叉破坏：

### 4.1 Preflight 禁用与优先级铁律
- **`corePlugins: { preflight: false }`**：在 `tailwind.config.ts` 中保持禁用 Preflight，以避免冲掉 Naive UI 的全局 CSS 重置；
- **`<meta name="naive-ui-style" />`**：在 `index.html` 的 `<head>` 中显式声明 Naive UI 样式挂载锚点；
- **分工边界**：
  - **Tailwind**：负责容器网格、Flex 排版、间距 (`gap-*`, `p-*`, `m-*`)、圆角、响应式断点（`md:*`, `lg:*`）、过渡动画；
  - **Naive UI**：负责复杂表单 (`NForm`, `NInput`, `NSelect`)、模态弹窗 (`NModal`)、表格 (`NDataTable`)、下拉菜单 (`NDropdown`) 及消息系统。

### 4.2 Tailwind 与 Google 主题变量对接
在 `tailwind.config.ts` 中将 Google 主题色彩映射为原子类：
```ts
theme: {
  extend: {
    colors: {
      google: {
        blue: { DEFAULT: '#1a73e8', hover: '#1765cc', dark: '#8ab4f8' },
        red: { DEFAULT: '#ea4335', dark: '#f28b82' },
        green: { DEFAULT: '#34a853', dark: '#81c995' },
        yellow: { DEFAULT: '#fbbc04', dark: '#fdd663' },
        surface: {
          light: '#f8f9fa',
          card: '#ffffff',
          dark: '#202124',
          darkCard: '#292a2d',
          darkElevated: '#303134'
        },
        text: {
          primary: '#202124',
          desc: '#3c4043',
          muted: '#5f6368',
          darkPrimary: '#e8eaed',
          darkDesc: '#bdc1c6',
          darkMuted: '#9aa0a6'
        },
        border: {
          light: '#dadce0',
          dark: '#3c4043'
        }
      }
    }
  }
}
```

### 4.3 静态资源映射与安全防护规范 (`/r/` 虚拟路由与 Path Traversal 防护)
1. **统一静态资源路由前缀**：所有存放在 `public/` 目录下的静态资源（如 Favicon、Logo、备案图标等）在前端页面引用时统一映射为以 `/r/` 为根前缀（如 `/r/static/favicon.svg`、`/r/static/img/beian.png`）；
2. **严禁路径穿越 (Strict Path Traversal Defense)**：
   - 资源拦截器在处理 `/r/*` 请求时，**必须强制执行严密的路径安全校验**；
   - 严禁包含 `..`、URL 编码 (`%2e%2e`)、双重编码 (`%252e%252e`)、空字节 (`\0`) 或反斜杠绕过；
   - 目标文件规范化绝对路径必须严格限制在 `public` 根目录之内，一旦探测到非法尝试立即阻断并响应 `403 Forbidden`。

---

## 5. [反模式 vs 推荐实践] 对照表

### 5.1 路由定义

#### ❌ 反模式 (无版本控制、扁平散乱、缺乏上下文)
```ts
// 错误示范：缺乏版本前缀，随意定义无规则路径
const routes = [
  { path: '/login', component: Login },
  { path: '/user-list', component: UserList },
  { path: '/my-team', component: Team }
]
```

#### ✅ 推荐实践 (遵循 RESTful 与版本分层)
```ts
// 正确示范：版本收敛、语义清晰、层级分明
const routes: RouteRecordRaw[] = [
  {
    path: '/v1',
    children: [
      {
        path: 'signin',
        children: [
          { path: 'identifier', component: SignInIdentifierView, meta: { title: '登录 - 输入账号' } },
          { path: 'challenge', component: SignInChallengeView, meta: { title: '登录 - 安全验证' } }
        ]
      },
      {
        path: 'competitions',
        children: [
          { path: '', component: CompetitionListView, meta: { title: '赛事大厅' } },
          { path: ':id', component: CompetitionWorkspaceView, meta: { title: '比赛工作台' } }
        ]
      }
    ]
  }
]
```

### 5.2 视觉色彩定义

#### ❌ 反模式 (生硬死黑与刺眼无层次)
```scss
// 错误示范：生硬刺眼的全黑背景与纯黑边框
body {
  background-color: #000000;
  color: #ffffff;
}
.card {
  border: 1px solid #000;
}
```

#### ✅ 推荐实践 (Google Material 分层表面色彩)
```scss
// 正确示范：Google Workspace 阶梯灰黑表面体系
:root {
  --v-bg-base: #f8f9fa;
  --v-bg-card: #ffffff;
  --v-text-primary: #202124;
  --v-text-desc: #3c4043;
  --v-color-primary: #1a73e8;
  --v-border-base: #dadce0;
}

html.dark {
  --v-bg-base: #202124;
  --v-bg-card: #292a2d;
  --v-text-primary: #e8eaed;
  --v-text-desc: #bdc1c6;
  --v-color-primary: #8ab4f8;
  --v-border-base: #3c4043;
}
```

### 5.3 异步提交流程中的错误生命周期 (Error Lifecycle in Submissions)

#### ❌ 反模式 (提交流程同步过早清空错误引发抖动与重颤)
```ts
// 错误示范：提交流程伊始同步提前清空错误，引发卡片高度收缩又膨胀的上下弹跳
async function handleVerifyChallenge() {
  if (authFlowStore.loading) return
  errorMsg.value = ''        // 导致错误行瞬间卸载，卡片骤缩 22px
  recaptchaError.value = ''  // 触发 watch 清空，重赋时向沙箱重复下发震颤
  try {
    await authFlowStore.submitChallenge('pwd', { ... })
  } catch (err: any) {
    errorMsg.value = err?.message || '密码错误' // 重新挂载，卡片膨胀 22px
  }
}
```

#### ✅ 推荐实践 (输入与事件驱动的精确错误自愈)
```ts
// 正确示范：提交流程不清空错误，错误由用户输入变更精准驱动自愈
watch(password, () => {
  if (errorMsg.value) errorMsg.value = ''
})

async function handleVerifyChallenge() {
  if (authFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true
  try {
    const res = await authFlowStore.submitChallenge('pwd', { ... })
    // 成功后由路由跳转或业务逻辑流转，无需提前重置
  } catch (err: any) {
    errorMsg.value = err?.message || '密码错误' // 就地平滑更新，无 DOM 剧变
  } finally {
    isSubmitting.value = false
  }
}
```

### 5.4 居中布局过渡解耦与并发防重锁 (Layout Decoupling & Concurrency Guard)

#### ❌ 反模式 (居中容器滥用 transition: all 与裸奔无防重)
```scss
// 错误示范：弹性居中容器相邻元素配置 transition: all，产生 300ms 滞后拉扯
.auth-footer {
  transition: all 0.3s ease; // 卡片高度变化推动 Footer 时引发橡皮筋弹性晃动
}
```

#### ✅ 推荐实践 (局部过渡隔离与互斥锁)
```scss
// 正确示范：移除整体布局属性缓动，错误提示采用轻量透明度淡入淡出
.field-error-fade-enter-active,
.field-error-fade-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.field-error-fade-enter-from,
.field-error-fade-leave-to {
  opacity: 0;
}
```

---

## 6. 交互体验与表单防抖动推荐约束 (Interaction & Anti-Jitter Recommended Constraints)

在认证流（如 `IdentifierView.vue`、`ChallengeView.vue`）及复杂表单交互中，为了彻底杜绝卡片闪烁、垂直抖动与状态撕裂，推荐遵守以下五项铁律：

1. **表单错误生命周期收敛铁律**：
   - **严禁在异步提交流程伊始无条件同步清空错误字段**（严禁在 `handleNext` / `handleVerify` 顶部执行 `errorMsg.value = ''`）；
   - 错误信息的清空必须且只能由**真实的用户修改行为**触发（如 `watch` 监听表单输入变化自愈消除、点击清空按钮 `@clear`、或重新点击验证码复选框启动重试）；
   - 提交失败时直接就地更新或保持错误文本，杜绝“卸载 -> 卡片收缩 -> 重新挂载 -> 卡片膨胀”的视觉跳变。
2. **居中弹性布局的过渡解耦铁律**：
   - 桌面端垂直居中容器（`md:justify-center`）中，子元素高度变动会以中心为基准导致上下位移（$\Delta Y = \frac{\Delta H}{2}$）；
   - 居中布局下容器自身与相邻兄弟元素（如 Footer）**严禁配置 `transition: all`**，防止高度突变与动画缓动产生复合共振拉扯；
   - 动态插入的提示行必须使用轻量局部渐变（如 `field-error-fade` 的 opacity 缓动），平滑过渡。
3. **异步提交流程的互斥防重锁 (`isSubmitting`)**：
   - 表单提交必须配置 `const isSubmitting = ref(false)` 配合 `try ... finally`，对连续快速点击、回车键与鼠标点击在同一事件循环内的并发重复触发实施物理短路拦截。
4. **挑战与动效侦听的实质值比对**：
   - 监听外部传入的错误文本（如 reCAPTCHA `props.errorMsg`）时，必须使用 `(newVal, oldVal)` 并严格判定 `newVal && newVal !== oldVal`，当错误文本未发生实质变更时严禁重复向沙箱下发重置或水平震颤（`.rc-shake`）指令。
5. **全屏加载遮罩与最小反馈驻留时间机制 ($\max(\text{minDuration}, t_{\text{backend}})$)**：
   - 在认证流转（如“下一步”）中，为全屏白色遮罩（80% 透明度）与顶部线性进度条设定固定的前端最小展示时长（建议基准为 **1.5s**）；
   - 当后端响应极快（<100ms）时，通过最小延时消除突闪（Flash of Loading），提升沉浸感；若超过 1.5s 后端仍未返回则保持展示，待后端响应完成且满足最小时间后，优雅平滑退出遮罩并流转至下一步。

---

## 7. 开发审计与溯源要求
所有针对 `src/volur-lite-view-NEXT` 的任何修改，在完成之后必须在 `agent/volur-design-md` 中记录 Markdown 施工文档，详细记录改动点、视觉对齐说明与影响范围。
