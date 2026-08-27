# Volur Lite 修复 Maven 重复依赖声明警告归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-framework`
- **文档类型**：依赖配置优化与构建警告清理

---

## 1. 问题背景

在执行 Maven 构建时，控制台输出以下警告提示：
```text
'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique: org.projectlombok:lombok:jar -> duplicate declaration of version (?) @ line 110, column 21
It is highly recommended to fix these problems because they threaten the stability of your build.
For this reason, future Maven versions might no longer support building such malformed projects.
```

Maven 规范要求在同一个 POM 的 `<dependencies>` 列表中，`groupId:artifactId:type:classifier` 必须保持唯一。

---

## 2. 问题定位与排查

经排查 [`volur-framework/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/pom.xml)：
1. 在第 100-104 行声明了 `org.projectlombok:lombok` (`<scope>provided</scope>`)；
2. 在第 110-114 行重复声明了完全相同的 `org.projectlombok:lombok` (`<scope>provided</scope>`)。

对全工程 12 个子模块及父工程 POM 进行了全局依赖唯一性扫描，确认仅 `volur-framework` 存在该重复声明。

---

## 3. 修复措施

在 [`volur-framework/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/pom.xml) 中移除多余的重复 `lombok` 依赖块，保留单一规范声明。

---

## 4. 验证结果

重新执行全平台 Maven 构建：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`），且不再产生任何重复依赖相关的 Maven 构建告警。
