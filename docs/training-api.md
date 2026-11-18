# Volur 训练模块（Training）接口设计

> 状态：待前端对齐 v1
> 模块定位：面向学生的平时练题场景。题目**引用**自题库（`ctf_challenge`），训练池独立管理分值与启停；靶机与附件复用平台现有能力。

## 1. 总体决策（已定）

> **序列化约定（联调必读）**：平台全局将 `Long` 序列化为**字符串**（防 JS 精度丢失），时间序列化为 ISO-8601 UTC（如 `2026-11-03T13:00:00Z`）。因此本文档示例中的 `id`、`points`、`rank`、`totalPoints` 等数字字段**实际返回字符串**，前端按字符串接收、按需转数值。
>
> **鉴权约定**：令牌通过 `access_token` **Cookie** 携带（JwtAuthFilter 不读 Authorization 头）；所有非 GET 请求需 CSRF 双重提交：携带 `XSRF-TOKEN` Cookie 与同值 `X-XSRF-TOKEN` 请求头。

| 决策点 | 结论 |
| :--- | :--- |
| 题目来源 | **引用**题库题目（存 `source_challenge_id`），不做复制快照；题库题目被训练引用后不可物理删除 |
| 分值 | 导入时按难度给默认分，管理端可单题修改（见 §6） |
| 排行榜 | 做，训练模块内个人积分榜（见 §3.6） |
| 靶机 | 复用容器模块现有接口，前端用 `sourceChallengeId` 直调（见 §4） |
| 附件 | 详情接口直接返回原题附件地址列表，前端拿 URL 下载 |
| flag 判定 | 静态比对；错 flag 属正常业务结果（HTTP 200），限流类才走错误码（见 §7、§8） |
| 错误码 | 业务模块码使用 **9**（原"ext 预留"，需同步更新 errorCode.md 字典） |

## 2. 数据模型（2 张表，JPA 自动建）

**training_challenge（训练池）**
- `id`
- `source_challenge_id`（唯一，指向 `ctf_challenge.id`）
- `points`（导入时按难度默认，可改）
- `status`：`ENABLED` / `DISABLED`
- `created_by`、`created_at`

**training_solve_record（解题记录）**
- `id`
- `user_id`
- `source_challenge_id`
- `points_earned`（解出时刻的分值快照）
- `solved_at`
- 唯一约束 `(user_id, source_challenge_id)` —— 同一用户同一题只记一次分

> 移除训练池不删解题记录；重新导入同题后，已解状态与解出数自动恢复。

## 3. 用户端接口（`/api/v1/training`，登录即可）

### 3.1 题目列表 `GET /api/v1/training/challenges`

Query：`category`（分类 ID，可选）、`difficulty`（`easy|normal|mid|hard|difficult` 或中文别名 `入门|简单|中等|进阶|困难`，可选）、`keyword`（题名模糊，可选）、`page`、`size`

```json
{
  "items": [
    {
      "id": 1001,
      "sourceChallengeId": 88,
      "name": "baby-sql",
      "category": { "id": 3, "name": "Web" },
      "difficulty": "easy",
      "points": 100,
      "solvedCount": 42,
      "solvedByMe": true,
      "hasAttachment": true,
      "dynamicContainer": true
    }
  ],
  "total": 57,
  "page": 1,
  "size": 20
}
```

`dynamicContainer` = 原题配置了靶机镜像（前端据此显示"启动靶机"按钮）。

### 3.2 题目详情 `GET /api/v1/training/challenges/{id}`

```json
{
  "id": 1001,
  "sourceChallengeId": 88,
  "name": "baby-sql",
  "category": { "id": 3, "name": "Web" },
  "difficulty": "easy",
  "points": 100,
  "solvedCount": 42,
  "solvedByMe": true,
  "solvedAt": "2026-10-25T10:30:00Z",
  "description": "（Markdown，前端渲染）",
  "attachments": [ { "name": "baby-sql.zip", "url": "..." } ],
  "dynamicContainer": true
}
```

### 3.3 提交 flag `POST /api/v1/training/challenges/{id}/flag`

Body：`{ "flag": "flag{...}" }`

三种业务结果都是 HTTP 200，靠 `data.result` 区分：

```json
{ "result": "CORRECT",   "pointsEarned": 100, "totalPoints": 850 }
{ "result": "DUPLICATE", "pointsEarned": 0,   "totalPoints": 850 }
{ "result": "WRONG",     "pointsEarned": 0,   "totalPoints": 750 }
```

限流拦截为错误响应，见 §7、§8。

### 3.4 靶机状态 `GET /api/v1/training/challenges/{id}/instance`

返回当前用户在该题靶机的运行状态（无实例时报错误码 `153904`）：

```json
{
  "challengeId": 88,
  "accessUrl": "http://...",
  "status": "RUNNING",
  "createdAt": "...",
  "expiresAt": "...",
  "remainingTimeSeconds": 1740
}
```

启动/销毁/延期**不复用此路径**，见 §4。

### 3.5 个人进度 `GET /api/v1/training/progress`

```json
{
  "totalChallenges": 57,
  "solvedCount": 23,
  "totalPoints": 3450,
  "rank": 12,
  "categories": [
    { "category": "Web", "total": 15, "solved": 8 },
    { "category": "Crypto", "total": 10, "solved": 3 }
  ]
}
```

### 3.6 排行榜 `GET /api/v1/training/leaderboard?limit=50`

```json
{
  "items": [
    { "rank": 1, "userId": 1001, "nickname": "周予安", "avatar": "...", "points": 8200, "solvedCount": 51 }
  ],
  "myRank": { "rank": 12, "points": 3450, "solvedCount": 23 }
}
```

并列规则：积分 desc → 解出数 desc → 最后一次解出时间早者优先。`myRank` 在榜外也返回。

## 4. 靶机接口（复用现有，前端直调）

以下接口为容器模块现有能力，`{challengeId}` 传 **`sourceChallengeId`**（列表/详情已返回）：

| 操作 | 接口 |
| :--- | :--- |
| 启动 | `POST /api/v1/challenges/instances/{challengeId}/start` |
| 销毁 | `POST /api/v1/challenges/instances/{challengeId}/stop` |
| 延期 | `POST /api/v1/challenges/instances/{challengeId}/extend` |

**延期新规则（本次需求，容器模块同步修改）**：剩余时间低于初始时长的 **20%**（初始 30 分钟 → 剩 6 分钟内）才允许延期，每次 +15 分钟，不设总时长上限；剩余充足时延期返回业务错误。每人同时最多 2 个容器（已有）。

## 5. 管理端接口（`/api/v1/admin/training`，管理员）

选题浏览复用现有题库管理列表 `GET /api/v1/admin/challenges/ctf`，本模块只提供导入与训练池管理：

### 5.1 训练池列表 `GET /api/v1/admin/training/challenges`

Query：`page`、`size`、`keyword`、`status`。返回训练题 + 原题信息 + 分值 + 状态 + 解出数。

### 5.2 批量导入 `POST /api/v1/admin/training/challenges/import`

```json
// 请求
{ "challengeIds": [88, 92, 101] }
// 响应
{
  "imported": 2,
  "skipped": [
    { "challengeId": 92, "reason": "ALREADY_IN_POOL" },
    { "challengeId": 101, "reason": "NOT_FOUND" }
  ]
}
```

导入时按 §6 默认分赋值。

### 5.3 修改 `PATCH /api/v1/admin/training/challenges/{id}`

Body：`{ "points": 300 }` 和/或 `{ "status": "DISABLED" }`。下架后用户端列表即不可见（已解记录保留）。

### 5.4 移除 `DELETE /api/v1/admin/training/challenges/{id}`

移出训练池，不删解题记录；同题重新导入后数据自动恢复。

## 6. 分值默认表（常量，可调整）

| 难度 | 枚举 | 默认分 |
| :--- | :--- | :--- |
| 入门 | `easy` | 100 |
| 简单 | `normal` | 200 |
| 中等 | `mid` | 300 |
| 进阶 | `hard` | 400 |
| 困难 | `difficult` | 500 |

## 7. flag 提交限流（Redis）

| 规则 | 参数 | 拦截行为 |
| :--- | :--- | :--- |
| 全局限速 | 每用户每 2 秒 1 次（所有题共享） | 错误码 `123901`，message 含剩余等待秒数 |
| 单题冷却 | 错后同题 5 秒内不可再交 | 错误码 `123902`，message 含剩余等待秒数 |
| 错误锁定 | 同题累计错 10 次，锁题 5 分钟 | 错误码 `123903`，message 含剩余锁定时长 |

靶机启动接口同样加限速：每用户 10 秒 1 次（错误码 `123905`）。

## 8. 错误码（模块码 9，遵循 6 位规范 A-B-C-D-EE）

| errorCode | 含义 |
| :--- | :--- |
| `153901` | 训练题不存在 |
| `153902` | 训练题已下架 |
| `153903` | 导入的题目不在题库中 |
| `153904` | 当前无运行中的靶机实例 |
| `123901` | flag 提交过于频繁（全局） |
| `123902` | 单题提交冷却中 |
| `123903` | 连续错误过多，题目已锁定 |
| `123905` | 靶机启动过于频繁 |

> 同步事项：`docs/errorCode.md` §3.3 中模块码 `9`（ext 预留）的定义更新为 `training 训练与平时练习`。

## 9. 配套的现有代码修改（后端）

1. 容器模块 `CtfInstanceServiceImpl#extendInstance`：新增"剩余 < 初始时长 20% 才可延期"判断；常量 `MAX_EXTEND_MINUTES` 更名 `EXTEND_STEP_MINUTES`（15 分钟），新增 `EXTEND_THRESHOLD_PERCENT`（20%）。
2. 题库删除接口（`AdminCtfChallengeController`）：删除前校验"未被训练池引用"，被引用则拒绝（错误码 `153905`）。
