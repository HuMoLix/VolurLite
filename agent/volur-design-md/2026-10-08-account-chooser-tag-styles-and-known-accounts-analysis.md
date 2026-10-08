# AccountChooser 状态胶囊精简与本地已知账号三元标识模型架构解耦实现

本文档记录针对 `volur-lite-view-NEXT` 中账号选择器组件（`AccountChooserView.vue`）的 UI 视觉重构，并对客户端持久化存储 `volur_known_accounts` 中 `identifier`、`username`、`displayName` 三者并存的架构解耦实施方案与落地细节进行系统性记录。

---

## 1. 变更背景与需求

1. **状态胶囊视觉降噪（对齐 Google 现代体验）**：
   - 在经典 Google Accounts 账号切换界面中，当前处于活跃登录态（Active / Signed in）的账号本身即为标准可用项，不需要额外的标签分散用户注意力；
   - 处于退出状态（Signed out）的账号，过去采用了实体底色胶囊（`rgba(95, 99, 104, 0.12)`），视觉偏重；改为仅保留细微边框线的描边药丸（Outlined Pill），取消背景色；
   - 处于被封禁/禁用状态（Disabled）的账号保持醒目的风险红色胶囊。
2. **账号三大标识字段架构解耦落地**：
   - 彻底解开过去在登录成功与探活时将 `identifier`、`username`、`displayName` 盲目回填为同一字符串的硬编码逻辑；
   - 实现真实场景下的分层解耦：输入凭据（`identifier`）、系统业务唯一键（`username`）、表现层展示名（`displayName`，优先使用真实姓名/昵称，兜底用户名）、邮箱（`email`）及头像（`avatarUrl`）。

---

## 2. 施工与改动清单

### 2.1 视图组件调整 (`AccountChooserView.vue`)

- **文件路径**：`src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue`
- **改动详情**：
  1. **移除 “已登录” 状态胶囊**：
     - 在模板遍历账号项右侧区域，删除了 `acc.sessionStatus === 'ACTIVE'` 时渲染的 `status-tag status--active` 徽章；当账号为激活状态时保持极简无胶囊展示；
     - 仅保留 `DISABLED`（已停用）与 `SIGNED_OUT`（已退出）的胶囊渲染判定。
  2. **重塑 “已退出” 状态胶囊样式**：
     - 清除原本的背景色填充（`background-color: transparent`）；
     - 亮色模式下应用 Google Gray 300 细边框：`border: 1px solid #dadce0`，文本为 `#5f6368`；
     - 暗色模式下应用 Google Dark 600 细边框：`border: 1px solid #3c4043`，文本为 `#bdc1c6`；
     - 清理 SCSS 中未再使用的 `.status--active` 样式规则。
  3. **卡片标题与副标题去重提炼**：
     - 增加 `getAccountSubtitle(acc)` 逻辑：若有 `email` 则副标题优先展示 `email`；若无 `email` 但 `username` 或 `identifier` 与主标题（`displayName`）不同，才展示次级标识；若均相同则隐藏副标题，杜绝两行出现重复文本。

---

### 2.2 后端全链路解耦与数据补全

1. **`SignInFlowResponseVO` 模型扩展**：
   - 文件：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignInFlowResponseVO.java`
   - 扩展增加 `username`、`email`、`displayName`、`avatarUrl` 字段，使终态认证响应能够携带完整解耦后的用户属性。
2. **`SigninServiceImpl.java` 终态认证与探活逻辑解耦**：
   - 文件：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java`
   - **探活接口 (`probeAccounts`)**：不再硬编码 `.displayName(user.getUsername())`，而是通过 `StringUtils.hasText(user.getRealName()) ? user.getRealName().trim() : user.getUsername()` 优先提取真实姓名/展示昵称；同时保持 `identifier` 为客户端原始请求项（无论邮箱还是用户名）。
   - **终态认证接口 (`authenticate`)**：构建返回 `SignInFlowResponseVO` 时，保留用户登录时提交的 `identifier`（如邮箱），同时解耦提供数据库规范 `user.getUsername()`、`user.getEmail()`、`displayName` 及 `avatarUrl`。
3. **WebAuthn 用户实体模型对齐**：
   - 文件：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/webauthn/UserPublicKeyCredentialUserEntityRepository.java`
   - `displayName` 生成逻辑同步优先读取 `user.getRealName()`，回退 `user.getUsername()`，对齐 W3C WebAuthn 规范。

---

### 2.3 前端存储与状态机解耦

1. **TypeScript 类型对齐**：
   - 文件：`src/volur-lite-view-NEXT/src/types/auth/flow.ts`
   - `SignInFlowResponseVO` 同步增加 `username?: string; email?: string; displayName?: string; avatarUrl?: string`。
2. **Pinia 登录流状态机持久化更新**：
   - 文件：`src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts`
   - 在 `submitAuthenticate` 收到 `SUCCESS` 后，分别提取用户输入的 `rawIdentifier`、服务端权威的 `canonicalUsername`，以及服务端下发的 `email`、`displayName`、`avatarUrl`，完整传入 `saveKnownAccount`。
3. **本地缓存工具智能去重与属性合并**：
   - 文件：`src/volur-lite-view-NEXT/src/utils/accountChooser.ts`
   - `saveKnownAccount` 升级匹配规则：针对 `identifier`、`username` 和 `email` 进行全方位交叉匹配；已存在记录时进行增量属性平滑覆盖合并（保留已有的非空 `email`、`displayName`、`avatarUrl`）；
   - `removeKnownAccount` 同步支持按照 `identifier`、`username`、`email` 任意维度进行精确删除。

---

## 3. 解耦后的数据流转效果示例

### 场景 A：用户使用邮箱 `577896795@qq.com` 登录（账号名为 `HuMoLix123`，姓名设置了 `胡莫`）

1. **输入阶段**：
   - 前端记录 `identifier = "577896795@qq.com"`；
2. **认证终态**：
   - 服务端下发：
     ```json
     {
       "status": "SUCCESS",
       "identifier": "577896795@qq.com",
       "username": "HuMoLix123",
       "email": "577896795@qq.com",
       "displayName": "胡莫",
       "avatarUrl": "https://img.volur.com/avatar/humolix.png"
     }
     ```
3. **本地 `volur_known_accounts` 存储形态**：
   ```json
   {
     "identifier": "577896795@qq.com",
     "username": "HuMoLix123",
     "email": "577896795@qq.com",
     "displayName": "胡莫",
     "avatarUrl": "https://img.volur.com/avatar/humolix.png",
     "lastLoginTime": 1791464128190
   }
   ```
4. **账号选择器展示**：
   - 头像：显示自定义图片或截取 `"胡"`；
   - 主标题：展示 `"胡莫"`；
   - 副标题：展示 `"577896795@qq.com"`；
   - 点击该卡片：将以 `"577896795@qq.com"` 作为 `identifier` 发起快速挑战。

---

## 4. 验证与回归测试

1. **前端类型检查**：运行 `npx vue-tsc --noEmit`，通过（0 errors）；
2. **后端编译与单元测试**：
   - 执行 `mvn test-compile -pl volur-auth,volur-security -am`，编译通过；
   - 执行 `mvn test -pl volur-auth,volur-security`，159 个测试用例全部通过（0 Failures，0 Errors）。
