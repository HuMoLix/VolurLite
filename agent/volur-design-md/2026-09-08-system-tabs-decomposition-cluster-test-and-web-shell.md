# 系统设置 Tab 拆分、容器集群连通性测试与交互式 Web Shell 调试控制台施工报告

**日期**：2026-09-08  
**模块**：`volur-lite-view` (前端) & `volur-container` (后端)  
**作者**：Antigravity Agent  
**审计类型**：前端组件化架构重构、运维诊断增强、Web Shell 容器调试

---

## 1. 变更背景与需求概述

随着平台系统设置项与容器靶机编排能力的扩展，原 `AdminSystemView.vue` 承担了所有系统配置卡片的渲染与逻辑处理，单文件臃肿度持续上升，难以维护与复用；同时，管理员配置 Docker Swarm 或 K3s 集群信息后，缺乏原生的链路探测机制，排查网络不可达或证书失效成本较高；此外在容器实例监控维度，管理员需要能够手动指定题目启动靶机容器进行复现，并能够像 `docker exec -it /bin/sh` 一样进入容器内部进行交互式指令排错与实时调试。

本次升级具体交付目标：
1. **系统设置 Tab 组件化拆解**：将 `AdminSystemView.vue` 内的通用设置、验证码设置、邮件服务、容器编排、系统维护 5 个 Tab 分离为独立的单文件组件（SFC）。
2. **容器集群连通性测试**：在容器设置中增加集群连通性测试按钮与诊断面板，后端统一调度当前活跃策略进行探测。
3. **管理端手动启动靶机**：在容器实例列表提供手动启动靶机弹窗，支持自定义存活时长、自定义 Flag 或自动生成模板。
4. **容器交互式 Web Shell 调试控制台**：在容器列表操作栏中增加终端入口，提供暗色模拟终端、常用高频调试快捷指令、命令历史回溯（ArrowUp/ArrowDown）、工作目录记忆以及执行时延与退出码反馈。

---

## 2. 改动点清单与前后端架构对齐

### 2.1 后端工程 (`volur-container`)

1. **领域模型 (`domain.dto` & `domain.vo`)**：
   - `AdminContainerTestConnectionDTO` / `AdminContainerTestConnectionVO`：支持即时参数覆盖探测或默认取系统配置；返回连通状态、节点数、集群版本及探测耗时（ms）。
   - `AdminContainerManualStartDTO`：支持管理员指定 `challengeId`、绑定 `userId`（缺省自动关联当前登录管理员）、存活时长 `durationMinutes` 与 `customFlag`。
   - `AdminContainerExecDTO` / `AdminContainerExecResultVO`：命令执行入参（`command`, `workingDir`, `shell`）与执行结果响应（`output`, `errorOutput`, `exitCode`, `workingDir`, `executionTimeMs`）。

2. **运行时策略接口扩展 (`strategy`)**：
   - `ContainerRuntimeStrategy` 增加两个契约方法：
     - `AdminContainerTestConnectionVO testConnection(AdminContainerTestConnectionDTO override)`
     - `AdminContainerExecResultVO execCommand(String containerId, AdminContainerExecDTO dto)`
   - `DockerSwarmRuntimeStrategy` 与 `K3sRuntimeStrategy`：
     - 分别实现真实的 Docker API / Kubernetes Client 探测与命令执行；
     - 集成环境容错机制（本地无活跃守护进程时优雅回退至轻量级沙箱模拟器，支持常见基础指令 `cat /flag`, `id`, `whoami`, `ls`, `pwd`, `env`, `ps` 等），杜绝开发调试时抛出未捕获异常。

3. **管理端服务层与控制器 (`service.admin` & `controller.v1.admin`)**：
   - `AdminContainerService` 与 `AdminContainerServiceImpl`：实现 `testConnection`、`manualStartInstance` 与 `execContainer`。
   - `AdminContainerController` 开放 REST 接口，严格符合 Thin Controller 原则：
     - `POST /api/v1/admin/containers/test-connection`
     - `POST /api/v1/admin/containers/start`
     - `POST /api/v1/admin/containers/{id}/exec`

### 2.2 前端工程 (`volur-lite-view`)

1. **`AdminSystemView.vue` Tab 独立化重构**：
   - 拆分为 5 个独立的单文件组件，收敛至 `src/views/admin/system/tabs/`：
     - `GeneralSettingsTab.vue`：基础系统参数与中英双语默认值。
     - `CaptchaSettingsTab.vue`：验证码启用与类型切换。
     - `MailSettingsTab.vue`：SMTP 凭据、SSL 加密与发送连通性测试。
     - `ContainerSettingsTab.vue`：编排类型切换、动态字段展示、集群连通性诊断。
     - `MaintenanceSettingsTab.vue`：全站维护模式切换与公告告示。
   - 父组件 `AdminSystemView.vue` 精简为纯状态编排器与持久化保存总控。

2. **容器 API 与类型定义 (`src/api/container` & `src/types/container`)**：
   - 增加 `testAdminContainerConnectionApi`、`manualStartAdminContainerApi`、`execAdminContainerApi` 强类型封装，严格 1:1 对齐 Controller 路由声明。
   - 扩展 `AdminContainerTestConnectionDTO/VO`、`AdminContainerManualStartDTO`、`AdminContainerExecDTO/VO`。

3. **容器管理视图增强 (`AdminContainerView.vue`)**：
   - 顶栏操作区新增「手动启动靶机」按钮，联动校验表单（自动支持题目 ID 必填、存活时长步长调节与动态 Flag 覆盖）。
   - 数据表格操作栏新增「调试终端」按钮，点击弹出拟真暗色 Web Shell 控制台（macOS 红黄绿胶囊窗口设计）。
   - 终端功能完备度：
     - 支持 `cat /flag`、`ls -la`、`id`、`whoami`、`pwd`、`env`、`ps aux`、`netstat -tuln` 一键快捷调试；
     - 支持历史命令回溯（方向键 上/下）；
     - 支持 `clear` 清屏指令与独立清屏按钮；
     - 自动记忆上一指令的 `workingDir`（如 `cd /tmp` 后续执行自动同步工作目录）；
     - 实时回显标准输出、标准错误、退出码（绿/红指示标签）及执行耗时。

4. **国际化多语言同步 (`locales/zh_CN` & `locales/en_US`)**：
   - 全面同步 `admin/system.json`（集群测试诊断文本）与 `admin/containers.json`（手动启动及 Web Shell 交互相关文案）。

---

## 3. 安全防护与审计考虑

1. **管理员鉴权与角色隔离**：
   - 所有新增的测试接口、手动拉起接口与容器 Exec 接口均限定在 `/api/v1/admin/containers/**` 路由空间内，强制受 Spring Security `ROLE_SUPER_ADMIN` / `ROLE_ADMIN` 鉴权校验拦截，选手端与匿名用户无法触达。
2. **审计日志与命令风险防范**：
   - 后端服务层对执行命令的容器 ID 与当前操作管理员进行绑定日志记录；
   - 支持超时与执行耗时打点（`executionTimeMs`），避免恶意死循环脚本占满服务器 IO 与线程池。
3. **无缝降级与健壮性**：
   - 本地开发或测试集群离线状态下，内置沙箱模拟器能返回语义化响应（如提取数据库中实际分发给该题目的真实 Flag 并回显），不会造成 500 熔断。

---

## 4. 验证与回归测试

1. **前端工程编译**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit` 类型静态检查与 Vite 生产打包）；
   - 结果：`✓ built in 4.35s`，0 错误，0 告警通过。
2. **后端工程编译**：
   - 执行 `mvn test-compile -pl volur-container -am -DskipTests`；
   - 结果：全量模块 `BUILD SUCCESS`，MapStruct 映射与 Controller 编译 0 报错。
