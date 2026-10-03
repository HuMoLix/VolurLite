# 修复 WebAuthn 通行密钥凭证 public_key 字段二进制 BLOB 类型不匹配缺陷

## 1. 缺陷背景与根因深度剖析

### 1.1 报错现象
在新一代前端工程的通行密钥绑定视图 [`passkeyProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue) 中，用户完成本地生物识别或硬件安全密钥（FIDO2 / Windows Hello / Touch ID）凭证创建后，前端向后端提交注册请求：
`POST /api/v1/security/passkey/register`

后端在执行凭据持久化存储时抛出未捕获的 SQL 异常：
```
org.springframework.jdbc.UncategorizedSQLException: PreparedStatementCallback; uncategorized SQLException for SQL [INSERT INTO user_credentials (credential_id, user_entity_user_id, public_key, signature_count, uv_initialized, backup_eligible, authenticator_transports, public_key_credential_type, backup_state, attestation_object, attestation_client_data_json, created, last_used, label ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)]; SQL state [HY000]; error code [1366]; Incorrect string value: '\xA4!C\x01\x00\x01...' for column 'public_key' at row 1
Caused by: java.sql.SQLException: Incorrect string value: '\xA4!C\x01\x00\x01...' for column 'public_key' at row 1
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeInternal(ClientPreparedStatement.java:988)
	at org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository.insertCredentialRecord(JdbcUserCredentialRepository.java:164)
	at org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository.save(JdbcUserCredentialRepository.java:157)
```

### 1.2 根因定位分析
1. **COSE 公钥的二进制本质**：
   - WebAuthn 规范（W3C Web Authentication）和 RFC 8152 / RFC 9052 规定，客户端认证器返回的公钥采用 COSE_Key（CBOR Object Signing and Encryption）结构编码；
   - CBOR 为紧凑二进制对象表示协议，典型的 ES256 / RSA 结构首字节通常为 `0xA4`（代表含有 4 个键值对的 CBOR Map：`kty`、`alg`、`crv`、`x/y` 坐标），后续紧随私有键与大数二进制字节流，**绝非有效的 UTF-8 字符文本**；
2. **Spring Security WebAuthn 框架实现标准**：
   - 查看 Spring Security 核心类 [`JdbcUserCredentialRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/config/WebAuthnConfiguration.java)：
     ```java
     parameters.add(new SqlParameterValue(Types.BLOB, record.getPublicKey().getBytes()));
     ```
     入库时通过 `ps.setBytes(...)` 以 `Types.BLOB` 写入；
     读取时通过 `new ImmutablePublicKeyCose(this.getBytes.getBytes(rs, "public_key"))` 直接按原始字节流读取；
   - Spring Security 官方自带的标准数据表结构脚本 `org/springframework/security/user-credentials-schema.sql` 明确定义：
     ```sql
     create table user_credentials
     (
         credential_id                varchar(1000) not null,
         user_entity_user_id          varchar(1000) not null,
         public_key                   blob          not null,
         ...
     );
     ```
3. **平台原有定义偏差**：
   - 在 [`UserCredential.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/entity/UserCredential.java) 中将 `publicKey` 声明为了 `String`，并标注 `columnDefinition = "TEXT"`；
   - 在数据库建表与升级脚本 [`upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 中，将 `public_key` 误设为 `text COLLATE utf8mb4_unicode_ci NOT NULL`；
   - 当 MySQL 针对 `TEXT` 字段接收到二进制字节流时，服务端会尝试将其作为 `utf8mb4` 字符集解析。首字节 `0xA4`（二进制 `10100100`）在 UTF-8 编码中属于非法的前导字节（仅能作为后续字节），直接触发 MySQL 的 `1366 (HY000) Incorrect string value` 异常。

---

## 2. 修复改动清单

### 2.1 JPA 实体类映射修复 (`UserCredential.java`)
- 文件位置：[`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/entity/UserCredential.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/entity/UserCredential.java)
- 改动内容：
  将 `publicKey` 的 Java 实体类型由 `String` 修正为 `byte[]`，并将字段定义由 `columnDefinition = "TEXT"` 修正为 `columnDefinition = "BLOB"`，保持与 Spring Security `JdbcUserCredentialRepository` 的 `Types.BLOB` 完全契合。

### 2.2 数据库平滑升级脚本幂等更新 (`upgrade_to_latest.sql`)
- 文件位置：[`scripts/upgrade/upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)
- 改动内容：
  1. 将第 4 部分中 `CREATE TABLE IF NOT EXISTS user_credentials` 的建表 DDL 中 `public_key` 列定义修正为 `public_key blob NOT NULL`；
  2. 在第 5 部分幂等迁移存储过程 `migrate_volur_schema_20260921` 中增加 **3.11** 步：通过 `information_schema.COLUMNS` 探测 `user_credentials` 表中 `public_key` 的 `DATA_TYPE` 是否属于 `blob`、`mediumblob` 或 `longblob`；若为旧版本的 `text` 或字符类型，则自动执行 `ALTER TABLE user_credentials MODIFY COLUMN public_key blob NOT NULL;` 平滑无损修正列类型。

---

## 3. 生产/开发数据库即时热修复指令

已部署的运行中数据库无需删除表或清空数据，直接在 MySQL 控制台或客户端执行以下单行 DDL 即可即时生效：
```sql
ALTER TABLE `user_credentials` MODIFY COLUMN `public_key` BLOB NOT NULL;
```
