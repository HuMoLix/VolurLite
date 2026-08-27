# 前端请求层安全反序列化与 64 位雪花算法 ID 精度截断修复

## 1. 变更背景与问题根因

在后端保持原始数字返回（例如 `"id": 351000953698123783`）的前提下，JavaScript 内置的 `JSON.parse` 基于 IEEE 754 双精度浮点数标准，对超出 `Number.MAX_SAFE_INTEGER`（$2^{53} - 1 = 9007199254740991$）的长整型数字会自动进行截断与四舍五入。
例如 `351000953698123783` 被浏览器解析为 `351000953698123800`，导致在管理端用户视图（`AdminUserView.vue`）中切换用户状态时，发送了篡改后的错误 ID 请求：
`http://localhost:3000/api/v1/admin/users/351000953698123800/status/active?status=false`

---

## 2. 施工与修复方案

### 2.1 撤销后端临时修改
- 彻底回退上一轮对后端 `JacksonConfig.java`、`WebMvcConfig.java` 及各 VO 实体类的改动，保持后端代码库纯净。

### 2.2 前端 Axios `transformResponse` 安全反序列化加固
- **修改文件**：`src/volur-lite-view/src/utils/request.ts`
- **核心逻辑**：
  - 在 Axios 实例化时配置 `transformResponse` 钩子，植入高效安全的 `safeJsonParse` 预处理器；
  - 在原始 JSON 字符串进入 `JSON.parse` 之前，通过正则预先识别所有未被双引号包裹的 16 位及以上长整数（包括对象字段 `id`、数组 `ids` 等各类长数值），将其自动包裹为双引号字符串：
    ```ts
    export function safeJsonParse(data: any): any {
      if (typeof data !== 'string') {
        return data
      }
      try {
        const safeData = data.replace(/([:,\[]\s*)(-?\d{16,})(?=[,\s\]}])/g, '$1"$2"')
        return JSON.parse(safeData)
      } catch {
        return JSON.parse(data)
      }
    }
    ```
  - 既能 100% 保证 Snowflake 64-bit ID（如 `351000953698123783`）以高精度字符串保留，又不会误伤短整数（`page`、`size`、`total`）以及毫秒时间戳（`timestamp`）。

---

## 3. 验证与质量测试

1. **前端类型校验与打包构建**：
   - 运行 `npx vue-tsc --noEmit`：0 错误，0 告警；
   - 运行 `npm run build`：Vite 打包编译成功（耗时 4.40s）。
2. **后端全模块测试**：
   - 运行 `mvn test`：所有模块单元测试 100% 通过（0 Errors, 0 Failures）。

---

## 4. 涉及文件清单

- **新增**：
  - `agent/volur-design-md/2026-08-27-fix-frontend-snowflake-id-precision-loss-in-request.md`
- **修改**：
  - `src/volur-lite-view/src/utils/request.ts`
