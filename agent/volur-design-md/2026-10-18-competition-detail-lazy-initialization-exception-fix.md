# 竞赛详情接口 Hibernate LazyInitializationException 修复记录

## 1. 故障背景与现象

在后台创建竞赛（CTF / THEORY 等）提交成功后，前端系统（`AdminCompetitionCreateView.vue`）获取到新生成的竞赛 ID，并立即路由跳转至对应的竞赛编辑页（`/admin/competitions/ctf/{id}/edit` 或 `/admin/competitions/theory/{id}/edit`）。
在进入编辑页面时，页面发起 `GET /api/v1/admin/competitions/{id}` 接口请求以拉取最新竞赛详情数据，服务端抛出未处理异常并触发 500 报错：

```text
org.hibernate.LazyInitializationException: Could not initialize proxy [com.humolix.volur.user.domain.entity.User#369738757047320576] - no session
	at org.hibernate.proxy.AbstractLazyInitializer.initialize(AbstractLazyInitializer.java:178)
	at com.humolix.volur.user.domain.entity.User$HibernateProxy.getUsername(Unknown Source)
	at com.humolix.volur.competition.mapper.CompetitionMapperImpl.competitionCreatedByUsername(CompetitionMapperImpl.java:673)
	at com.humolix.volur.competition.mapper.CompetitionMapperImpl.toAdminDetailVO(CompetitionMapperImpl.java:140)
	at com.humolix.volur.competition.service.admin.impl.AdminCompetitionServiceImpl.getCompetition(AdminCompetitionServiceImpl.java:114)
```

## 2. 根因分析

1. **实体延迟加载属性未被 Fetch**：
   - `Competition` 实体中定义了创建者关联属性：
     ```java
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "created_by_id", columnDefinition = "BIGINT")
     private User createdBy;
     ```
   - 默认采用 `FetchType.LAZY` 懒加载策略，在没有使用 `JOIN FETCH` 时，JPA 仅注入一个 Hibernate CGLIB/ByteBuddy 代理对象（`User$HibernateProxy`）。
2. **JPQL 查询仅抓取了 config，遗漏了 createdBy**：
   - `CompetitionRepository` 中的 `findByIdWithDetails` 和 `findBySlugWithDetails` 查询定义为：
     ```java
     @Query("SELECT c FROM Competition c LEFT JOIN FETCH c.config WHERE c.id = :id")
     ```
   - 此处只对 `c.config` 执行了抓取连接，`createdBy` 仍维持未初始化的代理状态。
3. **事务上下文与会话生命周期脱节**：
   - `AdminCompetitionServiceImpl.getCompetition(Long id)` 方法未声明 `@Transactional`。在 Repository 查询完成并离开后，持久化上下文（Session）已经关闭。
   - 当调用 `competitionMapper.toAdminDetailVO(competition)` 执行 DTO/VO 对象映射时，MapStruct 生成的代码尝试读取 `competition.getCreatedBy().getUsername()`。由于底层会话已关闭，Hibernate 代理无法发起按需查询加载真实 User 实体，直接抛出 `LazyInitializationException`。

## 3. 修复方案与代码变更

### 3.1 仓库层 JPQL 预加载补充 (CompetitionRepository)
在 `findByIdWithDetails` 与 `findBySlugWithDetails` 两个专用查询方法中，显式追加 `LEFT JOIN FETCH c.createdBy`：
- 在单条 SQL 查询中同时完成 `Competition`、`CompetitionConfig` 以及 `User` (createdBy) 的联表加载；
- 彻底避免在 Session 外触发懒加载，同时杜绝潜在的 N+1 查询；
- 采用 `LEFT JOIN FETCH` 确保即使历史数据中 `created_by_id` 为 null 时，依然能够平滑映射。

### 3.2 业务层事务保证 (AdminCompetitionServiceImpl)
在 `AdminCompetitionServiceImpl.getCompetition(Long id)` 上标注 `@Transactional` 注解：
- 保证方法内部在进行状态自检刷新（`competition.refreshStatus()`）与持久化保存（`competitionRepository.save(competition)`）时处于活跃的数据库事务中；
- 保证 VO 映射阶段处于受控的事务生命周期，避免代理访问断链。

## 4. 影响范围与验证

- **受影响模块**：`volur-competition`
- **变更文件**：
  - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionRepository.java`
  - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java`
- **验证结果**：
  - 执行 `mvn test-compile -DskipTests` 全平台 13 个模块全部编译通过；
  - MapStruct 生成的代码在调用 `competition.getCreatedBy().getUsername()` 时可直接读取已初始化的实体验证，不再出现 `LazyInitializationException`。
