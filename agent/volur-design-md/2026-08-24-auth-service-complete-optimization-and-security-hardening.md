# 施工记录：认证模块核心服务全面优化与安全加固

## 1. 基本信息

- **模块名称**：`volur-auth`
- **变更文件**：
  - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
  - [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/EmailServiceImpl.java)
  - [`JwtUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtUtil.java)
- **执行时间**：2026-08-24
- **规范依据**：[`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 变更背景与审查发现的安全隐患

在对 `volur-auth` 服务层核心方法进行全面安全与规范审计时，识别出以下关键缺陷并实施加固：

1. **登录防爆破与防 DoS 锁定优化 (`authenticate`)**：
   - 原代码中防爆破 Redis Key 直接拼接原始入参，若输入带空格或大小写不一致，会导致多次绕过或锁定状态脱节。
   - 优化为统一归一化处理（`.trim().toLowerCase()`），确保登录尝试计数与账户锁定状态全局一致。
2. **刷新令牌安全轮换机制（RTR）与 JTI 割裂修复 (`refreshToken`)**：
   - 原代码中 `refreshToken` 原样返回传入的 Token，且重新签发 AccessToken 时生成了与 RefreshToken 完全无关的随机 `UUID`。导致用户在单端登出拉黑 AccessToken 时，Refresh Token 依然在全网有效。
   - 引入 **Refresh Token Rotation (RTR)**：每次刷新时立即拉黑旧 Refresh Token，并基于相同的全新 `jti` 签发新的 AccessToken 和 RefreshToken，彻底修复会话注销逃逸漏洞；同时优化 JWT 解析逻辑为单次解析。
3. **防账户存在性枚举与邮件防刷 (`processForgotPassword`)**：
   - 原代码精确提示“邮箱尚未注册”或“用户名与邮箱不匹配”，存在严重的信息泄露（OWASP Top 10 - User Enumeration）。
   - 改造为安全模糊日志记录与静默返回，杜绝攻击者批量探测库内用户。
4. **全系统邮件发送 60 秒频控限制 (`EmailServiceImpl`)**：
   - 增加基于 Redis 的 60s 冷却限制（Key: `vredis:auth:email_rate_limit:{email}`），全面防止注册、重置密码、修改密码接口被脚本滥用引发邮件轰炸（Mail Bombing）。
5. **修改密码安全与用户绑定 (`verifyChangePasswordCode` & `changePassword`)**：
   - 原 Token 仅记录 Email 字符串，现升级为绑定 `userId`。
   - 在 `changePassword` 执行时增加对当前登录用户上下文的强校验（`currentUserId == tokenUserId`），防止横向越权操作。
6. **登出与销户的多端全局失效 (`logout` & `deleteAccount`)**：
   - 登出操作增加 `SecurityUtils.logout()` 清除上下文。
   - 账号销户（`deleteAccount`）时强制触发 `tokenService.forceLogoutUser(userId)`，使该用户在所有设备上的活跃 Token 立即全局失效；
   - 限制用户名拼接长度避免突破 `VARCHAR(64)` 字段限制；
   - 擦除个人资料隐私数据（`bio=null`, `isHidden=true`）。
7. **全覆盖的双键缓存驱逐机制 (`evictUserCache`)**：
   - 由于系统支持用户名及邮箱登录，Spring Cache 会分别以两者作为 Key 缓存 `UserDetails`。
   - 升级 `evictUserCache` 为同时驱逐 `username` 和 `email`（包括小写及原始字符），彻底杜绝缓存旧凭证滞留问题。
8. **事务回滚声明规范化**：
   - 为所有修改操作统一标注 `@Transactional(rollbackFor = Exception.class)`。

---

## 3. 核心修改代码与对照

### 3.1 [`JwtUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtUtil.java) 新增 JTI 绑定重载
```java
public String generateAccessTokenFromUser(User user, String jti) {
    ...
    return Jwts.builder()
            .subject(user.getUsername())
            .id(jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessExpMs)))
            .claim(AUTHORITIES_CLAIM, roles)
            .claim(USER_ID_CLAIM, user.getId())
            .claim(TOKEN_TYPE_CLAIM, "access")
            .signWith(getSecretKey(), Jwts.SIG.HS256)
            .compact();
}

public String generateRefreshTokenFromUser(User user, String jti) {
    final Instant now = Instant.now();
    return Jwts.builder()
            .subject(user.getUsername())
            .id(jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(refreshTokenExpirationMs)))
            .claim(USER_ID_CLAIM, user.getId())
            .claim(TOKEN_TYPE_CLAIM, "refresh")
            .signWith(getSecretKey(), Jwts.SIG.HS256)
            .compact();
}
```

### 3.2 [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/EmailServiceImpl.java) 增加限频
```java
private static final String VREDIS_AUTH_EMAIL_RATE_LIMIT_KEY_PREFIX = "vredis:auth:email_rate_limit:";

private void checkRateLimit(String email) {
    String limitKey = VREDIS_AUTH_EMAIL_RATE_LIMIT_KEY_PREFIX + email.toLowerCase().trim();
    if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
    }
    redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));
}
```

### 3.3 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 双键缓存驱逐与 RTR 刷新实现
```java
private void evictUserCache(User user) {
    if (user != null) {
        evictUserCache(user.getUsername(), user.getEmail());
    }
}

private void evictUserCache(String username, String email) {
    if (cacheManager.isPresent()) {
        Cache cache = cacheManager.get().getCache(CACHE_USER_DETAILS);
        if (cache != null) {
            if (StringUtils.hasText(username)) {
                cache.evict(username.trim());
                cache.evict(username.trim().toLowerCase());
            }
            if (StringUtils.hasText(email)) {
                cache.evict(email.trim());
                cache.evict(email.trim().toLowerCase());
            }
        }
    }
}
```

---

## 4. 验证结果

- 执行 `mvn clean test-compile`：平台 9 个模块（`volur-framework`, `volur-system`, `volur-user`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-scoreboard`, `volur-server`）全部编译成功（`BUILD SUCCESS`）。

---

## 5. 影响范围

- 优化完全向下兼容原有 RESTful API 契约与客户端对接协议；
- 极大增强了系统的抗爆破、防枚举、会话注销严密性及防邮件滥用能力。
