# 施工记录：认证模块用户注册（register）方法重构与安全加固

## 1. 基本信息

- **模块名称**：`volur-auth`
- **变更目标**：优化并重构 `com.humolix.volur.auth.service.impl.AuthServiceImpl.java` 中的 `register` 方法
- **执行时间**：2026-08-24
- **规范依据**：[`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 变更背景与审查发现的问题

在对 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 中的 `register` 方法进行规范与安全审查时，发现了以下若干规范与安全隐患：

1. **Spring `@Transactional` 默认不回滚受检异常**：
   - 原代码中抛出的 `UserOrEmailExistException` 继承自 `AuthException`（继承自 `java.lang.Exception` 受检异常）。
   - `@Transactional` 默认仅在遇到 `RuntimeException` 或 `Error` 时触发回滚。若事务内执行前置写操作后发生冲突抛出此异常，事务不会回滚。
2. **邮箱验证码校验与开关逻辑脱节（逻辑阻断）**：
   - 邮箱验证码校验 `emailService.verifyCode(...)` 位于 `REQUIRE_EMAIL_VERIFICATION` 开关判断外部，导致当系统配置未启用邮箱验证时，用户注册仍强制校验验证码而必然失败。
   - 邮箱后缀白名单过滤原先被包含在 `REQUIRE_EMAIL_VERIFICATION` 条件分支中，一旦关闭邮件验证，白名单校验也一并失效。
3. **邮箱后缀白名单弱匹配绕过漏洞（CWE-183）**：
   - 原代码使用 `request.getEmail().toLowerCase().endsWith(...)` 进行文本匹配。若白名单配置为 `example.com`，攻击者可使用 `attacker@evil-example.com` 或 `fake@other-example.com` 绕过校验。
4. **邮箱/用户名大小写未归一化**：
   - 未在注册入口对用户名和邮箱执行 `trim()` 与 `toLowerCase()`（针对邮箱）处理，导致在数据库存储与缓存服务之间可能出现大小写不一致及身份混淆风险。
5. **默认角色自动创建与幽灵角色隐患**：
   - 默认角色获取失败时直接向数据库插入无权限定义的空壳角色 `Role.builder().name("USER").description("普通用户").build()`，易引发系统权限不完整异常。
6. **浮点数时间戳微积分算法精度隐患**：
   - 原代码使用 `totalPoints(0.001 - (System.currentTimeMillis() * 0.0000000000000001))` 将时间戳塞入浮点字段，存在精度丢失、负积分以及同一毫秒并发冲突问题。

---

## 3. 具体修改内容与对照

### 3.1 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 改动

#### 核心修改点：
- **事务回滚声明**：声明 `@Transactional(rollbackFor = Exception.class)`。
- **数据清洗与归一化**：
  - `normalizedUsername = request.getUsername().trim()`
  - `normalizedEmail = request.getEmail().trim().toLowerCase()`
- **严格邮箱域名白名单校验**：
  - 提取 `@` 后的域名部分，支持配置项带 `@` 或不带 `@` 的域名精确匹配及标准子域名匹配（`domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain)`）。
  - 白名单校验独立于邮箱验证码开关，只要配置了白名单规则即可全局生效。
- **受控邮箱验证码校验**：
  - 仅在 `systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)` 为 `true` 时才校验邮箱验证码。
- **角色安全检查**：
  - 若系统缺少 `USER` 基础角色，抛出明确的内部错误异常，避免动态写入脏数据。
- **初始积分规范化**：
  - 初始 `totalPoints` 设置为精确的 `0.0`，消除浮点数时间戳 Hack。

#### 代码变更前（Before）：
```java
@Override
@Transactional
public void register(RegisterRequestDTO request) throws InvalidCaptchaException, UserOrEmailExistException {
    if (!systemSettingService.getBoolean("REGISTER_ENABLED", true)) {
        throw new BusinessException(HttpStatus.FORBIDDEN, "系统已关闭注册功能");
    }

    if (systemSettingService.getBoolean("ENABLE_CAPTCHA", true)) {
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new InvalidCaptchaException();
        }
    }

    if (userRepository.existsByUsername(request.getUsername()) || userRepository.existsByEmail(request.getEmail())) {
        throw new UserOrEmailExistException();
    }

    if (systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)) {
        String whitelistStr = systemSettingService.getSettingValue("EMAIL_WHITELIST_SUFFIXES", "");
        if (StringUtils.hasText(whitelistStr)) {
            String[] whitelist = whitelistStr.split(",");
            boolean isWhitelisted = Arrays.stream(whitelist)
                    .anyMatch(suffix -> request.getEmail().toLowerCase().endsWith(suffix.trim().toLowerCase()));
            if (!isWhitelisted) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
            }
        }
    }

    if (!emailService.verifyCode(request.getEmail(), request.getEmailCaptcha())) {
        throw new InvalidCaptchaException();
    }

    String defaultRoleName = systemSettingService.getSettingValue("DEFAULT_USER_ROLE", "USER");
    Role userRole = roleRepository.findByName(defaultRoleName)
            .orElseGet(() -> roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("普通用户").build())));

    User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .role(userRole)
            .status(true)
            .totalPoints(0.001 - (System.currentTimeMillis() * 0.0000000000000001))
            .solves(0L)
            .build();
    ...
}
```

#### 代码变更后（After）：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void register(RegisterRequestDTO request) throws InvalidCaptchaException, UserOrEmailExistException {
    if (!systemSettingService.getBoolean("REGISTER_ENABLED", true)) {
        throw new BusinessException(HttpStatus.FORBIDDEN, "系统已关闭注册功能");
    }

    if (systemSettingService.getBoolean("ENABLE_CAPTCHA", true)) {
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new InvalidCaptchaException("图形验证码错误或已过期");
        }
    }

    String normalizedUsername = request.getUsername().trim();
    String normalizedEmail = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByUsername(normalizedUsername) || userRepository.existsByEmail(normalizedEmail)) {
        throw new UserOrEmailExistException();
    }

    String whitelistStr = systemSettingService.getSettingValue("EMAIL_WHITELIST_SUFFIXES", "");
    if (StringUtils.hasText(whitelistStr)) {
        String emailDomain = normalizedEmail.substring(normalizedEmail.lastIndexOf("@") + 1);
        boolean isWhitelisted = Arrays.stream(whitelistStr.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StringUtils::hasText)
                .map(suffix -> suffix.startsWith("@") ? suffix.substring(1) : suffix)
                .anyMatch(allowedDomain -> emailDomain.equals(allowedDomain) || emailDomain.endsWith("." + allowedDomain));
        if (!isWhitelisted) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
        }
    }

    if (systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false)) {
        if (!emailService.verifyCode(normalizedEmail, request.getEmailCaptcha())) {
            throw new InvalidCaptchaException("邮箱验证码错误或已过期");
        }
    }

    String defaultRoleName = systemSettingService.getSettingValue("DEFAULT_USER_ROLE", "USER");
    Role userRole = roleRepository.findByName(defaultRoleName)
            .orElseGet(() -> roleRepository.findByName("USER")
                    .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "系统默认角色配置缺失")));

    User user = User.builder()
            .username(normalizedUsername)
            .password(passwordEncoder.encode(request.getPassword()))
            .email(normalizedEmail)
            .role(userRole)
            .status(true)
            .totalPoints(0.0)
            .solves(0L)
            .build();

    User savedUser;
    try {
        savedUser = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException ex) {
        throw new UserOrEmailExistException();
    }

    UserProfile userProfile = UserProfile.builder()
            .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
            .user(savedUser)
            .isHidden(false)
            .build();

    userProfileRepository.save(userProfile);

    SpringContextHolder.publishEvent(new UserStatusChangedEvent(this, savedUser.getId(), UserStatusAction.REGISTER));
}
```

---

### 3.2 规范与配置文件更新

1. **[`.gitignore`](file:///H:/HuMoLix/github/Volur-Lite/.gitignore)**：
   - 添加 `volur-design-md/` 目录忽略规则，避免施工和审计文档污染主代码库版本树。
2. **[`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)**：
   - 在 AI 行为指引中加入第 3 条规范：要求每次 Agent 对文件进行修改后，均需在 `volur-design-md` 目录下生成相应的 Markdown 施工与审计记录。

---

## 4. 验证结果

- 执行 `mvn clean test-compile`：整个平台 9 个模块（`volur-framework`, `volur-system`, `volur-user`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-scoreboard`, `volur-server`）全部编译成功（`BUILD SUCCESS`）。

---

## 5. 影响范围与后续建议

- **影响范围**：仅作用于 `volur-auth` 的注册接口逻辑，向下兼容原接口契约。
- **后续优化建议**：
  1. 在事件监听端，建议针对 [`UserStatusChangedEvent`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/event/UserStatusChangedEvent.java) 的处理方法添加 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`，彻底隔离事务提交后外部副作用。
  2. 后续可将 `UserOrEmailExistException` 继承关系统一迁移至 `BusinessException`，进一步精简方法签名。
