# 施工与审计记录：头像裁剪图像边界约束优化与密码修改/账号注销全流程 2FA 双重认证架构收敛

## 1. 基本信息

- **任务名称**：头像裁剪边界与黑色填充消除、修改密码与注销账号 2FA 双重验证实现及认证控制器架构收敛
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-verification`（注销邮件模板与 2FA 验证码派发服务）
  - `volur-notification`（安全事件类型扩展：`ACCOUNT_DELETED`）
  - `volur-auth`（2FA 注销账号接口、修改密码收敛、全端 Token 作废与事件总线发布）
  - `volur-user`（精简冗余接口，收敛为纯粹的用户画像与个人设置领域）
  - `volur-lite-view`（Canvas 裁剪边界钳位优化、个人中心修改密码与注销账号 2FA 交互）
- **关联文件**：
  - 后端：
    - [`EmailService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailService.java)
    - [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)
    - [`delete_account_email.html`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/resources/templates/email-templates/delete_account_email.html)
    - [`SecurityEventType.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/domain/enums/SecurityEventType.java)
    - [`DeleteAccountRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/DeleteAccountRequestDTO.java)
    - [`AuthService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java)
    - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
    - [`AuthController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)
    - [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java)
    - [`UserService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/UserService.java)
    - [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)
  - 前端：
    - [`AvatarCropperModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/user/AvatarCropperModal.vue)
    - [`ProfileView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)
    - [`src/types/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts)
    - [`src/api/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/auth/index.ts)
    - [`src/api/user/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/user/index.ts)
    - [`src/locales/zh_CN/profile.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/profile.json)
    - [`src/locales/en_US/profile.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/profile.json)

---

## 2. 核心架构决策与对照分析

### 问题剖析：`AuthController` 与 `UserController` 重复实现修改密码的优劣分析

| 维度 | `volur-auth` (`AuthController`) | `volur-user` (`UserController`) | 架构评判与结论 |
| :--- | :--- | :--- | :--- |
| **单一职责与领域边界 (DDD)** | **优**。凭证管理、2FA 鉴权、Token 颁发/吊销属于核心认证域 | **差**。与个人资料、头像设置等普通业务混杂 | 职责边界必须清晰收敛 |
| **多因子安全防护 (2FA)** | **支持**。强制通过绑定邮箱验证码核验 + Redis 短期令牌机制 | **无**。仅凭旧密码直接覆盖，缺少多因子防护 | 认证域具备完善的防爆破与验证码体系 |
| **会话与 Token 强制失效** | **支持**。修改后调用 `forceLogoutUser` 将全端 JWT 黑名单化 | **无**。修改后既有 Token 依然有效，存在劫持风险 | 认证域安全闭环更完备 |
| **安全审计与事件派发** | **支持**。发布 `SecurityNotificationEvent` 站内通知与日志 | **无**。仅打出局部日志 | 认证域具备平台级安全事件总线 |

**架构决策**：
彻底清理 `UserController` / `UserService` 中简易直改的密码与注销接口，将所有密码变更、凭证重置、账号注销全量统一收敛至 `volur-auth` / `AuthController`，保证单一数据流与最高安全等级。

---

## 3. 具体修改与功能落地

### 3.1 头像裁剪边界与黑色填充消除 (`AvatarCropperModal.vue`)
1. **缩放极限约束**：将最小缩放比例严格锁定为 `minScale = Math.max(canvasSize / img.width, canvasSize / img.height)`，确保无论如何缩小，图片始终完整覆盖裁剪区域；
2. **平移边界钳位**：新增 `clampPosition()`，动态将 `position.x` 限制在 `[canvasSize - w, 0]`，`position.y` 限制在 `[canvasSize - h, 0]`，拖拽或居中缩放时彻底杜绝露出底色或黑色边缘。

### 3.2 修改密码与注销账号 2FA 双重认证体系
1. **邮件服务支持 (`volur-verification`)**：
   - 增加专业高质感的 HTML 邮件模板 [`delete_account_email.html`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/resources/templates/email-templates/delete_account_email.html)；
   - `EmailService` 增加 `sendDeleteAccountEmail` 方法。
2. **认证服务与控制器 (`volur-auth`)**：
   - 新增 `DeleteAccountRequestDTO`；
   - `AuthController` 暴露 `POST /api/v1/auth/account/send-code`（发送注销 2FA 验证码）；
   - `AuthController` 暴露 `POST /api/v1/auth/account/delete`（校验 2FA 验证码与确认用户名后执行注销、清除会话并拉黑 Token）；
   - `SecurityEventType` 扩展 `ACCOUNT_DELETED`。
3. **前端个人中心交互 (`ProfileView.vue`)**：
   - **修改密码**：展示当前绑定邮箱，支持 60 秒倒计时发送邮箱验证码，校验成功后获取 Token 并重置密码，自动退出并引导重新登录；
   - **注销账号**：在危险操作弹窗中强制输入用户名，并获取 6 位邮箱验证码进行 2FA 确认，完成后清空本地凭据并返回登录页。

---

## 4. 验证结果

- **后端测试**：执行 `mvn test`，全平台 12 个子模块所有单元测试全部通过（0 失败，0 错误）。
- **前端打包**：执行 `npm run build`，编译与类型检查通过，打包生成正常。
