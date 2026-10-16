# Nginx 生产级加固与客户端 IP/TLS 防伪造重构实施记录

## 1. 变更背景与审计目标
对 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf` 进行了全方位生产环境规范与安全加固审查，重点排查并修复以下 5 大核心隐患：
1. **静态资源安全头缺失**：修复 Nginx `add_header` 作用域覆盖机制导致的静态文件安全响应头丢失问题；
2. **配置严重样板冗余**：消除多个特定限流 location 中重复编写的大量子配置；
3. **WebSocket 映射非规范**：杜绝直接透传 `$http_connection` 引起的非 WebSocket 请求连接头污染；
4. **Upstream 短连接瓶颈**：消除缺乏连接池导致的高并发本地端口耗尽与 502 Bad Gateway 风险；
5. **防 IP 与 TLS 头伪造**：堵死外部攻击者通过伪造 `X-Real-IP`、`X-Forwarded-For` 与 `X-TLS-*` 请求头击穿限流与风控引擎的重大漏洞。

---

## 2. 核心加固与改动点

### 2.1 Upstream 长连接池接入 (`upstream volur_backend`)
- 声明统一后端负载连接池：
  ```nginx
  upstream volur_backend {
      server volur-app:8080 max_fails=3 fail_timeout=10s;
      keepalive 64;
  }
  ```
- 配合默认 `proxy_set_header Connection "";`，复用 Nginx 到 Spring Boot 的底层 TCP 长连接，彻底消除高并发放题/答题阶段频繁三次握手与短连接 TIME_WAIT 堆积隐患。

### 2.2 WebSocket 严谨状态机映射 (`map $http_upgrade $connection_upgrade`)
- 按照 RFC 6455 规范建立动态映射：
  ```nginx
  map $http_upgrade $connection_upgrade {
      default upgrade;
      ''      close;
  }
  ```
- 在 `/api/` 路由中按需升级为 WebSocket，普通 HTTP API 请求不发送无效 Connection 头，保证 HTTP/1.1 长连接稳定。

### 2.3 防伪造：IP 与 TLS 握手特征绝对可信
- **IP 强制覆写**：
  全链路限流直接绑定不可在 TCP 应用层伪造的物理对端 IP `$remote_addr`；
  向后端代理强行覆盖 `proxy_set_header X-Real-IP $remote_addr;` 与 `proxy_set_header X-Forwarded-For $remote_addr;`，彻底剥离客户端伪造的所有 IP 头部。
- **TLS 特征纯原生提取**：
  将 `map $http_x_tls_*` 调整为 `map $ssl_*` 原生映射；彻底丢弃外部请求携带的 `X-TLS-*`，防止自动化黑客脚本伪造 TLS 协议与加密套件欺骗后端风控引擎。

### 2.4 静态资源安全头补齐与全站加固
- 全局补齐现代安全响应头：
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: camera=(), microphone=(), geolocation=()`
- 针对静态资源 location `~* \.(?:ico|css|js|...)` 显式重新声明安全头，防止被 `Cache-Control "public, no-transform"` 覆盖剥离。

### 2.5 架构精简与配置收敛
- 将通用的 `proxy_set_header` 和 `proxy_http_version 1.1` 上提至 `server` 块级别；
- 认证与验证码接口（`/api/v1/auth/login`、`/register`、`/captcha`、`/email/captcha`）消除重复样板代码，仅保留各自独特的 `limit_req` 速率控制与超时参数。
