# 施工与审计记录：个人资料性别枚举前后端对齐 (方案B)

## 1. 基本信息

- **任务名称**：前端 Gender 枚举与后端 Gender (UNKNOWN, MALE, FEMALE) 1:1 严格对齐，修复资料更新 400 报错
- **执行时间**：2026-09-07
- **影响文件**：
  - `volur-lite-view`：
    - [`src/volur-lite-view/src/types/user/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/user/index.ts)
    - [`src/volur-lite-view/src/views/profile/ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)
    - [`src/volur-lite-view/src/locales/zh_CN/profile.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/profile.json)
    - [`src/volur-lite-view/src/locales/en_US/profile.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/profile.json)

---

## 2. 变更背景与根因

- **问题现象**：调用 `PUT /api/v1/users/profile` 更新个人资料时，后端响应 `400 "请求体格式不正确或缺少请求体"`。
- **根本原因**：
  - 后端 [`Gender.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/enums/Gender.java) 仅声明了 `UNKNOWN(0, "UNKNOWN")`、`MALE(1, "MALE")`、`FEMALE(2, "FEMALE")`；
  - 前端原定义包含 `'SECRET'` 与 `'OTHER'`，且默认初始化为 `'SECRET'`；
  - Jackson 在反序列化字符串 `"SECRET"` 到枚举 `Gender` 时无法匹配，抛出 `HttpMessageNotReadableException`，被全局异常处理器转为 400 报错。

---

## 3. 具体修复方案 (方案B：前端严格对齐后端)

1. **类型对齐**：
   在 [`types/user/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/user/index.ts) 中将 `Gender` 类型更新为 `'UNKNOWN' | 'MALE' | 'FEMALE'`，与后端完全一致。
2. **表单选项与初始化**：
   在 [`ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue) 中：
   - 默认值由 `'SECRET'` 改为 `'UNKNOWN'`；
   - `genderOptions` 包含 `MALE`（男）、`FEMALE`（女）、`UNKNOWN`（保密）；
   - `initFormData` 回退值调整为 `'UNKNOWN'`。
3. **多语言词条对齐**：
   在 `zh_CN/profile.json` 和 `en_US/profile.json` 中配置 `"gender_unknown"` 映射至“保密”/“Secret”，保持向下兼容。

---

## 4. 验证与审计结论

- **前端构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），静态类型检查与打包 0 错误通过。
- **接口契约**：前端个人资料更新接口发送的 `gender` 字段严格为 `UNKNOWN` / `MALE` / `FEMALE`，Jackson 顺利反序列化，彻底杜绝 400 格式错误。
