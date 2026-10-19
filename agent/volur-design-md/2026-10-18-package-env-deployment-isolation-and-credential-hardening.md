# 交付打包脚本 package.sh 环境变量隔离与凭据兜底改造方案

## 1. 变更背景与问题定位

在平台通过 `package.sh` 构建交付包并上传到生产服务器部署的场景中，发现以下缺陷与异常：
1. **本地开发环境配置污染交付包**：
   原逻辑优先读取开发机工作区根目录的 `.env`，将其作为交付包中的 `.env.example` 和 `.env`。导致开发机本地的 `SPRING_PROFILES_ACTIVE=dev`、临时数据库连接凭据及调试配置被打包进生产交付归档，导致生产服务器 Spring Boot 容器默认以 `dev` profile 运行，削弱了生产环境连接池、SQL 批处理等性能优化。
2. **归档中包含物理 `.env` 导致服务器升级覆盖风险**：
   原逻辑在构建交付包时直接将 `.env.example` 复制为 `.env` 并一并打包。当运维人员解压新版本升级包（`tar -zxvf volur-lite-<arch>.tar.gz`）时，会将服务器上已有且配置好的生产 `.env` 强行覆盖。
3. **MySQL 卷初始化与应用密码不一致引发 `Access denied (1045)` 报错**：
   MySQL 容器仅在数据卷（`volur-mysql-data`）初次创建为空时依据环境变量初始化用户及密码。当 `.env` 因打包被覆盖或密码修改后，MySQL 容器因持久化数据卷跳过重置逻辑保持旧密码，而 `volur-app` 读取新密码连接数据库，导致抛出 `Access denied for user 'volur' (using password: YES)`。同时，原编排配置中缺少一致的密码安全缺省值兜底。

## 2. 改动点清单

1. **`package.sh` 交付包环境隔离**：
   - 彻底废除向交付包打包物理 `.env` 文件的行为，归档中仅提供标准生产配置模板 `.env.example`。
   - `.env.example` 默认明确指定 `SPRING_PROFILES_ACTIVE=prod`，杜绝本地 `dev` 配置逃逸。
   - 目标机首次运行 `./start.sh` 时，幂等检测当前若不存在 `.env` 则自动基于 `.env.example` 创建，既保证开箱即用，又确保版本迭代解压时绝不覆盖服务器现有 `.env`。
2. **编排凭据安全回退兜底**：
   - 在生成的独立 `docker-compose.yml` 中，针对 `volur-mysql`、`healthcheck` 及 `volur-app` 服务，统一为 `VOLUR_MYSQL_PASSWORD` 声明一致的回退默认值 `${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}`，防止因单侧变量缺失造成凭据不对称。
3. **交付文档同步**：
   - 更新交付包内置 `README.md`，明确 `.env.example` 与 `.env` 的职责与生命周期。

## 3. 影响范围与迁移建议

- **影响文件**：[`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh)
- **迁移与修复建议**：
  - 针对服务器已出现 `Access denied` 的环境：若为刚部署的全新环境，可执行 `docker compose down -v` 清空数据卷后重拉；若已有生产数据，可通过 MySQL 容器执行 `ALTER USER` 修正密码，或将服务器 `.env` 中的密码调整为初次创建数据卷时的原始密码。
