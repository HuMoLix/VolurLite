# Docker 镜像实时流式打包导出与浏览器直接下载改造施工审计记录

## 1. 变更背景与需求概述
在原有的镜像归档备份实现中，系统通过 `docker save -o <targetPath> <imageName>` 将生成的镜像离线包直接保存在后端服务器的 `data/backups/images/` 磁盘目录下。
这种落地到后端磁盘的设计存在明显缺陷：
1. **磁盘空间无度消耗**：容器镜像归档体积通常达数层几百 MB 甚至数 GB，频繁备份会迅速占满宿主机与服务端物理磁盘空间；
2. **下载流程割裂**：管理员导出镜像的本质意图是将其下载保存至本地运维终端或运维移动介质中，而非在后端留存孤儿大文件；
3. **安全与维护负担**：落盘文件缺乏周期性自动清理机制，容易演变为僵尸垃圾文件。

**本次改造核心目标**：
- **彻底去除后端磁盘落地存储**：后端严禁向 `data/backups/images/` 或任何服务器物理路径写入归档文件；
- **纯内存/流式管道传输 (0 字节磁盘占用)**：后端在接收到导出请求后，直接调度 Docker 引擎执行 `docker save <fullImageName>`（不带 `-o` 参数），直接捕获子进程标准输出（STDOUT）并在 64KB 高速缓冲区分块中直接泵入 HTTP 响应体 `HttpServletResponse.getOutputStream()`；
- **前端直链下载体验**：前端通过专用强类型下载接口获取二进制流，利用 `Blob` 与 `URL.createObjectURL` 触发浏览器本地文件原生保存；
- **前后端接口与规范 100% 严格对齐**。

---

## 2. 关键改动点说明

### 2.1 后端工程改动 (volur-container)

1. **接口定义与契约扩展 (`AdminImageService.java`)**：
   - 新增方法声明：`void exportImage(Long id, HttpServletResponse response)`；
   - 严格遵循 Volur 平台服务层规范：命令与动作类方法返回 `void`，通过传入的响应句柄实现受控流式交付。

2. **流式导出实现与落盘逻辑废除 (`AdminImageServiceImpl.java`)**：
   - **废除并清理旧落地存储逻辑**：
     - 从 `backupImage` 中彻底移除调用 `triggerDockerSaveArchive` 将 `.tar` 写入 `data/backups/images` 的逻辑；
     - 当收到 `backupType === 'ARCHIVE'` 的老式备份请求时，显式抛出业务异常提示已升级为直接流式下载；
     - 彻底删除私有方法 `triggerDockerSaveArchive(String imageName, String targetPath)`；
     - 清理磁盘上历史残留的测试归档目录与文件。
   - **实现高可靠动态流式导出 `exportImage`**：
     - 根据镜像 ID 查询数据库实体，依据 `nodeIps` 智能选择部署该镜像的节点（本地主节点或子节点 Remote Docker API）；
     - 构建命令行 `docker [-H <remoteHost:2375>] save <fullImageName>`；
     - 设置标准响应头：
       - `Content-Type: application/x-tar`
       - `Content-Disposition: attachment; filename="<repo>_<tag>.tar"; filename*=UTF-8''<encodedFilename>`（RFC 5987 编码规范，防止特殊字符或中文转义异常）；
     - **异步 STDERR 消费**：启动后台守护线程及时消费子进程的 `getErrorStream()`，杜绝因子进程错误输出缓冲区填满而导致的管道死锁现象；
     - **64KB 高性能流式传输**：采用 `byte[65536]` 缓冲区，直接将 `process.getInputStream()` 写入 `response.getOutputStream()`；
     - **资源防御与孤儿进程清理**：在 `finally` 代码块中确保若客户端中断下载（如用户主动取消或网络中断导致 `Broken pipe`），强制执行 `process.destroyForcibly()`，避免后端残留僵尸 `docker save` 进程。

3. **Controller 层端点声明与审计 (`AdminImageController.java`)**：
   - 新增端点：`GET /api/v1/admin/images/{id}/export`；
   - 添加 `@Operation(summary = "导出 Docker 镜像归档文件 (.tar) 并提供浏览器直接流式下载")`；
   - 添加 `@Syslog(module = "IMAGE", value = "导出 Docker 镜像")` 审计注解（经验证，`SyslogAspect` 已针对 `HttpServletResponse` 和 `OutputStream` 进行了参数排除，保证审计安全正常入库）。

---

### 2.2 前端工程改动 (volur-lite-view)

1. **强类型 Axios 请求实例增强 (`src/utils/request.ts`)**：
   - 在 `request` 工具对象中增加 `download(url: string, config?: AxiosRequestConfig): Promise<Blob>` 方法；
   - 默认配置 `responseType: 'blob'` 且 `timeout: 0`（大型镜像打包传输耗时较长，避免触发普通 API 15 秒超时熔断）；
   - 在响应拦截器中增强错误解析：当底层返回 4xx/5xx 时，如果 `error.response.data` 为 `Blob`，自动尝试读取 `Blob` 文本并解析 JSON 中的 `message`，确保用户能准确看到业务错误提示。

2. **API 模块定义 (`src/api/image/index.ts`)**：
   - 导出 `exportAdminImageApi(id: string | number): Promise<Blob>`，与后端 `GET /api/v1/admin/images/{id}/export` 严格 1:1 对齐。

3. **视图与交互更新 (`src/views/admin/image/AdminImageView.vue`)**：
   - **表格操作列新增“导出”按钮**：
     - 在 `imageColumns` 操作列中新增“导出”按钮（采用 `DownloadOutline` 图标）；
     - 增加 `exportingImageId` 状态管理，导出时按钮显示 Spin 加载态并暂时禁用其余导出；
     - 调整操作列宽度为 `310px`，确保 5 个操作按钮（详情、转移、备份、导出、删除）排列整齐舒展；
   - **流式下载函数 `handleExportImage`**：
     - 触发时弹出常驻提示 `message.loading('正在实时打包导出镜像 ...', { duration: 0 })`；
     - 接收到后端二进制 `Blob` 后，通过 `window.URL.createObjectURL(blob)` 生成临时资源链接；
     - 动态挂载虚拟 `<a>` 标签并设置 `download = "${safeRepo}_${tag}.tar"` 自动触发浏览器保存；
     - 执行后即时释放 ObjectURL 并展示成功提示；
   - **备份弹窗 (Backup Modal) 升级联动**：
     - 在弹窗内切换到“导出下载离线归档包 (.tar 下载)”模式时，提交按钮自动切换为“立即导出下载”；
     - 确认点击后直接调用 `handleExportImage` 启动流式下载，提示明确告知不占用后端存储。

4. **双语国际化文案完善 (`zh_CN/admin/images.json` & `en_US/admin/images.json`)**：
   - 补充 `table.export`、`backupModal.exportSuccess` 等对应中英文字段；
   - 更新 `backupModal.archiveNotice` 文案，准确描述流式传输特性。

---

## 3. 架构优势与安全可靠性

| 对比项 | 改造前 (落盘模式) | 改造后 (流式传输模式) |
| :--- | :--- | :--- |
| **服务器磁盘占用** | 每次备份消耗镜像等量磁盘空间 (数百 MB ~ 数 GB) | **0 字节**，完全不在服务器落地 |
| **I/O 性能损耗** | Docker -> 服务器磁盘写 -> (人工提取/二次传输) | Docker 管道 -> HTTP Socket 直写，极低 I/O 延迟 |
| **异常防护** | 磁盘写一半中断留下半截孤儿坏文件 | 客户端断开连接触发异常，子进程强制 `destroyForcibly` 立即销毁 |
| **管道死锁预防** | 未防范 STDERR 满溢可能阻塞父进程 | 独立守护线程持续排空 STDERR，安全可靠 |
| **用户体验** | 仅通知“已备份到服务器”，用户无法直接取得文件 | 点击后浏览器直接弹出 `.tar` 保存窗口，所见即所得 |

---

## 4. 验证与测试结果

1. **后端编译验证**：
   - 执行 `mvn clean test-compile`，`volur-container` 以及整个 `volur-lite-platform` 共 12 个模块全部构建成功（`BUILD SUCCESS`），0 语法警告与类型错误。
2. **前端构建验证**：
   - 执行 `npm run build` (`vue-tsc --noEmit && vite build`)，Vue 组件模板与 TypeScript 类型检查全部通过，产物成功输出至 `dist/`。
3. **本地 Docker 镜像导出测试**：
   - 宿主机测试 `docker save` POSIX tar 流输出，格式标准无损。
