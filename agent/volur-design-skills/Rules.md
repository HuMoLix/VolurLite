# Volur Lite 设计与编码规范

本文档定义 Volur Lite 项目的核心设计约束与编码规范。所有模块的新增代码编写、既有代码重构以及自动化代码生成均须严格遵循本规范。

---

## 1. 依赖注入与组件声明规范

### 1.1 基本原则
- **不可变性约束**：所有通过 Spring 容器注入的 Bean 或组件依赖属性，必须显式声明为 `private final`。
- **强制使用构造器注入**：类声明上必须添加 Lombok `@RequiredArgsConstructor` 注解，由编译器自动生成包含所有 `final` 字段的构造函数。
- **严禁字段注入**：禁止使用 `@Autowired` 进行成员变量直接注入。
- **消除样板构造器**：禁止手动编写用于依赖注入的构造函数；若类中已使用 `@RequiredArgsConstructor`，必须移除所有冗余的手写构造器。

### 1.2 [反模式]
```java
// 错误示例 1: 使用 @Autowired 进行字段注入
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserClient userClient;
}

// 错误示例 2: 已声明 @RequiredArgsConstructor 仍手动编写构造函数
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // 冗余手写构造函数，应予以移除
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
}
```

### 1.3 [推荐实践]
```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ApiResponseEntity<AuthenticateResponseVO> login(@Valid @RequestBody AuthenticateRequestDTO request) {
        AuthenticateResponseVO response = authService.authenticate(request);
        return ApiResponseEntity.ok("登录成功", response);
    }
}
```

---

## 2. 领域模型与实体对象规范 (Domain Model: DTO / VO / PO / BO / Entity)

### 2.1 目录结构与包组织约定
- **统一归属于 `domain` 顶层包**：
  各业务模块中的所有实体与领域模型（包括 DTO、VO、PO、BO、Entity、DO 等），**必须统一存放在模块的 `domain` 包路径下**。严禁在模块根路径下建立与 `domain` 平级的 `dto/`、`vo/`、`entity/` 等包。
- **标准包结构划分**：
  - `com.humolix.volur.<module>.domain.dto`：请求数据传输对象 (`*DTO`)
  - `com.humolix.volur.<module>.domain.vo`：响应视图数据对象 (`*VO`)
  - `com.humolix.volur.<module>.domain.entity` 或 `domain.po`：数据库持久化实体对象 (`*Entity` / `*PO` / `*DO`)
  - `com.humolix.volur.<module>.domain.bo`：业务逻辑处理对象 (`*BO`)
  - `com.humolix.volur.<module>.domain.enums`：领域内枚举类型（如适用）

#### 包结构 [反模式 vs 推荐实践]
```
❌ 错误包结构 (反模式: 散落平铺在模块根包下)
com.humolix.volur.auth
  ├── controller
  ├── dto               <-- 错误: 与 domain 平级
  │    └── AuthenticateRequestDTO.java
  ├── vo                <-- 错误: 与 domain 平级
  │    └── AuthenticateResponseVO.java
  ├── entity            <-- 错误: 与 domain 平级
  │    └── UserEntity.java
  └── service

✅ 正确包结构 (推荐实践: 统一收敛于 domain 包)
com.humolix.volur.auth
  ├── controller
  │    └── v1
  │         └── AuthController.java
  ├── domain            <-- 正确: 统一 domain 根包
  │    ├── dto
  │    │    └── AuthenticateRequestDTO.java
  │    ├── vo
  │    │    └── AuthenticateResponseVO.java
  │    ├── entity (或 po)
  │    │    └── UserEntity.java
  │    └── bo
  │         └── UserAuthBO.java
  └── service
```

---

### 2.2 请求传输对象 (DTO - Data Transfer Object)
- **核心定位**：用于承接外部 HTTP 请求入参，由 Spring MVC / Jackson 进行反序列化和参数校验绑定。
- **注解规范**：必须且仅需使用 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`。无需使用 `@Builder`。
- **严禁样板代码与冗余方法**：
  - 类声明 `@Data` 后，Lombok 会自动在编译期生成所有字段的标准 Getter / Setter 方法、`equals`、`hashCode`、`canEqual` 与 `toString` 方法。
  - **严禁在已有 `@Data` 的情况下显式编写 Getter / Setter 方法**（如 `getEmail()`, `setEmail(...)` 等），避免冗余样板代码污染类定义。
  - 严禁手动编写无参/全参构造函数。

#### DTO [反模式]
```java
// 错误示例: 已经声明了 @Data，仍然冗余手写 Getter / Setter 方法
package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCaptchaRequestDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String username;
    private String captchaId;
    private String captchaCode;

    // 冗余手写 Getter / Setter，应全部移除！
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCaptchaId() { return captchaId; }
    public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
    public String getCaptchaCode() { return captchaCode; }
    public void setCaptchaCode(String captchaCode) { this.captchaCode = captchaCode; }
}
```

#### DTO [推荐实践]
```java
package com.humolix.volur.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticateRequestDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaId;

    private String captchaCode;

    private Boolean rememberMe;
}
```

---

### 2.3 响应视图对象 (VO - View Object)
- **核心定位**：用于向客户端返回组装后的视图数据，通常由业务层（Service）或控制层（Controller）主动实例化与构建。
- **注解规范**：必须统一使用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`。
- **默认值保护**：若 VO 中字段包含初始化默认值（例如 `tokenType = "Bearer"`），必须显式添加 `@Builder.Default` 注解，避免使用 Builder 构造时默认值被覆盖丢失。
- **严禁样板代码**：严禁手动编写静态 `Builder` 内部类、Getter / Setter 方法及样板构造函数。

#### VO [反模式]
```java
// 错误示例: 手动编写大量 Getter/Setter、构造器以及手写 Builder 静态内部类
package com.humolix.volur.auth.domain.vo;

public class AuthenticateResponseVO {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    public AuthenticateResponseVO() {}

    public AuthenticateResponseVO(String accessToken, String refreshToken, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }

        public AuthenticateResponseVO build() {
            return new AuthenticateResponseVO(accessToken, refreshToken, tokenType);
        }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}
```

#### VO [推荐实践]
```java
package com.humolix.volur.auth.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticateResponseVO {

    private String accessToken;

    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";
}
```

---

### 2.4 持久化实体对象 (Entity / PO / DO)
- **核心定位**：与数据库表结构映射的持久化模型。
- **注解规范**：统一使用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`。
- **默认值保护**：包含初始值的属性同样必须显式声明 `@Builder.Default`。
- **存放路径**：位于 `com.humolix.volur.<module>.domain.entity` 或 `domain.po`。
- **数据库增量迁移同步铁律**：每次对持久化实体（Entity / PO）进行新增或字段变更（增删改字段、约束与索引变化）时，**必须同步修改/更新增量平滑升级脚本 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)**，并严格编写具备幂等性的探测性 DDL 与数据回填逻辑，确保生产环境无损升级。

---

### 2.5 业务逻辑对象 (BO - Business Object)
- **核心定位**：服务层（Service）内部流转或跨服务调用的业务实体对象，封装多表聚合数据或复杂运算中间态。
- **注解规范**：统一使用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`。
- **存放路径**：位于 `com.humolix.volur.<module>.domain.bo`。

---

## 3. 控制器层与 API 版本控制规范 (Controller & API Versioning)

### 3.1 目录与包结构版本化
- **显式版本子包**：Controller 类所在的包路径和文件目录必须显式包含版本号子目录，标准结构为 `com.humolix.volur.<module>.controller.v1`，对应物理目录路径为 `controller/v1/`。
- **默认版本策略**：在没有特殊需求或重大破坏性变更的情况下，默认统一使用 `v1`；仅在 API 发生非向后兼容的重大升级时方可引入 `v2`。

### 3.2 路由映射版本化
- **类级路由声明**：Controller 类的 `@RequestMapping` 必须显式声明 API 版本号前缀，遵循统一命名模式 `@RequestMapping("/api/v1/{module-name}")`。
- **请求验证规范**：请求参数实体使用 `@Valid @RequestBody SomeRequestDTO` 进行验证。

---

### 3.3 统一响应对象封装规范 (`ApiResponseEntity` & `ApiResponse`)

在 Controller 层向前端返回结果时，必须统一使用 `ApiResponseEntity<T>` 作为方法返回值，并通过 `ApiResponseEntity` 实体工厂方法进行组装。

#### 3.3.1 方法命名大小写规范 (Case Standards)
- **强制统一使用小写驼峰方法**：必须调用小写驼峰方法（如 `ApiResponseEntity.ok(...)`、`ApiResponseEntity.success(...)`、`ApiResponseEntity.created(...)`、`ApiResponseEntity.badRequest(...)` 等），严格遵循 Java 与 Spring 生态标准规范（对标 `ResponseEntity.ok()`）。
- **严禁使用大写方法**：大写静态方法（如 `OK()`, `SUCCESS()`, `CREATED()`, `BAD_REQUEST()` 等）已全部标记为 `@Deprecated`，新编写或重构的代码中严禁调用。

#### 3.3.2 `ok` 与 `success` 的语义与返回类型严格边界
- **`ApiResponseEntity.ok(data)` / `ApiResponseEntity.ok(message, data)`**：
  - **核心定位**：**专用于有具体业务数据负载（Payload / Data）**的成功响应场景。
  - **返回类型**：必须声明为 `ApiResponseEntity<T>`，其中泛型 `T` 对应具体的数据模型（如 `ApiResponseEntity<UserSummaryVO>`, `ApiResponseEntity<PageResponse<UserVO>>`, `ApiResponseEntity<Long>`, `ApiResponseEntity<Boolean>` 等）。
  - **典型应用**：
    - `GET` 查询接口（单条详情、列表、分页）；
    - `POST` / `PUT` / `PATCH` 产生具体业务返回值的场景（如登录返回 Token 凭据 `ApiResponseEntity<AuthenticateResponseVO>`、提交 Flag 返回判定结果 `ApiResponseEntity<Boolean>`、生成临时凭据 `ApiResponseEntity<String>`）。
- **`ApiResponseEntity.success(message)`**：
  - **核心定位**：**专用于无具体数据返回（纯命令式操作、状态确认或动作执行完成）**的成功响应场景。
  - **返回类型**：必须固定声明为 `ApiResponseEntity<Void>`，其内部 `data` 统一包装为 `null`。
  - **典型应用**：
    - `DELETE` 删除数据操作（如删除用户、删除题目、批量删除等）；
    - `PUT` / `PATCH` 状态切换或无需回传实体的属性更新（如切换账号禁用状态、更新配置等）；
    - 命令式/动作型 `POST` 操作（如发送验证码邮件、重置密码确认、退出登录、注销账号等）。
  - **严禁反模式**：严禁在无数据返回的方法中使用 `ApiResponseEntity.ok("...", null)` 或 `ApiResponseEntity.OK("...", null)`，凡是 `data` 为 `null` 且返回 `ApiResponseEntity<Void>` 的场景，必须且只能使用 `ApiResponseEntity.success("...")`！

#### 3.3.3 `created` 资源创建响应规范 (HTTP 201 Created)
- **`ApiResponseEntity.created(data)` / `ApiResponseEntity.created(message, data)`**：
  - **核心定位**：严格遵循 RESTful 架构语义（RFC 7231 / HTTP 201 Created），用于在服务端**成功持久化创建了新资源实体**的场景。
  - **HTTP 状态码**：HTTP 201 (Created)，响应体 `status = 201`。
  - **返回类型**：`ApiResponseEntity<T>`，通常携带新创建资源的主键 ID（`ApiResponseEntity<Long>`）或新建实体的完整视图对象（`ApiResponseEntity<AdminUserDetailsVO>`）。
  - **适用场景**：
    - `POST /api/v1/admin/users` 新增系统用户；
    - `POST /api/v1/admin/challenges/ctf` 新建赛题资源；
    - `POST /api/v1/announcements` 发布新公告等。
  - **不适用场景**：用户登录、提交判题 Flag、发送验证码等不生成独立持久化实体的 POST 操作严禁使用 `created`，应使用 `ok` 或 `success`。

#### 3.3.4 常用响应方法与 HTTP 动词映射速查表 (Cheat Sheet)
| 业务场景 | HTTP Method | 规范方法 | 返回类型 | 状态码 | Data 负载 | 典型示例 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **查询详情 / 列表 / 分页** | `GET` | `ApiResponseEntity.ok(msg, data)` | `ApiResponseEntity<T>` | 200 OK | ✅ 有实体/VO | `return ApiResponseEntity.ok("获取详情成功", userVO);` |
| **创建新资源实体** | `POST` | `ApiResponseEntity.created(msg, data)` | `ApiResponseEntity<T>` | 201 Created | ✅ 实体或主键 ID | `return ApiResponseEntity.created("创建题目成功", detailVO);` |
| **非创建实体的带返回操作** | `POST` / `PUT` | `ApiResponseEntity.ok(msg, data)` | `ApiResponseEntity<T>` | 200 OK | ✅ 结果载荷 | `return ApiResponseEntity.ok("登录成功", authResponseVO);` |
| **删除资源** | `DELETE` | `ApiResponseEntity.success(msg)` | `ApiResponseEntity<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("删除用户成功");` |
| **状态切换 / 纯更新动作** | `PATCH` / `PUT` | `ApiResponseEntity.success(msg)` | `ApiResponseEntity<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("状态切换成功");` |
| **命令式动作 (发邮件/重置/登出)** | `POST` | `ApiResponseEntity.success(msg)` | `ApiResponseEntity<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("验证码已发送至邮箱");` |

---

### 3.4 业务逻辑与策略核验归属规范 (Thin Controller 原则)
- **严禁在 Controller 层编写业务逻辑与策略判断**：
  - 严禁在 Controller 层注入并读取系统配置服务（如 `systemSettingService.getBoolean("ENABLE_CAPTCHA", false)`）。
  - 严禁在 Controller 层调用验证码核销服务（如 `captchaService.verifyCaptcha(...)`）、频控限制、权限逻辑判定或多服务编排。
- **强制收敛于 Service 业务层**：
  - 所有前置业务策略、系统开关读取、图形/邮件验证码核验、限频防刷与多服务编排逻辑必须全部下沉并封装在 Service 实现类内部。
  - Controller 只需将请求 DTO 对象直接透传给 Service（如 `emailService.sendVerificationEmail(request)`），保持 Controller 极简、轻薄且单一职责（Thin Controller）。
  - **优势与收益**：
    1. **安全闭环**：确保无论是通过 HTTP 路由调用，还是由内部其他 Service / 定时任务直接调用，策略均能 100% 生效，防止被内部调用绕过；
    2. **依赖解耦**：Controller 不再需要横向注入 `CaptchaService`、`SystemSettingService` 等依赖；
    3. **测试友好**：仅需对 Service 编写单元测试即可完整覆盖所有策略分支。

### 3.5 布尔二值分支响应规范 (Ternary Expression for Boolean Responses)
- **强制使用三元表达式**：当 Controller 方法接收 Service 层的布尔判定结果（例如 Flag 判题 `boolean isCorrect`、状态校验等）并需要根据 `true` / `false` 组装返回不同文案的 `ApiResponse<Boolean>` 时，**强制使用三元表达式（`return condition ? ApiResponseEntity.ok(...) : ApiResponseEntity.ok(...);`）进行扁平化组装与返回**。
- **严禁 `if-else` 块级分支嵌套**：严禁在 Controller 中为了返回不同的响应文案而编写冗长的 `if (condition) { return ... } else { return ... }` 块级结构，杜绝破坏 Controller 的扁平与紧凑性。
- **设计初衷**：
  1. **保持方法紧凑线性 (Flat & Lean)**：消除冗余样板分支结构与花括号嵌套，使控制层代码更加紧凑、直观且声明式；
  2. **彻底收敛业务逻辑**：复杂的业务判定已在 Service 内部完成，Controller 仅承担视图映射职责。

---

### 3.6 [反模式]
```java
// 错误示例 1: 包路径或目录未包含版本划分 (如放在 controller/AuthController.java)
package com.humolix.volur.auth.controller;

// 错误示例 2: 路由缺少版本号
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // ...
}

// 错误示例 3: Controller 层承载业务逻辑与配置核验
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final SystemSettingService systemSettingService;

    @PostMapping("/captcha/register")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        // 错误: 业务配置与图形验证码校验写在 Controller 中！
        if (systemSettingService.getBoolean("ENABLE_CAPTCHA", false)) {
            if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "图形验证码错误或已过期");
            }
        }
        emailService.sendVerificationEmail(request.getEmail(), request.getUsername());
        // 错误: 使用了大写 OK 且传入 null，应使用 ApiResponseEntity.success(...)
        return ApiResponseEntity.OK("验证码发送成功", null);
    }
}

// 错误示例 4: Controller 接收布尔判定结果后使用冗长繁琐的 if-else 块级嵌套
@PostMapping("/challenges/{challengeId}/submit")
public ApiResponse<Boolean> submitFlag(
        @PathVariable Long competitionId,
        @PathVariable Long challengeId,
        @CurrentUserId Long userId,
        @Valid @RequestBody CtfFlagSubmitRequestDTO request,
        HttpServletRequest httpRequest) {
    String ip = httpRequest.getRemoteAddr();
    boolean isCorrect = ctfStageService.submitFlag(competitionId, challengeId, userId, request, ip);
    // 错误: 简单的布尔二值响应使用了繁琐的 if-else 分支嵌套
    if (isCorrect) {
        return ApiResponseEntity.ok("恭喜！Flag 正确", true);
    } else {
        return ApiResponseEntity.ok("Flag 错误，请再接再厉", false);
    }
}
```

### 3.7 [推荐实践]
```java
// 正确示例 1: Controller 保持极简，规范使用小写方法及 ApiResponseEntity.success / created / ok
package com.humolix.volur.captcha.controller.v1;

import com.humolix.volur.captcha.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.captcha.service.EmailService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    // 动作型接口：返回 ApiResponse<Void>，使用 ApiResponseEntity.success
    @PostMapping("/captcha/register")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody EmailCaptchaRequestDTO request) {
        emailService.sendVerificationEmail(request);
        return ApiResponseEntity.success("验证码发送成功");
    }
}

// 正确示例 2: 使用三元表达式扁平化组装布尔二值响应
@PostMapping("/challenges/{challengeId}/submit")
public ApiResponse<Boolean> submitFlag(
        @PathVariable Long competitionId,
        @PathVariable Long challengeId,
        @CurrentUserId Long userId,
        @Valid @RequestBody CtfFlagSubmitRequestDTO request,
        HttpServletRequest httpRequest) {
    String ip = httpRequest.getRemoteAddr();
    boolean isCorrect = ctfStageService.submitFlag(competitionId, challengeId, userId, request, ip);
    return isCorrect
            ? ApiResponseEntity.ok("恭喜！Flag 正确", true)
            : ApiResponseEntity.ok("Flag 错误，请再接再厉", false);
}
```

---

## 4. 服务层方法签名与返回值规范 (Service Method Signatures & Return Types)

### 4.1 基本原则与设计理念

在领域服务接口（`*Service`）与实现类（`*ServiceImpl`）的方法定义中，必须遵循语义明确、空指针安全（NPE Safe）与异常驱动的现代化设计理念：

1. **执行类/命令式业务方法 (Execution / Command Methods)**：
   - **核心定位**：执行具体业务动作的方法，如发送邮件（`sendVerificationEmail`）、修改密码（`changePassword`）、重置密码（`resetPassword`）、状态变更（`updateStatus`）、注销账号（`deleteAccount`）等。
   - **强制返回 `void`**：遵循 Spring 业务架构“失败即抛异常，成功即正常走完流程”的规范。在发生校验不通过、配置缺失或业务冲突时，统一直接抛出 `BusinessException` 或对应的派生业务异常，由全局异常处理器（`GlobalExceptionHandler`）拦截处理。
   - **严禁返回虚假/冗余的布尔值**：严禁编写永远返回 `true` 的方法（如 `boolean sendEmail(...) { ... return true; }`），此类返回值不仅无实际判断意义，还会诱导调用方编写无用的 `if (result)` 判断样板代码。

2. **判定类/核验类业务方法 (Predicate / Query Methods)**：
   - **核心定位**：执行校验核销、存在性探测或布尔状态查询的方法，如验证码比对（`verifyCode`、`verifyCaptcha`）、存在性检查（`existsByUsername`）、状态断言（`isAccountNonLocked`）等。
   - **强制使用基本类型 `boolean`**：
     - 方法返回值必须且只能使用基本数据类型 `boolean`；
     - 方法内部必填逻辑开关形参必须使用 `boolean`（如 `boolean skipExistenceCheck`）；
   - **严禁在 Service 方法中使用包装类型 `Boolean` 作为返回值**：
     - `boolean` 具有确定性的二值状态（`true` / `false`）；
     - `Boolean` 是引用对象，存在三态（`true` / `false` / `null`）。若 Service 方法返回 `Boolean`，调用方在编写 `if (service.verifyCode(...))` 时 JVM 会进行底层自动拆箱（Auto-unboxing）。一旦由于逻辑疏漏返回 `null`，调用方会立即抛出致命的 `NullPointerException`（NPE）。

3. **包装类型 `Boolean` 的唯一合法使用场景 (Legitimate Uses of `Boolean`)**：
   - ① **领域模型与持久化实体字段**：如 DTO / VO / Entity / PO 的属性，映射数据库允许为 `NULL` 的列，或者在 DTO 中配合 `@NotNull` 区分前端“未传参（null）”与“传了 false”；
   - ② **泛型参数与容器约束**：如 `ApiResponse<Boolean>`、`Optional<Boolean>`、`CompletableFuture<Boolean>`、`List<Boolean>`（Java 泛型机制不支持基本数据类型）。

### 4.2 [反模式]
```java
// 错误示例 1: 执行类操作返回无意义的 true 布尔值
public interface EmailService {
    // 错误: 失败时抛异常，成功时永远只返回 true，此处的 Boolean/boolean 返回值属于冗余设计
    Boolean sendVerificationEmail(String email, String username);
}

// 错误示例 2: 校验类方法返回包装类型 Boolean，存在自动拆箱 NPE 风险
public interface CaptchaService {
    // 错误: 包装类型 Boolean 存在三态(true/false/null)，调用方 if(captchaService.verify(...)) 易引发 NPE
    Boolean verifyCaptcha(String captchaId, String code);
}
```

### 4.3 [推荐实践]
```java
// 正确示例: 业务动作返回 void，状态校验返回 primitive boolean
package com.humolix.volur.captcha.service;

import com.humolix.volur.captcha.domain.dto.EmailCaptchaRequestDTO;
import com.humolix.volur.captcha.domain.dto.VerifyChangePasswordCodeRequestDTO;

public interface EmailService {

    // 动作执行类方法统一返回 void
    void sendVerificationEmail(EmailCaptchaRequestDTO request);

    void sendVerificationEmail(String email, String username);

    void sendVerificationEmail(String email, String username, boolean skipExistenceCheck);

    void sendChangePasswordEmail(EmailCaptchaRequestDTO request);

    void sendResetPasswordEmail(String email, String username);

    // 校验判定类方法统一返回基本类型 boolean
    boolean verifyCode(String email, String code);

    String verifyChangePasswordCode(VerifyChangePasswordCodeRequestDTO request);
}
```

---

## 5. Redis 缓存键与常量命名规范 (Redis Key & Constant Naming)

### 5.1 命名格式与结构定义

在使用 Redis 缓存或操作键值对时，常量名与 Key 值必须遵循统一的多级命名空间规范：

1. **Java 常量名称规范**：
   - 必须以固定前缀 `VREDIS_` 开头，后接所属模块名、功能名及后缀（全部大写，以下划线 `_` 连接）。
   - **格式模板**：`VREDIS_<MODULE>_<FEATURE>_KEY_PREFIX` 或 `VREDIS_<MODULE>_<FEATURE>_<NAME>`
   - **组成说明**：
     - `VREDIS_`：Volur Redis 固定全局前缀标识。
     - `<MODULE>_`：所属业务模块名（大写，如 `AUTH_`, `USER_`, `CHALLENGE_`, `CONTAINER_`）。
     - `<FEATURE>_`：具体功能标识（大写，如 `CAPTCHA_`, `TOKEN_BLACKLIST_`, `EMAIL_CODE_`, `USER_STATUS_`）。
     - `KEY_PREFIX`：表示该常量为 Key 前缀。

2. **Redis Key 前缀值规范**：
   - 统一采用小写、多级冒号 `:` 分隔，且**必须以冒号 `:` 结尾**。
   - **格式模板**：`"vredis:<module>:<feature>:"`
   - **组成说明**：
     - `vredis:`：项目统一根命名空间。
     - `<module>:`：所属业务模块（全小写，如 `auth:`, `user:`, `challenge:`）。
     - `<feature>:`：具体功能名称（全小写，多词以下划线或冒号连接，如 `captcha:`, `token:blacklist:`, `email_code:`）。

3. **常用常量命名示例对照表**：
   | 业务场景 | Java 常量名 | 常量值 (Key 前缀) | 组装后完整 Key 示例 |
   | :--- | :--- | :--- | :--- |
   | 认证模块图形验证码 | `VREDIS_AUTH_CAPTCHA_KEY_PREFIX` | `"vredis:auth:captcha:"` | `vredis:auth:captcha:9a8b7c` |
   | 认证模块邮箱验证码 | `VREDIS_AUTH_EMAIL_CODE_KEY_PREFIX` | `"vredis:auth:email_code:"` | `vredis:auth:email_code:user@example.com` |
   | 认证模块 Token 黑名单 | `VREDIS_AUTH_TOKEN_BLACKLIST_KEY_PREFIX` | `"vredis:auth:token:blacklist:"` | `vredis:auth:token:blacklist:eyJhbGci...` |
   | 用户强制下线时间戳 | `VREDIS_AUTH_USER_LOGOUT_KEY_PREFIX` | `"vredis:auth:user:logout:"` | `vredis:auth:user:logout:10001` |
   | 题目动态容器状态 | `VREDIS_CHALLENGE_CONTAINER_KEY_PREFIX` | `"vredis:challenge:container:"` | `vredis:challenge:container:5001` |

---

### 5.2 缓存过期时间与操作参数规范 (TTL & Duration)
- **严禁使用 `TimeUnit` 设置过期时间**：
  - Spring Data Redis 自 4.1 起已将 `set(K key, V value, long timeout, TimeUnit unit)` 及以 `(long, TimeUnit)` 为参数的过期方法标记为已弃用（`@Deprecated`）。
  - 使用弃用重载会触发编译器警告：`'set(@NonNull K, @NonNull V, long, @NonNull TimeUnit)' 自版本 4.1 起已弃用`。
- **强制使用 `java.time.Duration`**：
  - 所有涉及设置缓存超时时间、过期时间的方法（例如 `redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10))`、`redisTemplate.expire(key, Duration.ofMinutes(30))` 等），必须统一使用 `java.time.Duration` 表达时间跨度。

---

### 5.3 [反模式]
```java
// 错误示例 1: 常量名缺少 VREDIS_ 前缀与模块标识，常量值缺少统一项目前缀
public class CaptchaServiceImpl {
    // 错误: 命名随意，极易与其它服务 Key 冲突
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final String CODE_KEY = "auth_code_";
}

// 错误示例 2: 常量名虽有模块但缺少统一 VREDIS_ 前缀，值未以冒号结尾
public class TokenServiceImpl {
    private static final String AUTH_TOKEN_KEY = "auth:token"; // 缺少结尾冒号
}

// 错误示例 3: 使用已弃用的 TimeUnit 设置缓存过期时间
public class EmailServiceImpl {
    public void sendVerificationEmail(String email, String username) {
        String code = RandomUtil.randomNumbers(6);
        String key = "vredis:auth:email_code:" + email;
        // 错误: 使用了 (long, TimeUnit) 弃用方法
        redisTemplate.opsForValue().set(key, code, 10, TimeUnit.MINUTES);
    }
}
```

### 5.4 [推荐实践]
```java
package com.humolix.volur.auth.service.impl;

import com.humolix.volur.auth.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    // 正确规范: 常量名以 VREDIS_ + 模块_ + 功能_ + KEY_PREFIX 命名，值为 "vredis:模块:功能:"
    private static final String VREDIS_AUTH_CAPTCHA_KEY_PREFIX = "vredis:auth:captcha:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            return false;
        }
        // 组装最终 Redis Key: vredis:auth:captcha:{captchaId}
        String key = VREDIS_AUTH_CAPTCHA_KEY_PREFIX + captchaId;
        String realCode = redisTemplate.opsForValue().get(key);
        if (realCode == null) {
            return false;
        }
        redisTemplate.delete(key);
        return realCode.equalsIgnoreCase(captchaCode.trim());
    }

    public void cacheCaptcha(String captchaId, String captchaCode) {
        String key = VREDIS_AUTH_CAPTCHA_KEY_PREFIX + captchaId;
        // 正确规范: 统一使用 java.time.Duration 指定过期时间
        redisTemplate.opsForValue().set(key, captchaCode, Duration.ofMinutes(5));
    }
}
```

---

## 6. 对象映射与模型转换规范 (Object Mapping: MapStruct)

### 6.1 基本原则
- **严禁手写转换方法**：禁止在 Service、Controller 或工具类中手写私有 `toVO(...)`、`toEntity(...)`、`toDTO(...)` 或冗余的 Builder/Setter 逐字段拷贝代码。
- **严禁使用反射浅拷贝**：严禁使用 `BeanUtils.copyProperties(...)` 等性能低下且类型不安全的反射拷贝工具。
- **统一使用 MapStruct**：所有跨层对象映射（Entity ↔ VO、DTO ↔ Entity、Entity ↔ BO 等），必须通过 MapStruct 声明式的接口进行类型安全的高性能编译期转换。

### 6.2 目录结构与命名约定
- **Mapper 接口存放路径**：统一存放在各模块的 `mapper` 包路径下，对应 `com.humolix.volur.<module>.mapper`。
- **命名规范**：遵循 `<Module>Mapper` 或 `<Entity/Feature>Mapper`，例如 `SystemMapper`、`UserMapper`、`ChallengeMapper`。
- **注解配置规范**：
  - 接口类上必须统一声明：`@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`。
  - `componentModel = "spring"`：使 MapStruct 自动生成 Spring Bean 组件，便于由 Spring 容器托管并注入。
  - `unmappedTargetPolicy = ReportingPolicy.IGNORE`：忽略未显式映射的字段，避免不必要的编译告警。

### 6.3 依赖注入规范
- **遵循构造器注入**：Service 类中依赖 Mapper 接口时，必须声明为 `private final SomeMapper someMapper;`，由 Lombok `@RequiredArgsConstructor` 自动完成构造注入。

### 6.4 [反模式]
```java
// 错误示例 1: 在 Service 类内手动编写私有 toVO 转换方法
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    public List<SystemSettingVO> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::toVO) // 错误: 手写转换样板代码
                .collect(Collectors.toList());
    }

    // 严禁在 Service 中编写此类私有转换方法！
    private SystemSettingVO toVO(SystemSettingEntity entity) {
        return SystemSettingVO.builder()
                .group(entity.getSettingGroup())
                .key(entity.getSettingKey())
                .value(entity.getSettingValue())
                .valueType(entity.getValueType())
                .isPublic(entity.getIsPublic())
                .description(entity.getDescription())
                .build();
    }
}

// 错误示例 2: 使用 BeanUtils 反射拷贝
public UserVO getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo); // 错误: 性能低下且重构不安全
    return vo;
}
```

### 6.5 [推荐实践]
```java
// 1. 声明 MapStruct 接口: com.humolix.volur.system.mapper.SystemMapper
package com.humolix.volur.system.mapper;

import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemMapper {

    @Mapping(source = "settingGroup", target = "group")
    @Mapping(source = "settingKey", target = "key")
    @Mapping(source = "settingValue", target = "value")
    SystemSettingVO toSettingVO(SystemSettingEntity entity);

    List<SystemSettingVO> toSettingVOList(List<SystemSettingEntity> entities);
}

// 2. 在 Service 中注入并优雅调用
package com.humolix.volur.system.service.impl;

import com.humolix.volur.system.domain.vo.SystemSettingVO;
import com.humolix.volur.system.mapper.SystemMapper;
import com.humolix.volur.system.repository.SystemSettingRepository;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final SystemMapper systemMapper;

    @Override
    public List<SystemSettingVO> getAllSettings() {
        return systemMapper.toSettingVOList(systemSettingRepository.findAll());
    }
}
```

---

## 7. 注解排序与排版约定

1. **类级注解排列顺序**：
   - 框架核心组件注解（如 `@RestController`, `@Service`, `@Configuration`）优先放置于最顶部。
   - 路由与安全配置注解（如 `@RequestMapping`, `@PreAuthorize`）紧随其后。
   - Lombok 注解（如 `@RequiredArgsConstructor`, `@Slf4j`, `@Data`, `@Builder`）放置于类声明最接近位置。
2. **模型分层隔离与包路径约定**：
   - 客户端请求入参：命名为 `*DTO`，存放于 `domain.dto`（例如 `com.humolix.volur.auth.domain.dto.RegisterRequestDTO`）。
   - 客户端响应视图：命名为 `*VO`，存放于 `domain.vo`（例如 `com.humolix.volur.auth.domain.vo.AuthenticateResponseVO`）。
   - 数据库持久化实体：命名为 `*Entity` 或 `*PO` / `*DO`，存放于 `domain.entity` 或 `domain.po`。
   - 业务逻辑流转实体：命名为 `*BO`，存放于 `domain.bo`。

---

## 8. 分页响应模型与业务场景选型规范 (Pagination Models & Selection Guidelines)

### 8.1 基本原则与轻量化约束
- **严禁直接返回 Spring Data 原生 `Page<T>`**：禁止在 Controller 接口或 Service 层对外暴露 Spring Data 的原生 `Page<T>` 或 `ApiResponse<Page<T>>`，避免向前端输出 `pageable`、`sort`、`first`、`last`、`numberOfElements`、`empty` 等底层冗余序列化字段。
- **强制使用通用标准化 Record**：所有列表分页响应统一使用 `com.humolix.volur.framework.common.api` 下的标准 Record 模型（`PageResponse<T>`、`CursorResponse<T>`、`SliceResponse<T>`），并由 `ApiResponseEntity.ok(msg, response)` 封装返回。
- **业务驱动的前置逻辑分析**：在开发任何分页接口前，**必须先对该业务场景的交互特征、数据规模、并发压力与查询性能进行深度逻辑分析**，严禁盲目套用传统分页，根据分析结果精准选择合适的分页模型。

---

### 8.2 三大分页模型特性与适用边界

#### 1. 标准传统分页模型 (`PageResponse<T>`)
- **模型结构**：`List<T> list`, `long total`, `int totalPages`, `int currentPage`
- **底层机制**：基于 Spring Data `Page<T>`，底层会执行一次 `COUNT(*)` 聚合查询以计算总量，再加上 `LIMIT offset, size` 数据查询。
- **适用场景**：
  - **管理端后台数据表格**（如用户管理、赛题管理、审核列表等），管理员需要明确掌握总记录数、支持任意跳页、展示完整页码条（如 1, 2, 3... 10）；
  - 数据总量有限或适中（非百万级高频写入），且前端业务强依赖总量展示与精准跳页的场景。
- **局限性**：在大数据量或复杂关联条件下，`COUNT(*)` 性能开销巨大；深分页（如 `page=10000`）会导致数据库扫描性能急剧恶化。

#### 2. 游标 / 流式无限滚动分页模型 (`CursorResponse<T>`)
- **模型结构**：`List<T> list`, `String nextPageToken`, `boolean hasMore`
- **底层机制**：基于主键 ID 或唯一时间戳游标（如 `WHERE id < :cursor ORDER BY id DESC LIMIT :size + 1`），利用索引定位，免除 `COUNT(*)` 与 `OFFSET` 扫描。
- **适用场景**：
  - **流式 / 瀑布流交互与信息流**（如站内信通知列表、实时动态墙、操作日志流、评论区无限滚动等）；
  - **高频写入与海量数据流**，有效避免传统分页在数据频繁插入/删除时产生的“数据重复/漂移”问题，且深翻页性能始终保持 $O(1)$ 级别。

#### 3. 无总数切片分页模型 (`SliceResponse<T>`)
- **模型结构**：`List<T> list`, `boolean hasNext`, `boolean hasPrevious`, `int currentPage`, `int pageSize`
- **底层机制**：基于 Spring Data `Slice<T>`，仅执行 `LIMIT offset, size + 1` 探测是否存在下一页，**完全不执行昂贵的 `COUNT(*)` 查询**。
- **适用场景**：
  - **前台海量数据列表浏览**（如公开题库广场、大型竞赛归档浏览等），用户通常只进行“下一页 / 上一页 / 加载更多”的顺序翻页，无需知道精确的总数；
  - 高并发读取场景，为了彻底剥离 `COUNT(*)` 对数据库带来的性能负担。

---

### 8.3 业务选型决策流程与指导矩阵

```mermaid
flowchart TD
    Start([开始: 业务分页需求]) --> Q1{前端是否需要展示<br/>精确总数与指定跳页?}
    Q1 -- 是 --> Q2{数据规模是否巨大<br/>或写入极度高频?}
    Q2 -- 否 (中小型数据/后台管理) --> UsePage[✅ 选用 PageResponse<T><br/>传统标准分页]
    Q2 -- 是 (需考虑性能降级) --> CheckOptim[评估是否可通过缓存/近似总数优化<br/>或与产品沟通降级]
    
    Q1 -- 否 --> Q3{交互形态是否为<br/>无限滚动/信息流/动态流?}
    Q3 -- 是 (信息流/通知/日志) --> UseCursor[✅ 选用 CursorResponse<T><br/>游标流式分页 (推荐)]
    Q3 -- 否 (上一页/下一页顺序翻页) --> UseSlice[✅ 选用 SliceResponse<T><br/>无总数切片分页]
```

#### 选型速查矩阵 (Cheat Sheet)

| 业务场景 | 典型功能举例 | 推荐分页模型 | 返回值类型 | 核心选型考量 |
| :--- | :--- | :--- | :--- | :--- |
| **管理后台表格** | 管理员用户列表、题目管理列表、队伍审核列表 | `PageResponse<T>` | `ApiResponse<PageResponse<T>>` | 需要明确展示总条数、总页数与精确跳页导航。 |
| **前台常规展示** | 竞赛列表、公开榜单（数据量适中） | `PageResponse<T>` | `ApiResponse<PageResponse<T>>` | 页面需展示总竞赛场次与标准分页器。 |
| **高并发海量题库** | CTF 题库广场、海量练习题浏览 | `SliceResponse<T>` | `ApiResponse<SliceResponse<T>>` | 免除大表 `COUNT(*)` 性能开销，仅提供上一页/下一页。 |
| **动态与通知流** | 站内信通知列表、比赛动态事件流、操作日志 | `CursorResponse<T>` | `ApiResponse<CursorResponse<T>>` | 移动端/信息流体验，抗高频写入，彻底避免翻页数据漂移。 |

---

### 8.4 分页请求入参与动态多条件查询规范 (Page Query DTO & Dynamic Multi-Condition Queries)

1. **统一继承基础基类 `PageRequestDTO`**：
   - 基础基类路径：`com.humolix.volur.framework.common.dto.PageRequestDTO`。
   - 所有分页查询请求入参对象（`*PageRequestDTO`）必须统一存放在所属业务模块的 `domain.dto` 包下，并显式继承 `PageRequestDTO`。
   - 类注解统一使用 Lombok `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@EqualsAndHashCode(callSuper = true)`。
   - **严禁反模式**：严禁在 Controller 方法签名中平铺声明多个零散的 `@RequestParam`（如 `int page, int size, String keyword, String role, Boolean status...`），导致方法签名冗长、脆弱且难以维护。

2. **Service 层动态多条件 Specification 查询最佳实践**：
   - **空请求防御**：`SomePageRequestDTO request = pageRequest != null ? pageRequest : new SomePageRequestDTO();`
   - **Pageable 快速构建与安全白名单**：调用 `request.toPageable(TargetEntity.class, "defaultSortField")`，由框架底层 `PageSortUtil` 自动对请求排序字段执行反射白名单校验，防止 SQL 注入风险。
   - **模糊查询安全防御与大小写不敏感**：
     - 对 `keyword` 必须执行 `.trim()` 清理前后空格；
     - 强制限制关键字最大长度（如 `cleanKeyword.length() > 50` 截断），防止超长文本导致慢查询或拒绝服务；
     - 统一使用 `cb.lower()` 配合 `cb.like()` 进行大小写不敏感匹配；
     - 跨关联表模糊搜索时采用 `root.join("...", JoinType.LEFT)` 并在 Predicate 中使用 `cb.or(...)` 组合。
   - **动态条件聚合**：统一使用 `return cb.and(predicates.toArray(new Predicate[0]));` 组装返回。
   - **只读事务保护**：查询类 Service 实现类或方法上统一声明 `@Transactional(readOnly = true)`。
   - **实体到 VO 投影转换**：使用 `entityPage.map(mapper::toPageResponseVO)` 配合 MapStruct 高性能映射。

3. **Controller 层标准极简写法 (Thin Controller)**：
   - Controller 方法直接声明强类型对象入参（Spring MVC 自动完成 Query Params 绑定）：
     ```java
     @GetMapping
     @Operation(summary = "分页获取数据列表")
     public ApiResponse<PageResponse<SomeVO>> pageSomething(SomePageRequestDTO pageRequest) {
         Page<SomeVO> pageResponse = someService.pageSomething(pageRequest);
         return ApiResponseEntity.ok("获取列表成功", new PageResponse<>(pageResponse));
     }
     ```

---

### 8.5 [反模式]
```java
// 错误示例 1: 直接对外暴露 Spring Data 原生 Page，导致前端接收大量冗余内部字段
@GetMapping
public ApiResponse<Page<CompetitionSummaryVO>> listCompetitions(...) {
    // 错误: 直接返回 Page<T> 反模式
    return ApiResponseEntity.ok("获取成功", competitionService.listCompetitions(page, size, keyword));
}

// 错误示例 2: Controller 方法签名平铺大量离散 @RequestParam，未使用 DTO 封装
@GetMapping
public ApiResponse<PageResponse<AdminUserDetailsVO>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean status,
        @RequestParam(required = false) Instant startTime,
        @RequestParam(required = false) Instant endTime,
        @RequestParam(required = false, defaultValue = "id") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
    // 错误: 离散参数反模式，难以扩展且破坏 Thin Controller 规范
}

// 错误示例 3: 不分析业务特征，在流式通知列表中盲目使用传统 Page 分页，导致深翻页卡顿与数据漂移
@GetMapping("/notifications")
public ApiResponse<PageResponse<NotificationVO>> listNotifications(...) {
    // 错误: 站内信通知流更适合 CursorResponse 游标分页
}
```

### 8.6 [推荐实践]
```java
// 正确示例 1: 1. 继承通用 PageRequestDTO 定义业务请求对象
package com.humolix.volur.waf.domain.dto;

import com.humolix.volur.framework.common.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WafInterfacePageRequestDTO extends PageRequestDTO {
    private String keyword;
    private String serviceName;
    private Boolean wafProtected;
}

// 正确示例 2: Service 层优雅实现动态多条件查询
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WafInterfaceServiceImpl implements WafInterfaceService {

    private final WafApiInterfaceRepository wafApiInterfaceRepository;
    private final WafInterfaceMapper wafInterfaceMapper;

    @Override
    public Page<WafInterfacePageResponseVO> pageWafInterfaces(WafInterfacePageRequestDTO pageRequest) {
        WafInterfacePageRequestDTO request = pageRequest != null ? pageRequest : new WafInterfacePageRequestDTO();
        Pageable pageable = request.toPageable(WafApiInterface.class, "createdAt");

        Specification<WafApiInterface> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getWafProtected() != null) {
                predicates.add(cb.equal(root.get("wafProtected"), request.getWafProtected()));
            }

            if (StringUtils.hasText(request.getServiceName())) {
                predicates.add(cb.equal(root.get("serviceName"), request.getServiceName().trim()));
            }

            if (StringUtils.hasText(request.getKeyword())) {
                String cleanKeyword = request.getKeyword().trim();
                if (cleanKeyword.length() > 50) {
                    cleanKeyword = cleanKeyword.substring(0, 50);
                }
                String pattern = "%" + cleanKeyword.toLowerCase() + "%";
                Predicate serviceNameLike = cb.like(cb.lower(root.get("serviceName")), pattern);
                Predicate pathLike = cb.like(cb.lower(root.get("path")), pattern);
                Predicate summaryLike = cb.like(cb.lower(root.get("summary")), pattern);
                predicates.add(cb.or(serviceNameLike, pathLike, summaryLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<WafApiInterface> entityPage = wafApiInterfaceRepository.findAll(spec, pageable);
        return entityPage.map(wafInterfaceMapper::toPageResponseVO);
    }
}

// 正确示例 3: Controller 层极简接收与标准 Record 响应封装
@RestController
@RequestMapping("/api/v1/waf/interfaces")
@RequiredArgsConstructor
public class WafInterfaceController {

    private final WafInterfaceService interfaceService;

    @GetMapping
    @Operation(summary = "获取全量或组合筛选的接口列表")
    public ApiResponse<PageResponse<WafInterfacePageResponseVO>> pageWafInterfaces(WafInterfacePageRequestDTO pageRequest) {
        Page<WafInterfacePageResponseVO> pageResponse = interfaceService.pageWafInterfaces(pageRequest);
        return ApiResponseEntity.ok("获取接口列表成功", new PageResponse<>(pageResponse));
    }
}

// 正确示例 4: 流式通知列表游标分页 (CursorResponse)
@GetMapping("/feed")
public ApiResponse<CursorResponse<NotificationVO>> getNotificationFeed(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int limit) {
    return ApiResponseEntity.ok("获取通知流成功", notificationService.getFeed(cursor, limit));
}
```

---

## 16. 系统动态配置项命名规范 (System Setting Key Standard)

### 16.1 基本原则与命名约束
- **强制层级点分小写命名**：全系统所有动态系统设置项的 Key 统一采用 `system.<group>.<field_name>` 格式（全小写、英文句点层级分隔、下划线连接属性单词）。
- **统一常量集中管理**：所有 Key 必须在 `com.humolix.volur.system.constant.SystemSettingKeys` 常量类中显式定义，禁止在业务逻辑中散落硬编码魔法字符串。
- **严禁双写与别名兼容**：彻底杜绝扁平大写下划线命名（如 `MAINTENANCE_MODE`、`ENABLE_CAPTCHA`、`REGISTER_ENABLED`）及后端 `KEY_ALIASES` 别名映射与双写机制，保证数据库记录、多级缓存、前后端交互的单一真实数据源（Single Source of Truth）。

### 16.2 标准键名分组参考表
| 配置分组 | 标准配置键名 (`Key`) | 类型与含义 |
| :--- | :--- | :--- |
| **基础设置 (`general`)** | `system.general.platform_name` | 平台全称 (String) |
| | `system.general.platform_desc` | 平台描述与标语 (String) |
| | `system.general.icp_number` | ICP 备案号 (String) |
| | `system.general.default_locale` | 默认语言 (如 `zh_CN`) |
| | `system.general.frontend_url` | 前端访问基准 URL |
| **鉴权与注册 (`auth`)** | `system.auth.registration_enabled` | 是否开放注册 (Boolean) |
| | `system.auth.require_email_verification`| 注册是否强制邮箱验证 (Boolean) |
| | `system.auth.max_login_attempts` | 密码错误最大重试次数 (Integer) |
| | `system.auth.email_whitelist_suffixes`| 允许注册的邮箱域名白名单后缀 (String) |
| **人机验证 (`captcha`)** | `system.captcha.enabled` | 是否启用人机验证 (Boolean) |
| | `system.captcha.tolerance_px` | 滑块验证容差像素 (Integer) |
| | `system.captcha.ticket_ttl_seconds` | 验证凭证有效期秒数 (Integer) |
| | `system.captcha.max_attempts` | 最大验证重试次数 (Integer) |
| | `system.captcha.force_reset_captcha` | 重置密码是否强制人机验证 (Boolean) |
| | `system.captcha.turnstile_site_key` | Cloudflare Turnstile Site Key |
| | `system.captcha.turnstile_secret_key` | Cloudflare Turnstile Secret Key |
| **邮件服务 (`mail`)** | `system.mail.smtp_host` | SMTP 主机地址 |
| | `system.mail.smtp_port` | SMTP 端口号 (Integer) |
| | `system.mail.smtp_user` | SMTP 用户名 |
| | `system.mail.smtp_pass` | SMTP 密码 / 授权码 |
| | `system.mail.smtp_ssl` | 是否启用 SSL (Boolean) |
| | `system.mail.sender_name` | 发件人名称与邮箱 |
| **容器基础设施 (`container`)** | `system.container.port_range_start` | 靶机对外暴露的动态端口池起始端口 |
| | `system.container.port_range_end` | 靶机对外暴露的动态端口池结束端口 |
| **运维与安全 (`maintenance`/`security`)** | `system.maintenance.enabled` | 全站维护模式开关 (Boolean) |
| | `system.maintenance.notice_message` | 维护公告通知文本 (String) |
| | `system.security.rate_limit_window` | 限流时间窗口秒数 (Integer) |
| | `system.security.rate_limit_max_requests` | 窗口内最大请求次数 (Integer) |

> **配置职责边界铁律**：
> - **全局系统配置（System Settings）**：仅配置全站共用的基础设施参数（如宿主机端口池范围 `20000 - 30000`、邮件服务器、滑块验证码参数等）；
> - **竞赛业务规则（Competition Rules）**：单容器存活时长（如 120 分钟）、单用户最大并发靶机数、以及**动态 Flag 生成模板**（如 `flag{{{UUID}}}`、`ciscn{{{RANDOM:16}}}`）必须下放到各个竞赛/题目挂载配置中独立定义，严禁在系统全局设置中写死。

### 16.3 [反模式 vs 推荐实践]
```java
// ❌ 错误示例: 使用旧版大写扁平键名、在全局系统设置中固化单赛事规则
boolean isMaintenance = systemSettingService.getBoolean("MAINTENANCE_MODE", false);
String notice = systemSettingService.getSettingValue("MAINTENANCE_NOTICE", "...");
String flagPrefix = systemSettingService.getSettingValue("FLAG_PREFIX", "volur{");

// ✅ 正确示例: 全局配置统一使用 SystemSettingKeys 常量；赛事规则（Flag 模板、靶机生命周期）由 Competition/Challenge 实体承载
boolean isMaintenance = systemSettingService.getBoolean(SystemSettingKeys.MAINTENANCE_ENABLED, false);
String notice = systemSettingService.getSettingValue(SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE, "系统正在全站维护升级中，暂不可用，请稍后访问。");
int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
```

---

## 17. 全链路快速失败与严禁隐式 Mock 铁律 (Fail-Fast & Zero-Mock Principle)

### 17.1 基本原则
- **没配置就是没配置，严禁私自 Mock 假数据**：对于未配置的基础设施或关键业务依赖（如 SMTP 邮件主机/账号未配置、人机验证密钥未配置、短信服务未配置等），必须直接抛出明确异常快速失败（如 `throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务未配置，无法执行此操作")`）。
- **严禁伪造执行成功或返回假数据**：绝不能为了使测试“跑通”而假装服务可用、返回伪造的 Mock 数据或静默吞掉异常。必须确保在开发、自动化测试与联调期间能第一时间暴露出未配置的问题。
- **杜绝隐式兜底假值**：在参数检验与流程推进中，缺失关键入参或状态必须直接报错中断，严禁隐式使用不可靠的假值掩盖问题。

### 17.2 [反模式 vs 推荐实践]
```java
// ❌ 错误示例 1: 邮件服务未配置时私自伪造发送成功与假凭证
if (!StringUtils.hasText(smtpHost)) {
    log.warn("SMTP 未配置，mock 发送成功");
    return "mock_verify_code_123456"; // 严重违规！掩盖真实环境缺陷
}

// ❌ 错误示例 2: 第三方校验未配置时默认放行
if (!captchaEnabled) {
    return true; // 若业务场景强依赖人机校验，未配置或未提供凭证却假装成功放行
}

// ✅ 正确示例: 缺失关键配置或凭据时直接抛出强类型业务异常中断
String smtpHost = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SMTP_HOST, "");
if (!StringUtils.hasText(smtpHost)) {
    throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "系统邮件基础设施尚未配置，无法发送验证邮件");
}
```