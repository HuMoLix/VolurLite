# 竞赛队伍列表权限管控与队员关键信息脱敏加固

## 1. 变更背景与安全隐患分析

### 1.1 隐患背景
在竞赛参赛队伍接口 `GET /api/v1/competitions/{competitionId}/teams` 的历史实现中：
1. **未登录匿名访问隐患**：由于 Spring Security 配置中放行了 `/api/v1/competitions/**`，且 [`CompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java) 的 `listTeams` 接口未声明认证参数或权限注解，导致任意未登录访客均能直接发起请求。
2. **敏感个人隐私数据全量泄露**：[`CompetitionMemberVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionMemberVO.java) 中包含了参赛队员的真实身份证号（`idCard`）、手机号（`phone`）、真实姓名（`realName`）及电子邮箱（`email`）。此前接口无差别向所有请求方直接序列化输出全部明文，存在极其严重的信息安全与个人隐私泄露风险。

### 1.2 改造目标与安全规范
- **接口强制身份认证管控**：对 `/api/v1/competitions/{competitionId}/teams` 施加严格登录权限管控，未登录用户访问统一由框架层前置拦截并响应 `401 Unauthorized`。
- **细粒度角色可见性与数据脱敏规则**：
  1. **管理员（Admin / SuperAdmin）**：拥有完整审计与管理权限，可查看队员全部完整信息；
  2. **本队队长（Captain）与领队（Leader）**：拥有队伍管理权限，能查看其所在队伍所有成员的完整真实信息（明文不脱敏）；
  3. **本队普通队员（Member）**：同队队友之间针对关键隐私数据（`idCard`、`phone`、`email`、`realName`）进行掩码脱敏（队员查看自己本人信息保持完整）；
  4. **非本队外部成员（其他队伍队员、普通参赛用户）**：禁止接触私密信息，其 `idCard`、`phone`、`email`、`realName` 等敏感字段统一置为 `null`，仅保留队员公开昵称、头像和队内角色等基本公共档案。

---

## 2. 核心改动点

### 2.1 Web 控制器层鉴权加固
修改 [`CompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java#L30-L36)：
- 在 `listTeams` 方法入参声明 [`@CurrentUserId Long userId`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/CurrentUserId.java)；
- 利用框架底层 [`CurrentUserIdArgumentResolver`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserIdArgumentResolver.java) 的 Fail-Fast 机制，当检测到未认证或匿名请求时立即抛出 `BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录")`，杜绝未登录穿透；
- 将解析得到的 `userId` 向下透传至业务服务层。

### 2.2 服务接口与业务实现层细粒度脱敏
1. **服务契约升级**：
   - 更新 [`CompetitionTeamService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionTeamService.java)：扩展 `listTeams(Long competitionId, Long currentUserId)`，并提供默认无状态重载以保持兼容性。
2. **动态权限识别与脱敏逻辑**：
   - 更新 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)：
     * 首先校验竞赛有效性，杜绝越权访问不存在的比赛；
     * 解析当前用户在当前竞赛中的所属队伍及角色（通过 `memberRepository.findAllByCompetitionIdAndUserId` 查询 `INVITED_ACCEPTED` 态成员记录）；
     * 遍历参赛队伍时，分别判定当前用户在该队伍中是否为队长/领队、普通队员或非本队人员；
     * 针对同队队员查看其他队友，调用 `desensitizeKeyInfo` 对关键信息进行标准化掩码脱敏：
       - 手机号（`phone`）：保留前 3 位与后 4 位，中间使用 4 个 `*` 隐藏（如 `138****5678`）；
       - 身份证号（`idCard`）：18 位标准身份证保留前 6 位与后 4 位，中间使用 8 个 `*` 隐藏（如 `110101********1234`）；
       - 电子邮箱（`email`）：用户名部分保留首尾字符并以 `***` 脱敏（如 `z***n@example.com`）；
       - 真实姓名（`realName`）：同队队员之间保持明文展示，不进行脱敏；
     * 针对非本队人员（外部参赛者），调用 `hidePrivateInfo` 将身份证、手机号、真实姓名、邮箱、部门、职位等字段强制置为 `null`。

### 2.3 单元测试覆盖
1. **业务服务层测试**：新增 [`CompetitionTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CompetitionTeamServiceImplTest.java)，全面覆盖：
   - 队长/领队视角：本队成员敏感信息明文完整展示；
   - 队员视角：自身信息明文展示，其他队友关键隐私（身份证、手机号、邮箱）脱敏，真实姓名保持明文；
   - 外部人员视角：非本队队员敏感信息全量置空隐藏。
2. **控制器路由与鉴权测试**：新增 [`CompetitionTeamControllerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/controller/CompetitionTeamControllerTest.java)，验证：
   - 已登录用户正常放行并正确注入 `userId`；
   - 未登录匿名请求被 `CurrentUserIdArgumentResolver` 拦截并拒绝。

### 2.4 全局脱敏工具类抽取 (`volur-framework`)
- 在 [`volur-framework`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework) 的 `com.humolix.volur.framework.common.util` 包下新增 [`DesensitizationUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/DesensitizationUtil.java)：
  * `maskPhone(phone)`：手机号脱敏（如 `138****5678`）；
  * `maskIdCard(idCard)`：身份证号脱敏（如 `110101********1234`）；
  * `maskEmail(email)`：邮箱脱敏（如 `z***n@example.com`）；
  * `maskRealName(realName)`：真实姓名脱敏（如 `张*`、`李*五`）；
  * `maskBankCard(cardNum)`：银行卡号脱敏；
  * `maskKey(key)`：密钥/凭证脱敏；
  * `mask(str, prefix, suffix, maskChar)`：通用字符串掩码脱敏；
- 改造 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)，全面复用 `DesensitizationUtil`，消除各业务模块样板代码冗余；
- 新增 [`DesensitizationUtilTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/util/DesensitizationUtilTest.java) 完备单元测试（覆盖全部脱敏函数与空值/边界场景）。

---

## 3. 验证与审计结果

- **工具类测试**：运行 `mvn test -pl volur-framework -Dtest=DesensitizationUtilTest`，7 个测试用例 100% 通过；
- **业务与控制器测试**：运行 `mvn test -pl volur-competition`，6 个测试用例全部通过（`BUILD SUCCESS`）；
- **全平台回归测试**：运行 `mvn test` 对全平台 12 个子模块进行全量回归，全部通过（`0 Failures, 0 Errors, BUILD SUCCESS`）。
