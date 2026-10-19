# Nginx 全站 HTTPS 生产级加固与 GitHub 仓库同步隔离方案

## 1. 变更背景与架构目标

1. **HTTPS 生产级终结与全链路加固诉求**：
   原 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf` 中，全站流量默认仅运行在 HTTP (80) 端口，底部关于 HTTPS (443) 的配置仅为残缺的注释块。若直接解开注释，缺少所有的路由代理（API 反向代理、SSE 实时推送、WebSocket 握手、静态文件缓存及认证接口防爆破限流），无法满足生产环境对全站加密通信、合规性与安全防护的要求。
2. **私有配置与公网开源交付解耦（本地仓库独立维护）**：
   平台已将通用的反向代理模版沉淀为 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf.example` 并提交至公共开源分支。对于包含具体域名证书路径、HSTS 策略及本地私有配置的 `nginx.conf`，必须实现“仅本地仓库提交”，在执行 `scripts/git/github.sh` 同步至公共 GitHub 仓库时自动剥离并加入忽略清单，杜绝敏感配置泄露。

---

## 2. 核心改动清单

### 2.1 GitHub 导出脚本隔离 (`scripts/git/github.sh`)
- 在孤儿临时分支剥离列表与自动注入的 `.gitignore` 中，追加以下路径：
  - `src/volur-lite-view-NEXT/nginx/conf/nginx.conf`
  - `src/volur-lite-view-NEXT/nginx/ssl`
- 确保导出至 GitHub 的分支中仅保留 `nginx.conf.example` 模版，私有配置文件与本地 TLS 凭据物理隔离在本地仓库。

### 2.2 Nginx HTTPS (443) 生产级完善与加固 (`nginx.conf`)
1. **HTTP (80) 智能重定向**：
   - 监听 80 端口，全量请求执行 `301 https://$host$request_uri` 永久重定向；
   - 显式保留 `location ^~ /.well-known/acme-challenge/`，保证 Let's Encrypt / Certbot 自动化证书申请与续签顺利完成。
2. **现代 TLS 协议与密码套件收敛**：
   - 采用标准 `listen 443 ssl;` 与现代 `http2 on;` 指令；
   - 强制收敛 TLS 协议版本为 `TLSv1.2 TLSv1.3`；
   - 采用兼顾高安全性与兼容性的 Mozilla 推荐密码套件，关闭 `ssl_prefer_server_ciphers`（TLS 1.3 优先由客户端选择最优套件）；
   - 开启 `ssl_session_cache shared:SSL:10m` 与 `ssl_session_timeout 1d`，关闭 `ssl_session_tickets`，将 `ssl_buffer_size` 优化为 `4k` 以降低首包到达时间（TTFB）。
3. **全站现代安全响应头**：
   - 补充 `Strict-Transport-Security: max-age=31536000; includeSubDomains`（HSTS）；
   - 在主服务、`= /index.html` 以及静态资源正则匹配块中统一声明防护响应头（`X-Frame-Options`、`X-XSS-Protection`、`X-Content-Type-Options`、`Referrer-Policy`、`Permissions-Policy`），规避 Nginx `add_header` 继承作用域覆盖隐患。
4. **后端 TLS 特征与凭据透传**：
   - 向后端 Java 服务注入 `X-Forwarded-Proto https`、`X-Forwarded-Ssl on` 与 `X-Forwarded-Port 443`，触发后端 `AuthCookieService` 将 JWT 认证 Cookie 标定为 `Secure`；
   - 原生提取 `$effective_tls_*` 变量（包括 Protocol, Cipher, Curves, ALPN, Session-Reused），透传给后端风控研判引擎。
5. **全量业务路由 100% 对齐**：
   - 将 SPA 静态资源路由（`try_files $uri $uri/ /index.html`）、SSE 长连接（`chunked`、超时 86400s、禁用缓存与缓冲）、认证防爆破限流接口（`login`、`register`、`captcha`、`email/captcha`）及通用的 `/api/` 反向代理完整置入 443 虚拟主机中。

---

## 3. 验证与测试

1. **Nginx 语法核验**：
   在隔离容器中执行 `docker run --rm ... nginx:alpine nginx -t`：
   - 输出：`nginx: the configuration file /etc/nginx/nginx.conf syntax is ok`
   - 输出：`nginx: configuration file /etc/nginx/nginx.conf test is successful`
2. **导出流水线校验**：
   - `scripts/git/github.sh` 已配置剥离 `nginx.conf` 与 `nginx/ssl`，后续执行脚本时，公共 GitHub 分支将不受私有 HTTPS 变更干扰。
