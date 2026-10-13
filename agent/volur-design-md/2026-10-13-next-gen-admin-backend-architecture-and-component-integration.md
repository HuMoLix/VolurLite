# Volur Lite NEXT 管理后台架构设计与核心组件集成方案

## 1. 变更背景与设计目标

随着 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）演进，前台竞赛大厅与认证体系已全面落地。为建立完整的平台全生命周期管控中枢，需基于经典前端（`src/volur-lite-view`）中的后台设置及管理规范，结合 NEXT 现代组件库与 Google Material Design 3 色彩规范，在 NEXT 工程中构建统一的管理后台。

### 核心目标
1. **侧边栏体系复用**：管理后台继续使用 `src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue` 作为统一侧边栏导航框架。
2. **Dock 入口动态透出**：在 `VSideBar.vue` 底部 Dock 的设置弹层（`VSettingCard.vue`）中，在“用户设置”下一项增加“平台管理”入口，仅对具备管理员权限（`userStore.isAdmin`）的用户展示，点击直接流转至管理后台。
3. **路由安全防御与权限守卫**：在路由元信息 `RouteMeta` 中引入 `requiredAdmin?: boolean`，在全局导航守卫 `router.beforeEach` 中对 `/admin/` 路径及 `requiredAdmin` 进行强类型校验与快速失败防越权拦截。
4. **标准化通用组件深度集成**：管理后台全面采用 NEXT 封装的标准化通用组件：
   - 按钮：`VBtn.vue`
   - 列表/表格：`VTable.vue`
   - 模态弹窗：`VModal.vue`
   - 文本与多行输入：`VInput.vue`
   - 下拉选项框：`VSelection.vue`
   - 检索搜索框：`VSearch.vue`
5. **纯净代码与文档外置原则**：代码文件杜绝一切解释性注释与冗余备注，所有设计背景与技术细节收敛至本文档。

---

## 2. 侧边栏与 Dock 入口改造 (`VSettingCard.vue` / `VSideBar.vue` / `SideBar.vue`)

### 2.1 入口设计与位置编排
- 在 `VSettingCard.vue` 模板中，在“用户设置”（`user-settings`）项的正下方，通过 `v-if="userStore.isAdmin"` 条件渲染“平台管理”项目。
- 图标采用 Ionicons 5 `SpeedometerOutline`，文字国际化键名为 `common.platform_admin`（中文为“平台管理”）。
- 点击后触发 `handleAdminClick`，依次向上 emit `item-click` 与 `admin-click` 事件，并自动通过 `router.push(props.adminRoute)` 导航至 `/admin/dashboard`，随后关闭设置浮层。

### 2.2 属性透传与事件冒泡链条
1. `types.ts`：在 `VSettingCardProps` 与 `VSideBarProps` 中增加可选属性 `adminRoute?: string`（默认值为 `'/admin/dashboard'`）。
2. `VSideBar.vue`：接收 `adminRoute` 属性并透传至普通状态与折叠状态下的 `VSettingCard` 实例，向上冒泡 `admin-click` 事件。
3. `SideBar.vue`：作为最外层响应式容器，声明 `adminRoute` 默认属性并向内透传至 `VSideBar`，完整维持跨组件属性通信一致性。

---

## 3. 路由架构与管理员权限守卫 (`requiredAdmin`)

### 3.1 路由元信息类型扩展
在 `src/volur-lite-view-NEXT/src/types/router.d.ts` 的 `RouteMeta` 接口中，扩展增加 `requiredAdmin?: boolean`。

### 3.2 路由树配置 (`routes/admin.ts`)
管理后台路由收敛至 `/admin` 路径，采用嵌套路由模式：
- **布局容器**：`AdminLayout.vue`（作为 `/admin` 根级组件），重定向默认至 `/admin/dashboard`。
- **子路由节点**：
  - `/admin/dashboard`：`AdminDashboardView.vue`（管理员控制台）
  - `/admin/users`：`AdminUserView.vue`（用户管理列表与模态操作）
  - `/admin/system`：`AdminSystemView.vue`（后台设置，包括基础配置、发信 SMTP 与维护模式）
- 所有管理路由的 `meta` 均显式声明 `requiredAuth: true`、`requiredAdmin: true` 与 `roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']`。

### 3.3 全局路由守卫校验流 (`router/index.ts`)
```mermaid
flowchart TD
    Start[导航流转 to] --> CheckPath{to.matched 是否包含 requiredAdmin<br/>或 path.startsWith('/admin')}
    CheckPath -- 否 --> PublicPass[普通鉴权检查 / 放行]
    CheckPath -- 是 --> CheckLogin{检测 logged_in Cookie}
    CheckLogin -- 未登录 --> RedirectLogin[重定向至 /v1/signin/identifier<br/>附带 redirect 参数]
    CheckLogin -- 已登录 --> FetchProfile{userStore.user 是否已载入}
    FetchProfile -- 未载入 --> DoFetch[调用 userStore.fetchCurrentUser]
    FetchProfile -- 已载入 --> CheckAdmin{userStore.isAdmin}
    DoFetch --> CheckAdmin
    CheckAdmin -- 非管理员 --> BlockRedirect[拦截并重定向至 /competitions 赛事大厅]
    CheckAdmin -- 是管理员 --> AllowPass[允许访问管理后台]
```

---

## 4. 管理端页面设计与通用组件应用

### 4.1 管理后台主布局 (`AdminLayout.vue`)
- **侧边栏集成**：直接挂载 `SideBar.vue`，自定义菜单选项数组 `adminMenuOptions`（控制台、用户管理、后台设置）。
- **底部动作槽位**：配置 `show-bottom-action="true"`，图标采用 `ArrowBackOutline`，路由配置为 `/competitions`，支持一键平滑返回前台大厅。
- **顶栏状态与面包屑**：采用毛玻璃半透明容器（`bg-white/80 dark:bg-[#202124]/80`），左侧展示 Google Blue 风格的 `管理后台` 胶囊徽章与当前页面标题，右侧挂载返回前台的 `VBtn` 快捷按钮。

### 4.2 控制台总览 (`AdminDashboardView.vue`)
- **欢迎横幅与快捷动作**：头部展示运营中枢标题与直达后台设置、用户管理的快捷 `VBtn` 操作。
- **四大核心度量卡片**：
  - 注册用户总量（`PeopleOutline`）
  - 正常启用账号数（`CheckmarkCircleOutline`）
  - Redis 响应延迟（`HardwareChipOutline`）
  - 实时推送总线状态（`RadioOutline`）
- **系统底层环境卡片**：直观展示内核版本号、Spring Boot / JDK 25 运行环境、Redis 连通性与 SSE 状态。

### 4.3 后台设置 (`AdminSystemView.vue`)
完全承接经典前端 `AdminSystemView.vue` 的全部平台设置与健康概览能力：
1. **系统健康概览卡片**：顶部栅格呈现系统内核版本、底层运行时、Redis 连通状态与实时延迟、SSE 监听状态。
2. **三标签分组表单**：
   - **平台基础设置 (General)**：平台全称、描述标语、ICP 备案号（采用 `VInput`）、默认语言（采用 `VSelection`）、开放注册开关、邮箱强制验证开关（采用 `n-switch`）。
   - **邮件服务配置 (SMTP)**：SMTP 服务器主机、端口、发信账号、发件人地址、授权码（采用 `VInput type="password"`）、发信人昵称、SSL/TLS 加密开关。提供“测试 SMTP 连通性”功能，通过 `VBtn` 调用 `testSmtpConnectionApi` 实时自检连通性。
   - **平台运维模式 (Maintenance)**：全站维护模式开关及警告说明、维护公告文本（采用 `VInput type="textarea"`）。
3. **底部操作栏**：
   - “重置为默认值”按钮：采用 `VBtn clz="normal"`。
   - “保存所有配置”按钮：采用 `VBtn clz="primary"`，携带 `loading` 状态，组装批量更新载荷调用 `batchUpdateAdminSystemSettingsApi`。

### 4.4 用户管理 (`AdminUserView.vue`)
深度融合六大指定通用组件的完整管理列表交互范式：
1. **指标色带 (Stat Ribbon)**：统计展示总用户数、管理员数、活跃数、封禁数。
2. **筛选工具栏**：
   - 搜索框：采用 `VSearch`，支持用户名、邮箱、组织关键词检索，监听回车与清除。
   - 角色筛选：采用 `VSelection`，支持按普通用户、管理员、超级管理员精准过滤。
   - 状态筛选：采用 `VSelection`，支持按正常启用、已封禁状态过滤。
   - 动作按钮：采用 `VBtn clz="normal"` 刷新，`VBtn clz="primary"` 新增用户。
3. **数据表格 (`VTable`)**：
   - 深度集成 Naive UI 渲染函数与 Google 色彩体系：
     - 用户列：结合 `VAvatar` 渲染圆形头像、用户名及邮箱副标题。
     - 角色列：根据权限渲染超级管理员（紫色徽章）、管理员（Google Blue 徽章）及普通用户徽章。
     - 状态列：渲染正常（Google Green 徽章）与封禁（Google Red 徽章）。
     - 操作列：挂载“编辑”(`VBtn clz="normal"`)、“封禁/解封”(`VBtn clz="normal"`) 与“删除”(`VBtn clz="warning"`)。
   - 远程分页：集成 `page`、`pageSize`、`itemCount` 与多档位尺寸选择器。
4. **用户编辑与新增模态弹窗 (`VModal`)**：
   - 采用 `VModal`，根据操作模式动态切换标题。
   - 表单全面采用 `VInput`（用户名、邮箱、密码、组织、简介）、`VSelection`（权限角色分配）与状态切换开关。
   - 底部操作按钮采用 `VBtn`（取消与提交保存）。
5. **删除二次确认模态弹窗 (`VModal`)**：
   - 采用受控 `VModal` 进行不可逆操作安全拦截，配合 `VBtn clz="warning"` 进行危险操作确认。

---

## 5. 多语言国际化支持 (i18n)

在 `src/volur-lite-view-NEXT/src/locales/` 下分别建立并补充管理端语言文件：
- `zh-CN/admin.json`：简体中文资源包（控制台、系统设置、用户管理、布局徽章）。
- `en-US/admin.json`：英文国际化资源包。
- `zh-TW/admin.json`：繁体中文国际化资源包。
- 在 `zh-CN/common.json`、`en-US/common.json`、`zh-TW/common.json` 中统一注入 `platform_admin` 键名。
- 在 `src/volur-lite-view-NEXT/src/i18n/index.ts` 中实现 `admin` 命名空间在路由命中 `/admin` 时的按需与预置静态装载。

---

## 6. 构建与类型验证

执行 `vue-tsc --noEmit && vite build` 严格验证：
- 全部组件 TypeScript 类型推导准确无误；
- `noUnusedLocals` 与 `noUnusedParameters` 静态检查完全通过；
- Vite 构建全量打包通过，生产 Chunk 正常输出。
