# 容器动态 Flag 环境变量注入规范重构方案记录 (仅注入 volurflag)

## 1. 变更背景与设计动机
在平台原有的容器调度设计中，靶机启动时为了向下兼容历史题目与社区通用规范，向容器同时注入了 `FLAG` 与 `GZCTF_FLAG` 两个环境变量。
为统一平台标准并隔离特定外部框架依赖，现根据需求重构动态 Flag 注入规则：
1. 废止 `FLAG` 与 `GZCTF_FLAG` 环境变量注入；
2. 统一且仅注入 `volurflag` 作为容器的动态 Flag 环境变量；
3. 确保在 Docker Swarm 调度策略与 K3s (Kubernetes) 调度策略中双轨对齐，且在普通动态开题与管理员手动启动两种场景下完全一致。

---

## 2. 核心改动点说明

### 2.1 Docker Swarm 运行时策略 (`DockerSwarmRuntimeStrategy.java`)
- **常规开题调度 (`startContainer`)**：
  - 移除 `command.add("FLAG=" + dynamicFlag)` 与 `command.add("GZCTF_FLAG=" + dynamicFlag)`；
  - 调整为仅注入单一环境变量 `command.add("volurflag=" + dynamicFlag)`。
- **管理员手动启动调度 (`startManualContainer`)**：
  - 同步移除 `FLAG` 与 `GZCTF_FLAG` 注入逻辑；
  - 调整为在传入有效 `dynamicFlag` 时仅注入 `volurflag`。

### 2.2 K3s (Kubernetes) 运行时策略 (`K3sRuntimeStrategy.java`)
- **常规开题调度 (`startContainer`)**：
  - 移除 Pod 规格定义中注入的 `FLAG` 与 `GZCTF_FLAG` `EnvVar` 对象；
  - 使用 `StringUtils.hasText(dynamicFlag)` 条件判断，仅向容器注入名称为 `volurflag` 的环境变量。
- **管理员手动启动调度 (`startManualContainer`)**：
  - 同步调整 Pod 环境变量列表构建逻辑，仅追加 `volurflag` 变量。

---

## 3. 影响范围与回归验证

- **影响文件**：
  - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java`
  - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java`
- **回归验证**：
  - 执行 `mvn -pl volur-container -am test-compile` 与 `mvn -pl volur-container test`，模块全部单元测试通过（7/7 passed），无任何编译报错；
  - 代码严格遵循零注释与纯粹代码原则。
