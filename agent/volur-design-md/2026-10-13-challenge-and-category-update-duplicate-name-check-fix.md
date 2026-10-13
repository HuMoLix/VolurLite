# CTF 分类及赛题更新重名校验缺陷修复方案 (Challenge & Category Duplicate Name Check Fix)

## 1. 变更背景与问题溯源

在管理端修改 CTF 赛题分类名称时（例如将已存在的 `"web"` 修改为首字母大写的 `"Web"`），前端发起 `PUT /api/v1/admin/challenges/ctf/categories/{id}` 请求，后端响应 `409 Conflict`，提示 `"分类名称已存在"`，导致修改失败。

### 1.1 核心原因分析
1. **Java 层大小写敏感性 (Case-Sensitive)**：
   Java 中的 `String.equals()` 为区分大小写比较。原有分类名称为 `"web"`，请求入参为 `"Web"`，判定 `!category.getName().equals(request.getName())` 为 `true`，触发了重名检查分支。
2. **JPA 查重逻辑未排除当前实体自身 ID**：
   原有服务层调用 `categoryRepository.existsByName(request.getName())`，查询条件仅为 `WHERE name = :name`，未增加排除当前正在编辑的实体 ID 条件（`AND id <> :id`）。
3. **MySQL 校对规则不区分大小写 (Case-Insensitive)**：
   数据库底层表 `ctf_challenge_category` 的排序规则采用 `utf8mb4_unicode_ci`（或默认 `*_ci`），在 SQL 执行 `WHERE name = 'Web'` 时，MySQL 判定 `'web' = 'Web'` 成立，直接匹配到了当前正在更新的记录本身。
4. **冗余防御与重复 trim() 反模式**：
   DTO 请求入参上已声明 `@Trimmed` 注解，服务层中存在多余的 `.trim()` 手写逻辑，且混用 `StringUtils.hasText`，违反了服务层入参防冗余校验规范。
5. **同类赛题模块普遍性隐患**：
   经全局排查，不仅 CTF 分类更新存在该缺陷，CTF 赛题（`CtfChallenge`）、AWD 赛题（`AwdChallenge`）、AWD+ 赛题（`AwdPlusChallenge`）、ISW 赛题（`IswChallenge`）以及理论赛题（`TheoryChallenge`）在更新名称时均使用了未排除自身 ID 的 `existsByName(...)`，存在完全相同的误报阻断隐患。

---

## 2. 改造方案与实现

### 2.1 Repository 接口层扩展
为所有涉及赛题及分类的 Spring Data JPA 数据访问接口扩展排除自身 ID 的派生查询方法：
- `CtfChallengeCategoryRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`
- `CtfChallengeRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`
- `AwdChallengeRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`
- `AwdPlusChallengeRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`
- `IswChallengeRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`
- `TheoryChallengeRepository`: 新增 `boolean existsByNameAndIdNot(String name, Long id);`

### 2.2 Service 业务校验重构
将所有对应赛题和分类管理服务实现类（`AdminCtfCategoryServiceImpl`、`AdminCtfChallengeServiceImpl`、`AdminAwdChallengeServiceImpl`、`AdminAwdPlusChallengeServiceImpl`、`AdminIswChallengeServiceImpl`、`AdminTheoryChallengeServiceImpl`）中的更新查重逻辑统一改造为：
```java
if (request.getName() != null && !request.getName().isEmpty() && !entity.getName().equals(request.getName())) {
    if (repository.existsByNameAndIdNot(request.getName(), id)) {
        throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在" /* 或 "题目名称已存在" */);
    }
    entity.setName(request.getName());
}
```
1. 规范入参使用，彻底移除冗余的 `.trim()` 与 `StringUtils.hasText`；
2. 查重查询严格绑定 `id` 排除自身，使当前实体仅进行英文字母大小写重命名时（如 `"web"` -> `"Web"`），可正常更新，同时有效拦截与其他已有记录重名的冲突。

---

## 3. 影响范围与验证

### 3.1 变更文件清单
- 后端 持久层接口：
  - `com.humolix.volur.challenge.repository.ctf.CtfChallengeCategoryRepository`
  - `com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository`
  - `com.humolix.volur.challenge.repository.awd.AwdChallengeRepository`
  - `com.humolix.volur.challenge.repository.awdp.AwdPlusChallengeRepository`
  - `com.humolix.volur.challenge.repository.isw.IswChallengeRepository`
  - `com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository`
- 后端 服务实现类：
  - `com.humolix.volur.challenge.service.admin.ctf.impl.AdminCtfCategoryServiceImpl`
  - `com.humolix.volur.challenge.service.admin.ctf.impl.AdminCtfChallengeServiceImpl`
  - `com.humolix.volur.challenge.service.admin.awd.impl.AdminAwdChallengeServiceImpl`
  - `com.humolix.volur.challenge.service.admin.awdp.impl.AdminAwdPlusChallengeServiceImpl`
  - `com.humolix.volur.challenge.service.admin.isw.impl.AdminIswChallengeServiceImpl`
  - `com.humolix.volur.challenge.service.admin.theory.impl.AdminTheoryChallengeServiceImpl`

### 3.2 验证结果
- 执行 `mvn test-compile -pl volur-challenge -am`，编译通过且无任何警告或构建错误；
- 代码严格遵守项目零注释原则与防冗余校验强约束规范。
