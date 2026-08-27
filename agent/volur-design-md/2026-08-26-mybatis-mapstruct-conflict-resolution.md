# 启动异常排查修复与 MyBatis / MapStruct 架构清晰化治理

## 1. 启动异常与问题深度剖析

### 1.1 致命异常：`SystemMapper: Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`
- **根因分析**：
  在 [`VolurLiteApplication.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/java/com/humolix/volur/VolurLiteApplication.java) 启动类上标注了 MyBatis 的 `@MapperScan("com.humolix.volur.**.mapper")` 注解。
  而在 Volur Lite 架构中，`com.humolix.volur.<module>.mapper` 包下存放的全部是 **MapStruct** 领域对象转换映射器（标注 `@Mapper(componentModel = "spring")`）。
  当 Spring Boot 启动时，MyBatis Spring 扫描器错误地将 MapStruct 映射接口识别为 MyBatis DAO Mapper，尝试为其创建 `MapperFactoryBean`。由于 Volur Lite 全面基于 Spring Data JPA 构建，容器中没有配置 MyBatis 的 `SqlSessionFactory`，导致 `MapperFactoryBean.checkDaoConfig()` 触发断言失败，抛出 `IllegalArgumentException` 导致启动中断。

### 1.2 告警异常：`RedisWorkerIdAssigner: RedisConnectionFailureException`
- **根因分析**：
  本地启动时 Redis 服务（6379 端口）未运行或网络不可达。
- **高可用容灾表现**：
  Volur Lite 的分布式雪花算法分配器 [`RedisWorkerIdAssigner`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/id/worker/RedisWorkerIdAssigner.java) 捕获了该异常并优雅降级为基于本机 IP/Hostname 哈希的 WorkerID（`731`），保证了雪花算法 ID 在无 Redis 环境下依然可以正常生成，未阻断系统运行。

---

## 2. 治理与重构方案

### 2.1 清除 MyBatis-Plus 与 MapStruct 的语义冲突
1. **移除 `@MapperScan`**：从 `VolurLiteApplication.java` 移除 `@MapperScan("com.humolix.volur.**.mapper")`，MapStruct 映射器由 MapStruct 编译器生成的 Spring `@Component` 自动装配，无需任何第三方扫描注解；
2. **清理冗余依赖**：
   - Volur Lite 数据持久层 100% 采用 **Spring Data JPA**（Hibernate 7.x）；
   - 从 `volur-framework/pom.xml` 和根 `pom.xml` 中彻底移除 `mybatis-plus-spring-boot3-starter`；
   - 清理所有实体枚举类中残留的 `com.baomidou.mybatisplus.annotation.EnumValue` 注解；
   - 从 `application-dev.yml` 中清理 `mybatis-plus:` 配置段与日志配置。

---

## 3. 验证结果

1. 执行 `mvn clean compile -DskipTests`：全平台 12 个子模块全部构建成功（12/12 SUCCESS）；
2. 执行 `mvn test`：所有单元测试执行完毕，0 失败 0 错误（BUILD SUCCESS）；
3. 彻底消除 `sqlSessionFactory` 缺失引发的启动崩溃。
