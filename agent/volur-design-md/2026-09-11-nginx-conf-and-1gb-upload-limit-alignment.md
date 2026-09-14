# Nginx 配置深度优化与全链路 1GB 文件上传限制放开审计文档

## 1. 变更背景与需求
为了支持 Volur Lite 容器化生产部署，并满足管理员上传大体积 CTF 赛题附件、Docker 镜像存档及系统数据备份包的需求，需要：
1. 优化 `src/volur-lite-service/nginx/conf/nginx.conf` 核心网关配置，解决静态扩展名正则劫持与 SSE 实时流缓冲断连问题；
2. 将 Nginx 接入层、Spring Boot 容器层、业务存储服务层（StorageService）的文件上传上限统一放宽至 **1GB (1024MB)**。

---

## 2. 核心改动点与实现细节

### 2.1 Nginx 网关配置优化 (`src/volur-lite-service/nginx/conf/nginx.conf`)
- **放宽请求体上限**：配置 `client_max_body_size 1024M;`；
- **防静态正则劫持**：将 `location /api/` 改造为 `location ^~ /api/`。利用 `^~` 阻断机制，防止 `location ~* \.(?:png|jpg|...)` 优先拦截用户上传的头像或赛题图片导致 404；
- **SSE 实时长连接专属通道**：
  - 新增 `location = /api/v1/realtime/stream`；
  - 显式关闭缓冲与缓存：`proxy_buffering off;`、`proxy_cache off;`、`chunked_transfer_encoding on;`；
  - 超时时间设置为 24 小时（`proxy_read_timeout 86400s;`），杜绝 60 秒频繁断开重连；
- **真实客户端 IP 规范**：改用 `$proxy_add_x_forwarded_for`，兼容多层代理/CDN 链路；
- **超时保护**：针对大文件上传场景，将 API 代理的读写超时时间由 60 秒延长至 300 秒（`proxy_send_timeout 300s;`、`proxy_read_timeout 300s;`）。

### 2.2 Spring Boot Multipart 上限调整 (`application.yml`)
- 在 `volur-server` 的 `application.yml` 中调整 Servlet Multipart 约束：
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 1024MB
        max-request-size: 1024MB
  ```

### 2.3 业务存储服务校验调整 (`StorageServiceImpl.java`)
- 调整通用资源（附件/包）的最大限制常量：
  - 原值：`100L * 1024 * 1024` (100MB)
  - 新值：`1024L * 1024 * 1024` (1GB)
- 优化文件大小校验异常提示，支持根据阈值自动以 GB/MB 为单位显示：
  ```java
  String limitDesc = maxSize >= 1024L * 1024 * 1024
          ? (maxSize / (1024L * 1024 * 1024)) + "GB"
          : (maxSize / (1024L * 1024)) + "MB";
  throw new BusinessException(HttpStatus.BAD_REQUEST, "文件大小超过限制: " + limitDesc);
  ```

---

## 3. 影响范围与验证结果
- **构建编译验证**：执行 `mvn test-compile`，全平台 12 个模块全部编译通过（`BUILD SUCCESS`）。
- **影响范围**：
  - 管理员赛题附件上传接口 (`/api/v1/system/upload/resource`) 上限正式提升至 1GB；
  - 普通图片上传（头像、图标）保持 5MB 独立校验，保证系统安全；
  - 前端 SSE 实时推送流不再受 Nginx 缓冲影响，保障即时通信。
