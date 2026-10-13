# 全局响应拦截器单一职责与 Blob 异常错误解析改造

## 1. 变更背景与问题定位
在管理后台赛题模块（如 `/admin/challenges/theory/:id/edit`）调用题目导出接口（`GET /api/v1/admin/challenges/theory/banks/:bankId/questions/export`）时，当后端返回错误（例如题库无题目抛出 `BusinessException` 返回 HTTP 404 业务错误）：
1. 界面上连续弹出两次 Toast 错误提示；
2. 弹出的错误内容为原生的英文状态信息（`Request failed with status code 404`），未能展示后端返回的真实业务报错（如“当前题库无题目可供导出”）。

经过全链路调用分析，确定根本原因如下：
1. **职责重叠导致二次弹窗**：Axios 响应拦截器（`src/utils/request.ts`）默认会在非 2xx 响应时触发全局 Toast（`window.$message.error`）并抛出错误；而业务组件（如 `AdminTheoryChallengeEditView.vue`）的 `catch` 块中再次手动调用了 `message.error`，导致同一错误在屏幕上重复触发两次弹窗。
2. **`responseType: 'blob'` 导致错误载荷解析失效**：文件下载或导出请求中声明了 `responseType: 'blob'`。当后端接口异常（HTTP 4xx/5xx）返回 JSON 错误体时，浏览器 Axios 会将响应体反序列化为 `Blob` 实例。拦截器读取 `error.response?.data?.message` 为 `undefined`，降级读取原生 `error.message`，导致真实的后端业务报错被遮蔽。

---

## 2. 改动点清单

### 2.1 全局 Axios 响应拦截器改造 (`src/utils/request.ts`)
1. **异步拦截器升级**：将 `service.interceptors.response.use` 的成功与失败回调转为异步函数 (`async`)。
2. **Blob 错误解析增强**：
   - 在失败回调中探测 `error.response?.data instanceof Blob`，若为 Blob 则通过 `await error.response.data.text()` 转换为字符串并解析 JSON。
   - 提取 JSON 中的 `message` 字段作为 `errorMsg`，若提取成功则优先使用后端返回的真实业务错误文本。
   - 在成功回调中同样增加对 `response.data instanceof Blob` 且类型包含 `application/json` 的探测，若解析出 `success: false` 则自动触发全局错误 Toast 并 reject。
3. **保证拦截器单一职责**：全局拦截器作为全站 HTTP 错误统一反馈的唯一责任方（除非显式配置 `skipToast: true`）。

### 2.2 题目管理视图及弹窗组件解耦 (`src/views/admin/challenge/`)
清理以下视图与弹窗中所有与 API 调用重叠的 `message.error` 冗余调用，避免与全局拦截器冲突：
- `AdminTheoryChallengeEditView.vue`：
  - `fetchBankDetail`、`fetchQuestionList`、`handleQuestionFileSelected`、`handleExportQuestions`、`handleDeleteQuestion` 移除 catch 块冗余 `message.error`。
- `AdminTheoryChallengeView.vue`：
  - `fetchBankList`、`handleOpenEditBank`、`handleSaveBank`、`handleDeleteBank`、`handleBankFileSelected`、`handleExportBanks` 移除 catch 块冗余 `message.error`。
- `components/AdminTheoryCreateChallengeModal.vue`：
  - `loadDetail`、`handleSave` 移除 catch 块冗余 `message.error`（保留客户端表单校验相关的 `message.warning`）。
- `AdminCtfChallengeView.vue`：
  - `fetchChallenges`、`handleExportChallenges`、`handleDeleteChallenge` 移除 catch 块冗余 `message.error`。
- `AdminCtfChallengeEditView.vue`：
  - `fetchChallengeDetail`、`handleSave` 移除 catch 块冗余 `message.error`（保留跳转逻辑）。
- `components/CtfCategoryModal.vue`：
  - `fetchCategories`、`handleCreateCategory`、`handleSaveEditCategory`、`handleDeleteCategory` 移除 catch 块冗余 `message.error`。
- `components/CtfImportModal.vue`：
  - `downloadTemplate`、`handleSubmitImport` 移除 catch 块冗余 `message.error`。

---

## 3. 安全与架构影响说明
1. **单一数据真实源与单一职责**：明确了全局响应拦截器负责全链路 HTTP 错误呈现的单一职责，消除了调用方防御性代码与提示代码的随意扩散。
2. **平滑降级与安全性**：在解析 Blob 载荷时采用安全沙箱与 `try...catch` 保护，对于非 JSON 的纯二进制流异常自动平滑降级为原生网络错误信息，不影响正常文件下载流程。
3. **编译与类型安全**：重构后前端工程在 Vite + Vue-TSC 全量构建中 100% 编译通过，无任何类型缺失或破坏性改动。
