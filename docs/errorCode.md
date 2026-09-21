# Volur Lite 统一错误代码（ErrorCode）设计规范与编码字典

## 1. 规范背景与设计目标

在大型分布式与前后端分离架构中，仅依赖 HTTP 协议状态码（如 400, 401, 403, 404, 500）已无法满足细粒度的业务区分需求：
1. **精确前端决策**：同样是 HTTP 401（Unauthorized），可能是“请求未携带 Token”（需直接跳转登录）、“Token 过期”（需触发无感 Refresh Token）、“Token 在黑名单中已注销”（需清理本地缓存并提示重新登录）、或“异地登录被强制挤下线”（需弹出强提醒对话框）。若无业务错误代码，前端只能通过文本匹配，极易因文案修改或多语言切换而失效。
2. **全链路可观测性与告警**：监控系统（Prometheus、ELK、Sentry）可针对特定 `errorCode` 聚类统计并设立告警阈值（如 `131303` 剧增预示 Token 续期策略异常；`170101` 激增预示邮件网关故障）。
3. **国际化与多语言解耦**：前端根据 `errorCode` 即可独立决定展示文案或执行控制逻辑，后端 `message` 仅作为调试与默认兜底参考。

---

## 2. 编码格式与位权规范 (6 位定长标准)

全平台统一采用 **6 位十进制整数（A-B-C-D-EE）** 作为标准错误识别码：

```
┌───────┬──────────────┬──────────────┬──────────────┬──────────────────┐
│  A    │      B       │      C       │      D       │        EE        │
│ (1位) │    (1位)     │    (1位)     │    (1位)     │      (2位)       │
├───────┼──────────────┼──────────────┼──────────────┼──────────────────┤
│ 来源  │   错误类型   │   业务模块   │   严重等级   │   具体错误序号   │
└───────┴──────────────┴──────────────┴──────────────┴──────────────────┘
```

> **说明与历史版本澄清**：
> 历史简述文档中曾提及“8 位”，系在设计初期将后续流水号预留为 4 位（即 `A-B-C-D-EEEE`）。经工程实践验证，在 4 层分类树下，单个分类末端分配 2 位序号（01~99，可容纳 100 种具体错误）已完全满足中大型平台需求，且 6 位代码在网络载荷、日志排查与人机识别上更为紧凑高效。因此，**平台统一以 6 位定长为正式工程标准**，全面兼容当前已实装代码（如 `131301`~`131306`）。

---

## 3. 各维度详细枚举字典

### 3.1 第 1 位：错误来源与责任域 (Source)

| 代码 | 来源定义 | 核心定位与判定场景 |
| :--- | :--- | :--- |
| **`1`** | **服务端 / 内部处理** | 服务端核心业务逻辑拦截、安全防线阻断、内部执行故障、数据库或基础设施异常。即使是由客户端传参引起，但由服务端安全过滤器、拦截器或业务断言主动侦测并阻断的（如 Token 缺失、账号冻结、限频阻断），均归于此类。 |
| **`2`** | **客户端 / 外部调用** | 客户端显式违背契约错误，如参数格式不匹配、JSON 反序列化错误、缺失必要路径参数、调用未允许的 HTTP 方法、客户端主动取消请求等。 |

---

### 3.2 第 2 位：错误大类 (Category)

| 代码 | 类别名称 | 英文标识 | 典型场景 |
| :--- | :--- | :--- | :--- |
| **`1`** | **系统错误** | System Error | 数据库连接失败、Redis 超时、空指针异常、未捕获的运行时故障 |
| **`2`** | **业务逻辑错误** | Business Rule Error | 业务状态机流转阻断、前置校验不满足、竞赛尚未开始、队伍满员 |
| **`3`** | **参数与凭据错误** | Parameter / Credential Error | 缺少 Token 请求头、表单字段格式非法、验证码错误、输入超长 |
| **`4`** | **认证与权限错误** | Auth & Permission Error | 用户名或密码错误、Token 伪造、越权访问、角色权限不足 |
| **`5`** | **资源状态错误** | Resource State Error | 请求的实体不存在、唯一索引重复冲突、资源被并发锁定 |
| **`6`** | **网络与通信错误** | Network / Transport Error | 内部 RPC 超时、长连接异常断开、网关转发失败 |
| **`7`** | **第三方服务错误** | Third-party Service Error | SMTP 邮件服务器投递失败、Docker Daemon 通信中断、外部 OAuth 异常 |
| **`8`** | **其他与未知错误** | Other / Unknown Error | 降级兜底错误、未预期的非标准错误 |

---

### 3.3 第 3 位：所属业务模块 (Module)

| 代码 | 模块标识 | 模块名称 | 包含的核心业务功能 |
| :--- | :--- | :--- | :--- |
| **`0`** | `system` | **系统与基础设施** | 系统设置、动态配置、异步任务调度、系统健康度监控 |
| **`1`** | `auth` | **认证与安全凭据** | JWT 颁发与校验、登录登出、人机验证码校验、加解密与防重放 |
| **`2`** | `user` | **用户与身份信息** | 用户档案、角色与权限管理、团队/战队成员关系、个人安全中心 |
| **`3`** | `notice` | **通知与邮件通信** | 邮件发送、Webhook 推送、站内广播公告、系统事件投递 |
| **`4`** | `container` | **容器与动态靶机** | Docker 镜像管理、动态靶机实例生命周期、端口映射、容器资源限制 |
| **`5`** | `competition` | **竞赛与天梯赛事** | 比赛创建与配置、队伍报名审核、动态天梯榜、实时积分结算、防作弊 |
| **`6`** | `challenge` | **题库与赛题挑战** | CTF 题目、理论题库、Flag 判题逻辑、题目附件管理、分值策略 |
| **`7`** | `data` | **数据统计与分析** | 平台运营大屏、提交热力图、战队胜率分析、指标监控 |
| **`8`** | `audit` | **审计日志与监控** | 用户登录日志、操作审计、管理后台变更记录、异常行为追踪 |
| **`9`** | `ext` | **扩展与预留模块** | 自定义插件、第三方开放平台生态预留 |

---

### 3.4 第 4 位：错误严重程度 (Severity)

| 代码 | 级别名称 | 标识 | 处理原则与行为 |
| :--- | :--- | :--- | :--- |
| **`1`** | **致命错误** | Fatal | 系统处于不可用或高危边缘（数据损坏、资金/积分作弊、密钥泄露）。必须立即记录 ERROR 日志并触发紧急运维告警。 |
| **`2`** | **严重错误** | Critical | 关键业务链路中断（如整个邮件通道断开、用户无法登录）。记录 ERROR 日志并触发即时通知。 |
| **`3`** | **一般错误** | Warning | 预期的业务拒绝或安全防御（如密码输入错误、Token 过期、竞赛未开始、参数校验失败）。记录 WARN/INFO 日志，客户端友好提示。 |
| **`4`** | **轻微提示** | Notice | 幂等操作冗余触发、非阻断性警告。通常直接返回并由客户端 Toast 提示。 |

---

### 3.5 第 5~6 位：细分流水序号 (Sequence)

- 取值范围：`01` ~ `99`（十进制两位定长，不足两位前导补零）。
- 编码规则：在“来源-类型-模块-等级”确定后，按业务发现顺序从 `01` 递增分配。

---

## 4. 已实装与典型错误代码字典表

### 4.1 认证模块：Token 与登录凭据异常 (`1313xx`)

> 位于 `com.humolix.volur.framework.common.exception.authException.tokenException`

| 错误识别码 | 对应 Java 异常类 | HTTP Status | 默认文案 Key / 消息 | 前端决策行为 |
| :--- | :--- | :--- | :--- | :--- |
| **`131301`** | `TokenMissingException` | 401 Unauthorized | 请求中缺少访问令牌 | 跳转至登录页，提示“请先登录账号” |
| **`131302`** | `InvalidTokenException` | 401 Unauthorized | 访问令牌无效或解析失败 | 清理本地凭据并引导重新登录 |
| **`131303`** | `TokenExpiredException` | 401 Unauthorized | 访问令牌已过期 | **触发无感刷新流程**（调用 `/api/v1/auth/refresh`） |
| **`131304`** | `TokenMalformedException` | 401 Unauthorized | 访问令牌格式错误 | 判定为非法伪造，清除存储并登出 |
| **`131305`** | `TokenBlacklistedException` | 401 Unauthorized | 访问令牌已失效（登出黑名单） | 清理本地凭据，禁止刷新 |
| **`131306`** | `UserForceLoggedOutException` | 401 Unauthorized | 账号已在其他设备登录 | **弹窗强提醒**：“您的账号已在其他设备登录”，确认后登出 |

---

### 4.2 认证与安全其他异常 (`1413xx` / `2313xx`)

| 错误识别码 | 场景描述 | HTTP Status | 推荐文案 | 前端处理建议 |
| :--- | :--- | :--- | :--- | :--- |
| **`141301`** | 用户名或密码错误 (`BadCredentialsException`) | 401 Unauthorized | 用户名或密码错误 | 表单报错，提示剩余重试次数 |
| **`141302`** | 账号已被封禁/禁用 | 403 Forbidden | 账号已被禁用，请联系管理员 | 模态框拦截，禁止任何操作 |
| **`141201`** | 越权访问或角色权限不足 (`AccessDeniedException`) | 403 Forbidden | 权限不足，拒绝访问该资源 | 弹出“无权操作”提示或跳转 403 页面 |
| **`231301`** | 人机验证码错误或已过期 | 400 Bad Request | 验证码错误或已过期，请重新验证 | 自动刷新图形/滑动验证码 |
| **`121301`** | 登录尝试过于频繁（触发风控限流） | 429 Too Many Requests | 登录尝试过于频繁，请稍后重试 | 锁定提交按钮并展示倒计时 |

---

### 4.3 用户与团队模块 (`x223xx`)

| 错误识别码 | 场景描述 | HTTP Status | 推荐文案 | 前端处理建议 |
| :--- | :--- | :--- | :--- | :--- |
| **`252301`** | 用户资源未找到 | 404 Not Found | 目标用户不存在 | 路由回退或展示空状态 |
| **`252302`** | 用户名或邮箱已存在（唯一性冲突） | 409 Conflict | 该用户名或邮箱已被注册 | 表单对应字段标红报错 |
| **`232301`** | 密码复杂度不符合安全基线 | 400 Bad Request | 密码长度必须在 8~32 位之间，且包含字母与数字 | 实时密码强度校验提示 |
| **`122301`** | 团队已达到最大人数上限 | 400 Bad Request | 战队人数已达上限，无法加入 | 提示“战队已满员” |

---

### 4.4 竞赛与挑战赛题模块 (`x553xx` / `x663xx`)

| 错误识别码 | 场景描述 | HTTP Status | 推荐文案 | 前端处理建议 |
| :--- | :--- | :--- | :--- | :--- |
| **`125301`** | 竞赛尚未开始 | 400 Bad Request | 竞赛尚未开放，暂不能查看赛题 | 锁定赛题视图并展示倒计时 |
| **`125302`** | 竞赛已结束 | 400 Bad Request | 竞赛已结束，停止提交 Flag | 禁用提交输入框与按钮 |
| **`236301`** | 提交的 Flag 格式错误 | 400 Bad Request | Flag 格式不符合规范 | 前端正则快速校验拦截 |
| **`126301`** | 提交的 Flag 不正确 | 200 OK / 400 Bad Request | Flag 判定错误 | 错误震动反馈，扣减防刷配额 |
| **`126302`** | 该赛题已被本人/本队解决（重复提交） | 400 Bad Request | 您已成功攻克此题，无需重复提交 | 提示已完成状态 |
| **`126303`** | Flag 提交限频中 | 429 Too Many Requests | 提交过于频繁，请等待冷却时间 | 展示冷却倒计时进度条 |

---

### 4.5 容器与动态靶机模块 (`1742xx`)

| 错误识别码 | 场景描述 | HTTP Status | 推荐文案 | 前端处理建议 |
| :--- | :--- | :--- | :--- | :--- |
| **`174201`** | Docker 靶机实例启动失败 | 500 Internal Server Error | 靶机环境启动失败，请联系管理员 | 提示重试或向裁判提交工单 |
| **`124301`** | 容器靶机配额耗尽（单用户仅限1台） | 400 Bad Request | 您已有正在运行的靶机，请先销毁 | 提供快捷跳转“销毁已有靶机” |
| **`124302`** | 靶机实例已过期自动销毁 | 404 Not Found | 靶机环境已过期释放 | 更新页面状态为已销毁 |

---

### 4.6 系统与基础设施模块 (`1101xx` / `1701xx`)

| 错误识别码 | 场景描述 | HTTP Status | 推荐文案 | 前端处理建议 |
| :--- | :--- | :--- | :--- | :--- |
| **`110101`** | 系统内部未知异常 (Fallback 500) | 500 Internal Server Error | 服务器内部故障，请联系管理员 | 友好错误浮窗，附带追踪 ID |
| **`170101`** | SMTP 邮件通信服务不可用 | 500 Internal Server Error | 邮件服务暂时不可用，请稍后再试 | 提示用户稍后重试获取验证码 |
| **`120301`** | 全局接口限频阻断 | 429 Too Many Requests | 请求过于频繁，请稍后重试 | 限制用户快速连击点击 |

---

## 5. 后端架构实现规范 (Backend Architecture)

### 5.1 响应工厂方法 (`ApiResponseEntity`)

在 `ApiResponseEntity` 中全面支持显式注入 `errorCode` 的静态工厂方法：

```java
// 400 Bad Request
public static <T> ApiResponse<T> badRequest(int errorCode, String messageKey);
public static <T> ApiResponse<T> badRequest(int errorCode, String messageKey, T data);

// 401 Unauthorized
public static <T> ApiResponse<T> unauthorized(int errorCode, String messageKey);

// 403 Forbidden
public static <T> ApiResponse<T> forbidden(int errorCode, String messageKey);

// 404 Not Found
public static <T> ApiResponse<T> notFound(int errorCode, String messageKey);

// 405 Method Not Allowed
public static <T> ApiResponse<T> methodNotAllowed(int errorCode, String messageKey);

// 409 Conflict
public static <T> ApiResponse<T> conflict(int errorCode, String messageKey);

// 429 Too Many Requests
public static <T> ApiResponse<T> tooManyRequests(int errorCode, String messageKey);

// 500 Internal Server Error / 通用 Error
public static <T> ApiResponse<T> error(int errorCode, String messageKey);
public static <T> ApiResponse<T> error(HttpStatusCode status, int errorCode, String messageKey);
public static <T> ApiResponse<T> internalServerError(int errorCode, String messageKey);
```

### 5.2 业务异常定义模式 (`BusinessException` 继承树)

所有领域业务异常必须直接或间接继承 `BusinessException`，并在构造函数中显式注入标准 6 位 `errorCode`：

```java
package com.humolix.volur.framework.common.exception.authException.tokenException;

public final class TokenMissingException extends TokenAuthenticationException {

    public TokenMissingException() {
        // 1 (服务端) + 3 (参数/凭据缺失) + 1 (认证模块) + 3 (一般严重) + 01 (序号)
        super(131301, "请求中缺少访问令牌");
    }

}
```

---

## 6. 前端消费与决策最佳实践 (Frontend Integration)

前端强类型 Axios 拦截器（`src/utils/request.ts`）基于 `errorCode` 实现精确状态流转与程序化分支：

```typescript
// src/utils/request.ts 响应拦截器示例
service.interceptors.response.use(
  (response) => {
    // 成功响应 (success === true, errorCode === 0)
    return response
  },
  async (error) => {
    const { status, data } = error.response || {}
    const errorCode = data?.errorCode

    // 1. 基于细粒度 errorCode 的精准分支判定
    if (status === 401) {
      switch (errorCode) {
        case 131303: // TokenExpiredException - 访问令牌过期，触发无感刷新
          return handleTokenRefresh(error.config)

        case 131306: // UserForceLoggedOutException - 异地登录顶号
          clearAuthStorage()
          window.$dialog?.warning({
            title: '下线提醒',
            content: '您的账号已在其他设备登录，如非本人操作请尽快修改密码。',
            positiveText: '重新登录',
            onPositiveClick: () => { window.location.href = '/login' }
          })
          return Promise.reject(error)

        case 131301: // TokenMissingException - 未携带令牌
        case 131305: // TokenBlacklistedException - 令牌已注销
        default:
          clearAuthStorage()
          window.$message?.warning(data?.message || '请先登录账号')
          window.location.href = '/login'
          return Promise.reject(error)
      }
    }

    // 2. 统一通用 Toast 提示 (脱离单纯字符串比对)
    if (data?.message) {
      window.$message?.error(data.message)
    }
    return Promise.reject(error)
  }
)
```

---

## 7. 错误代码治理与生命周期铁律

1. **唯一性铁律 (Uniqueness)**：已发布的错误代码具有全局唯一性，严禁两个不同业务含义的异常复用同一个 `errorCode`。
2. **只增不改 (Immutability)**：一旦某错误代码进入生产环境或被前端引用，其数值与含义终身不可更改；若业务下线，该错误代码应标记为废弃（`Deprecated`），不得直接改写其含义。
3. **零码即成功 (Zero Means Success)**：全平台统一规定 `errorCode == 0` 专属于成功状态（HTTP 2xx），任何错误状态下的 `errorCode` 必须大于 0。
4. **禁止魔数散落**：业务代码中严禁在多个地方手写硬编码数字，推荐在各模块异常类中集中常量化或通过专用 Exception 类封装。