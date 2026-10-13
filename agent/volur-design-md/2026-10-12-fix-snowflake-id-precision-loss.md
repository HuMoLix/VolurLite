# 雪花算法 ID 精度丢失与大数类型转换缺陷修复

## 1. 变更背景与动因
在竞赛管理后台（`/admin/competitions/{competitionId}/edit?tab=challenges`）批量从题库挂载赛题时：
1. 题库列表接口 `GET /api/v1/admin/challenges/ctf?page=1&size=10` 返回的题目 ID 为标准 64 位雪花算法（Snowflake ID）字符串，如 `"367978528756465667"`；
2. 批量挂载接口 `POST /api/v1/admin/competitions/{competitionId}/ctf/mount/batch` 发送的请求体中，该 ID 却被序列化为数值 `367978528756465660`，导致末尾低位精度截断丢失（`67` 变为 `60`）；
3. 后端服务通过该错误 ID 查询题库持久化记录不存在，进而抛出 `404 题库中题目不存在` 业务异常。

### 根因分析：
JavaScript 原生 `Number` 类型遵循 IEEE 754 双精度 64 位浮点数标准，有效尾数精度为 53 位，最大安全整数为 `Number.MAX_SAFE_INTEGER`（$2^{53} - 1 = 9,007,199,254,740,991$）。
当雪花算法生成的长整型 ID（18 位，约为 $3.68 \times 10^{17}$）通过 `Number(id)` 进行显式类型转换时，低位比特因超出尾数表示范围而被舍入，最终导致序列化时产生精度丢失。

---

## 2. 核心改动明细

### 2.1 赛题挂载标签页修复 (`ChallengeTab.vue`)
- 路径：`src/views/admin/competition/tabs/ChallengeTab.vue`
- 在 `handleConfirmImport` 组装批量挂载载荷时，移除 `challengeId: Number(id)` 强转，直接保留 `challengeId: id` 原生字符串形式。
- 类型系统 `CtfChallengeMountRequestDTO` 已声明 `challengeId: number | string`，保持字符串类型在 JSON 传输时直接被 Jackson 正确反序列化为 Java `Long`，杜绝任何精度丢失。

### 2.2 容器靶机管理标签页排查与加固 (`ContainerTab.vue`)
- 路径：`src/views/admin/competition/tabs/ContainerTab.vue`
- 在 `fetchInstances` 中，调用 `getAdminContainerListApi` 时移除原 `stageId: Number(props.compId)` 强转，改为 `stageId: props.compId`。
- 消除当竞赛/阶段 ID 为大整数雪花 ID 时可能引发的同类请求参数精度丢失隐患。

---

## 3. 影响范围与验证结果
- **修改文件**：
  - `src/views/admin/competition/tabs/ChallengeTab.vue`
  - `src/views/admin/competition/tabs/ContainerTab.vue`
- **验证结果**：
  - 执行 `vue-tsc --noEmit` 类型检查，所有 TypeScript 强类型校验 100% 通过（Exit Code 0）。
  - 请求载荷保持高精度字符串传输，彻底解决 404 题目不存在及大数舍入异常。
