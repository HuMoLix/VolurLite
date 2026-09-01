# 施工与审计记录：文件上传 MD5 摘要去重校验与头像渲染显示问题根治

## 1. 基本信息

- **任务名称**：文件/头像上传 MD5 摘要校验与去重、旧头像自动清理、Naive UI `<n-avatar>` 渲染状态死锁彻底修复
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-system`（`StorageService` 接口与实现层新增 `calculateMd5` 与 `getFileMd5` 支持 Local 与 OSS 双模式）
  - `volur-user`（`UserServiceImpl.uploadAndSetAvatar` 新增 MD5 对比校验与旧文件清理）
  - `volur-lite-view`（LocalStorage `avatar` -> `avatarUrl` 自动兼容降级、Pinia 响应式引用更新、`<n-avatar>` 动态唯一 Key 重建）

---

## 2. 变更要点与根因排查

### 2.1 为什么上传后可以访问图片 API，但 `<n-avatar>` 仍然显示用户名首字母？
1. **Naive UI `<n-avatar>` 的内部加载错误状态死锁**：
   - 当页面首次挂载时，`userStore.user?.avatarUrl` 尚未完成网络拉取（为 `undefined`）；
   - Naive UI 内部组件渲染空/无有效图片源的 `<img>` 后触发 `onError`，将其内部的 `hasLoadErrorRef` 状态置为 `true`，进而永久回退渲染插槽内容（即文字）；
   - 后续当 `avatarUrl` 异步就绪或用户完成上传后，如果 `<n-avatar>` 没有分配动态变更的 `:key`，Naive UI 的同一个组件实例**不会主动重置已锁死的 `hasLoadErrorRef` 状态**，导致虽然 URL 100% 正确且能访问，但界面仍然显示首字母；
2. **LocalStorage 历史缓存字段名差异 (`avatar` vs `avatarUrl`)**：
   - 早期登录返回的实体字段包含 `avatar`，而 Store 与 Profile 视图期望 `avatarUrl`，导致从本地缓存恢复时 `userStore.user.avatarUrl` 为 `undefined`；
3. **Pinia 实体直接属性修改未触发引用响应**：
   - 修改为 `user.value = { ...user.value, avatarUrl }`，确保 Vue 响应式追踪触发所有组件重渲染。

### 2.2 文件上传 MD5 摘要校验与取消重复上传
1. **接口定义**：
   - [`StorageService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/StorageService.java) 声明 `calculateMd5(MultipartFile file)` 与 `getFileMd5(String filePath)`；
2. **实现与存储模式兼容**：
   - [`StorageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/StorageServiceImpl.java) 实现本地与 OSS 环境下的 MD5 计算；
3. **业务层去重与旧文件清理**：
   - [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java) 在 `uploadAndSetAvatar` 中：
     - 若用户已有头像，计算新文件的 MD5 与旧头像文件的 MD5；
     - 若 MD5 相同，则取消冗余存储，直接返回旧头像地址；
     - 若 MD5 不同且新文件存储成功，自动删除旧头像物理文件，释放服务器存储空间。

---

## 3. 验证与审计结论

- **后端单元测试**：全平台 12 个模块执行 `mvn test` 全部通过（0 失败，0 错误）。
- **前端打包编译**：执行 `npm run build` 打包构建，TypeScript 类型检查全部通过（0 错误）。
