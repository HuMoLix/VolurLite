# 施工与审计记录：个人中心全功能构建与系统动态存储上传服务实现

## 1. 基本信息

- **任务名称**：个人中心模块（资料编辑、头像裁剪上传、隐私设置、密码修改、账号注销）与后端动态文件存储（OSS / Local）完整构建
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-system`（存储接口、动态配置与文件上传服务）
  - `volur-framework`（静态资源路由与 MVC 扩展）
  - `volur-user`（用户个人中心 API、资料更新、头像与密码安全管理）
  - `volur-lite-view`（前台个人中心视图、Canvas 头像裁剪组件、API 与国际化语言包）
- **关联文件**：
  - 后端：
    - [`FileUploadController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/controller/v1/FileUploadController.java)
    - [`StorageService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/StorageService.java)
    - [`StorageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/StorageServiceImpl.java)
    - [`StorageResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/domain/vo/StorageResponseVO.java)
    - [`SystemSettingKeys.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)
    - [`SystemSettingInitializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)
    - [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java)
    - [`UserService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/UserService.java)
    - [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)
    - [`UpdateUserProfileRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/dto/UpdateUserProfileRequestDTO.java)
    - [`UpdateAvatarRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/dto/UpdateAvatarRequestDTO.java)
    - [`ChangeUserPasswordRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/dto/ChangeUserPasswordRequestDTO.java)
    - [`WebMvcConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java)
  - 前端：
    - [`ProfileView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)
    - [`AvatarCropperModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/user/AvatarCropperModal.vue)
    - [`src/api/user/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/user/index.ts)
    - [`src/api/system/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/system/index.ts)
    - [`src/types/user/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/user/index.ts)
    - [`src/types/system/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/system/index.ts)
    - [`src/router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts)
    - [`src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)
    - [`src/locales/zh_CN/profile.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/profile.json)
    - [`src/locales/en_US/profile.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/profile.json)

---

## 2. 需求背景与架构设计

根据平台轻量化演进与用户个人中心功能规划，需补齐用户个人资料维护、头像上传与裁剪、账号安全、密码修改及账号注销功能，同时为全平台提供可动态切换底层存储（本地 Local 目录存储 vs 阿里云 OSS 对象存储）的通用文件存储组件。

### 架构设计要点：
1. **动态存储解耦**：系统配置中维护 `system.storage.type`（`LOCAL` / `OSS`）及 OSS 相关 Endpoint、AK、SK、Bucket 等参数，存储服务 `StorageService` 统一封装底层读写，业务层零侵入；
2. **前后端接口严格 1:1 对齐**：前端 DTO/VO 严格映射后端 Jakarta Validation 校验注解（密码复杂度正则、长度限制、非空约束等）；
3. **交互与视觉标准**：
   - 个人中心界面采用 Volur 极简设计（卡片容器、黑白主操作按钮、`#444444` 描述文本、暗黑模式支持）；
   - 自研交互式 HTML5 Canvas 头像裁剪模态框（支持缩放、拖拽平移、圆形蒙版预览与 Blob 导出上传）。

---

## 3. 具体改动内容

### 3.1 后端服务实现

1. **依赖引入**：
   - 在平台父 POM 与 `volur-system` 中引入 `aliyun-sdk-oss:3.18.5` 与 `commons-io:2.18.0`；
   - `volur-user` 模块依赖 `volur-system` 基础服务。
2. **文件存储服务 (`StorageService` & `StorageServiceImpl`)**：
   - 支持图片 (`uploadImage`) 与通用资源 (`uploadResource`) 上传；
   - 支持文件类型白名单校验与图片内容真实性探测（`ImageIO.read`）；
   - 支持路径安全化（防止目录遍历与双斜杠注入）；
   - 本地模式自动保存至配置目录（默认 `uploads/`），并生成 SHA-256 摘要与 Web 访问路由；
   - OSS 模式自动生成全局唯一对象 Key，上传后返回公网访问 URL。
3. **文件上传接口 (`FileUploadController`)**：
   - `POST /api/v1/system/upload/image`：上传图片；
   - `POST /api/v1/system/upload/resource`：上传通用资源。
4. **静态资源映射 (`WebMvcConfig`)**：
   - 增加 `/api/v1/uploads/**` 静态映射至本地 `file:uploads/` 目录。
5. **用户个人中心接口与业务层 (`UserController` & `UserServiceImpl`)**：
   - `GET /api/v1/users/me`：获取当前登录用户概要信息；
   - `GET /api/v1/users/profile`：获取当前用户详细资料；
   - `PUT /api/v1/users/profile`：更新个人资料（性别、生日、组织、专业、简介与隐私开关）；
   - `POST /api/v1/users/avatar`：直接上传头像并自动绑定当前用户资料；
   - `PUT /api/v1/users/avatar`：通过 URL 设置头像；
   - `PUT /api/v1/users/password`：验证原密码并安全修改登录密码；
   - `DELETE /api/v1/users/account`：安全注销当前账号（停用、隐藏、匿名化邮箱用户名）。

---

### 3.2 前端实现

1. **API 与类型定义**：
   - `src/types/user/index.ts` 补齐 `UserProfileResponseVO`、`UpdateUserProfileRequestDTO`、`UpdateAvatarRequestDTO`、`ChangeUserPasswordRequestDTO`；
   - `src/types/system/index.ts` 新增 `StorageResponseVO`；
   - `src/api/user/index.ts` 与 `src/api/system/index.ts` 导出强类型 Axios 请求函数。
2. **交互式头像裁剪组件 (`AvatarCropperModal.vue`)**：
   - 支持本地图片选择与拖拽加载；
   - Canvas 实时渲染、鼠标拖拽平移、滚轮及滑块缩放；
   - 1:1 圆形蒙版裁剪与 256x256 高清 Blob 导出上传。
3. **个人中心主视图 (`ProfileView.vue`)**：
   - **Hero 区域**：显示大号头像（悬浮显示“更换头像”微交互）、用户名、角色徽标、邮箱与注册时间；
   - **个人资料 Tab**：提供性别选择、生日日期选择器、组织/学校、专业及个人简介；
   - **隐私设置 Tab**：提供邮箱、电话、组织公开开关；
   - **账号安全 Tab**：包含旧密码/新密码/确认密码修改表单，以及危险区域账号注销双重校验弹窗（需输入用户名确认）。
4. **路由与导航集成**：
   - 在 `routes.ts` 的 `AppLayout` 下注册 `/profile` 路由；
   - 在 `AppLayout.vue` 用户下拉菜单中点击“个人中心”即可平滑导航到 `/profile`。
5. **双语国际化 (`zh_CN` / `en_US`)**：
   - 新增 `profile.json` 包含全部中英文词条。

---

## 4. 验证结果

- **后端验证**：
  - 执行 `mvn test`：全平台 12 个模块所有单元测试全部通过（0 失败、0 错误）；
  - 执行 `mvn test-compile`：全量构建耗时 12.9s，编译 100% 成功。
- **前端验证**：
  - 执行 `npm run build` (`vue-tsc --noEmit && vite build`)：耗时 7.58s，无类型与打包错误。
