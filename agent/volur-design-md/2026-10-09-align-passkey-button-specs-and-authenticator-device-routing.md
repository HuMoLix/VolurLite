# Passkey 按钮样式对齐与 WebAuthn 硬件设备路由改造方案

## 1. 变更背景与核心问题分析

### 1.1 按钮样式与平台不一致分析
在对比 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 时发现样式差异核心原因如下：
1. **HTML 标签差异**：
   - 在 `IdentifierView.vue` 中，“次”按钮（“创建账号”）采用原生 `<button type="button">`，配合 `.btn-create-account` 类；而“主”按钮（“下一步”）采用 Naive UI 的 `<n-button type="primary">`，配合 `.btn-next-step` 类；
   - 此前在 `PasskeyModal.vue` 中错误地将“次”按钮也包装为了 `<n-button>`，导致二次封装的 DOM 结构带来了多余的内边距、默认灰色投影与波纹图层，改变了原本纯扁平文本按钮的质感；
2. **字体与基线对齐机理**：
   - Tailwind 在本项目中配置了 `corePlugins: { preflight: false }`，原生 `<button>` 在浏览器下不会自动继承父级页面的 `font-family`（通常降级为默认的 Arial 或系统字体），而 Naive UI 的 `<n-button>` 内部继承了配置的 `vfonts`（Lato）；
   - 在将 `.auth-action-btn` 显式补充 `font-family: inherit !important; line-height: normal !important; display: inline-flex !important; align-items: center !important; justify-content: center !important;` 后，原生 `<button>` 与 `<n-button>` 的字体族、字阶与文本基准线完全一致。

### 1.2 WebAuthn 硬件认证器路由缺失问题
在浏览器调用 `navigator.credentials.get` / `navigator.credentials.create` 时：
1. “选择其他设备”原本应唤起外部硬件安全密钥（如 USB YubiKey、NFC、蓝牙或手机跨设备二维码）；
2. “继续”按钮原本应直接唤起 Windows 本机设备凭据（Windows Hello 指纹、面容或 PIN 码）；
3. 此前 `verifyPasskey` 与 `createPasskey` 在构建请求体时未区分 WebAuthn 传输通道与提示符（Hints），导致两者触发的均为浏览器默认的弹窗逻辑，未能精确路由至特定硬件形态。

---

## 2. 核心改动点

### 2.1 按钮结构与样式 100% 对齐 IdentifierView.vue
- 在 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 中：
  - “取消”与“选择其他设备”调整为原生 `<button type="button" class="auth-action-btn btn-create-account">`；
  - “创建安全密钥”/“继续”保持为 `<n-button type="primary" class="auth-action-btn btn-next-step">`；
  - 样式定义完全同步 `IdentifierView.vue` 中的悬浮色彩阶梯（亮色下 `rgba(11, 87, 208, 0.08)`，暗色下 `rgba(138, 180, 248, 0.12)`）与阴影。

### 2.2 WebAuthn 设备提示（Hints）与传输通道（Transports）精准分流
- 在 [`passkey.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/passkey.vue) 中：
  - **当 `attachment === 'cross-platform'`（选择其他设备）时**：
    - 注入 WebAuthn Level 3 提示符：`hints: ['security-key', 'hybrid']`；
    - 在 `allowCredentials` 中强制声明传输通道为 `transports: ['usb', 'nfc', 'ble', 'hybrid']`；
    - 操作系统将直接定向调出“安全密钥（插入 USB 密钥或使用手机扫码）”窗口；
  - **当 `attachment === 'platform'`（继续 / 验证）时**：
    - 注入 WebAuthn Level 3 提示符：`hints: ['client-device']`；
    - 在 `allowCredentials` 中声明传输通道为 `transports: ['internal']`；
    - 操作系统将直接调出本机 Windows Hello（PIN / 指纹 / 面容）验证对话框。

---

## 3. 验证与质量保证
- **零注释原则**：所有 `.vue` 与 `.ts` 代码保持极致纯净；
- **全链路构建**：前端通过 `npm run build`（`vue-tsc --noEmit && vite build`），0 类型报错。
