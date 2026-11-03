# 公开系统配置接口递归更新异常修复记录

## 1. 变更背景

前端及外部客户端调用公开系统配置接口 `GET /api/v1/system/settings/public` 时，服务端偶发或稳定返回 HTTP 400 异常：

```json
{
    "status": 400,
    "errorCode": 0,
    "success": false,
    "message": "Recursive update",
    "data": null,
    "timestamp": "1793617011039",
    "path": "/api/v1/system/settings/public"
}
```

该接口为全平台前端应用（`volur-lite-view` 与 `volur-lite-view-NEXT`）初始化必调的基础接口。接口返回 400 会导致全站基本配置（Logo、网站标题、维护状态、ICP 备案号等）加载失败。

---

## 2. 根因分析 (Root Cause)

1. **缓存计算重入冲突**：
   - 在 [`SystemSettingServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java) 的 `getPublicSettingsMap()` 方法中，使用了多级缓存模板 `cacheTemplate.getOrLoad(CacheNames.SYSTEM_SETTING, PUBLIC_SETTINGS_MAP_CACHE_KEY, () -> ...)`；
   - 一级缓存由 Caffeine（底层基于 `java.util.concurrent.ConcurrentHashMap`）管理，在计算尚未就绪的值时，会将桶节点置为占位计算状态（`ReservationNode`）；
   - 在该回源 Lambda 表达式内部，遍历 `systemSettingRepository.findByIsPublicTrue()` 结果时，错误地对每个配置项调用了 `getSettingValue(setting.getSettingKey(), setting.getSettingValue())`；
   - `getSettingValue` 方法内部同样调用了 `cacheTemplate.getOrLoad(CacheNames.SYSTEM_SETTING, key, () -> ...)`，即在同一线程、同一 Caffeine 缓存实例的计算任务中，再次尝试对同一 Map 实例进行计算重入；
   - 当遇到哈希桶重叠或状态检查时，JDK 底层 `ConcurrentHashMap` 触发递归计算保护，直接抛出 `java.lang.IllegalStateException("Recursive update")`。

2. **异常转换与 HTTP 状态码映射**：
   - `VolurCacheTemplate` 还原了底层抛出的原始异常 `IllegalStateException`；
   - [`GlobalExceptionHandler.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 中 `@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})` 将其捕获并标注为 `@ResponseStatus(HttpStatus.BAD_REQUEST)`，从而输出了带有 `"message": "Recursive update"` 的 400 响应。

---

## 3. 详细改动点

1. **消除缓存重入调用**：
   - 在 [`SystemSettingServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java) 中新增私有方法 `resolveEntityValue(SystemSettingEntity entity)`；
   - 在 `findByIsPublicTrue()` 流式处理中直接从实体属性 `entity.getSettingValue()` 或环境配置属性 `environment.getProperty(...)` 解析最终字符串值，严禁在 `cacheTemplate.getOrLoad` 回调内部重入 `cacheTemplate`。
2. **规范常量引用与清理冗余注释**：
   - 显式导入 `SystemSettingKeys` 常量类，替换类内全限定类名；
   - 清理类中零注释铁律禁止的行内解释性注释。
3. **补齐单元测试**：
   - 在 [`pom.xml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/pom.xml) 中引入 `spring-boot-starter-test`（`scope: test`）；
   - 新增 [`SystemSettingServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/test/java/com/humolix/volur/system/service/SystemSettingServiceImplTest.java)，验证 `getPublicSettingsMap()` 回源逻辑不会产生二级缓存递归调用，并验证前缀过滤及维护状态注入正确。

---

## 4. 影响范围与验证结果

- **影响模块**：`volur-system`
- **编译与测试验证**：
  - 执行 `mvn -f src/volur-lite-platform/pom.xml test -pl volur-system -am` 编译并运行测试，全部 1 个测试用例通过（`BUILD SUCCESS`）；
  - 彻底封堵了由于 Caffeine / `ConcurrentHashMap` 递归计算引发的 HTTP 400 故障。
