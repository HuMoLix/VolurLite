# 交付包打包脚本实现与镜像体积深度审计文档

## 1. 变更背景与用户需求
1. **体积成因分析**：用户对导出的 `volur-images.tar.gz` 达到 521MB 的原因存疑，需深度拆解各组件的体积构成与瘦身可行性；
2. **交付打包自动化**：需提供一个开箱即用的跨平台 Python 自动化打包脚本，一键生成用于向任意新主机交付的完整安装包。

---

## 2. 镜像包 (`volur-images.tar.gz`) 体积深度剖析

通过对 `volur-images.tar.gz` 内部 `manifest.json` 与各图层 OCI 数据的解包分析：
- **未压缩原始体积总和**：**1.61 GB**；
- **Gzip 压缩后体积**：**521 MB**（综合压缩率达到 68%）。

各组件具体体积与占比明细如下：

| 组件 / 镜像 | 原始体积 (Uncompressed) | 压缩后估算 (Gzip) | 占比 | 深度成因分析 |
| :--- | :--- | :--- | :--- | :--- |
| **`mysql:9.6.0`** | **939 MB** | **~280 MB** | **55%** | 官方基于完整 Oracle Linux 9 / glibc 构建，包含 MySQL 9 服务端、Router、诊断工具及客户端，其中单个核心数据层就达 658MB。 |
| **`volur-app:latest`** | **507 MB** | **~190 MB** | **35%** | 包含 Java 25 JRE 底座系统（382MB）以及包含全套 Spring Boot/Hibernate/国密依赖的 `volur-server.jar`（125MB）。 |
| **`redis:8.6.2-alpine`**| **96.4 MB** | **~32 MB** | **6%** | 基于 Alpine Linux 的轻量级官方 Redis 缓存服务。 |
| **`volur-nginx:latest`**| **70.4 MB** | **~23 MB** | **4%** | 基于 Alpine Linux 的 Nginx 服务（~68MB）与打包后的前端 SPA 静态产物（~2MB）。 |

### 瘦身核心结论
- 若目标机器具备公网访问能力，**无需将 MySQL 和 Redis 打入离线包**，仅打包前后端自有业务镜像（`volur-app` + `volur-nginx`），压缩包体积可从 **521MB 骤降至 ~210MB**（体积降低 60%）。

---

## 3. 交付打包脚本设计 (`scripts/package_delivery.py`)

在 `scripts/` 目录下实现了免依赖、纯 Python 标准库驱动的交付打包工具：
- **功能特性**：
  - 支持 `--mode full`（全量离线包，含全部 4 个镜像，~520MB）与 `--mode slim`（轻量在线包，仅含 2 个业务镜像，~210MB）；
  - 支持 `--build` 参数，在打包前自动调用 `docker compose build` 执行最新源码容器化编译；
  - 自动将 `volur-lite-volur-app` / `volur-lite-volur-nginx` 重命名为标准的 `volur-app:latest` / `volur-nginx:latest`；
  - 自动生成配套的 `docker-compose.yml`（生产纯净版）、`.env` 配置文件、`start.sh`（Linux/macOS 一键拉起）、`start.bat`（Windows 一键拉起）与 `README.md` 操作指南；
  - 自动打包为单一带时间戳的发布 ZIP 归档包（如 `volur-lite-delivery-full-YYYYMMDD.zip`）。
