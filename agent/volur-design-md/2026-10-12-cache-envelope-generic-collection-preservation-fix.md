# 缓存信封集合泛型类型保留与反序列化 ClassCastException 修复方案

## 1. 变更背景与问题溯源

在访问 `/competitions/{slug}/ctf`（如 `/competitions/test/ctf`）接口获取赛题列表时，系统抛出如下异常：
```
java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO
	at com.humolix.volur.competition.service.ctf.impl.CtfCompetitionServiceImpl.listChallenges(CtfCompetitionServiceImpl.java:89)
```

### 根因分析
1. **泛型擦除与归一化缺陷**：
   在 `CtfCompetitionServiceImpl.listChallenges` 中，使用 `cacheTemplate.getOrLoad` 缓存底座题目列表。底层缓存组件 `VolurCache` 写入信封 `CachedValue` 时，`normalizeType` 对 `Collection` / `List` 仅记录了接口类型全限定名（如 `"java.util.List"`），集合内元素的类型信息因泛型擦除而丢失。
2. **Jackson 反序列化默认退化**：
   从 Redis (L2) 读取缓存或从信封反序列化时，因缺乏元素类型指示，Jackson 执行 `parseObject(json, List.class)` 会将 JSON 数组中的每个对象默认反序列化为 `LinkedHashMap<String, Object>`。
3. **动静分离迭代触发 checkcast**：
   为实现题目底座共享并安全派生战队解题状态（`isSolvedByMyTeam`），`listChallenges` 在第 89 行对 `cachedBaseList` 执行增强 `for (CtfCompetitionChallengeVO vo : cachedBaseList)` 迭代，JVM 执行 `checkcast` 指令时因元素实际为 `LinkedHashMap` 而崩溃。

---

## 2. 改动点与设计实现

遵循方案 A，在框架层底层 `CachedValue` 与 `JsonUtils` 中增强对集合元素类型的自动探测、安全校验与强类型泛型反序列化能力。

### 2.1 JsonUtils 增强 (`JsonUtils.java`)
- 扩展 `parseSet(String json, Class<T> elementClass)` 方法，利用 Jackson `getTypeFactory().constructCollectionType(Set.class, elementClass)` 支持 `Set` 集合的强类型反序列化；与原有的 `parseList` 共同构成标准集合反序列化设施。

### 2.2 CachedValue 结构扩展与自动元素类型提取 (`CachedValue.java`)
1. **信封数据结构升级**：
   扩展 `CachedValue` 记录为 `CachedValue(boolean nullValue, String type, String elementType, String json)`。保留兼容构造器 `CachedValue(boolean nullValue, String type, String json)`。
2. **运行时元素类型探测 (`resolveElementType`)**：
   在 `CachedValue.of(Object value)` 时，当 `value instanceof Collection<?> coll`，自动探测第一个非空元素的类全限定名作为 `elementType`。
3. **信封编解码兼容 (`encode` / `decode`)**：
   - 编码时若 `elementType != null`，向 JSON 增加 `"e": elementType` 字段；
   - 解码时安全解析 `node.hasNonNull("e") ? node.get("e").asText() : null`，具备对无 `"e"` 历史存量缓存数据的完全向后兼容能力。
4. **强类型集合解包 (`unwrap`)**：
   当 `elementType != null` 且目标类型继承自 `Collection` 时：
   - 先对 `elementType` 进行安全白名单校验（`resolveType`）；
   - 若目标类型为 `Set` 则路由至 `JsonUtils.parseSet(json, elementTarget)`；
   - 其余路由至 `JsonUtils.parseList(json, elementTarget)`，彻底恢复泛型元素对象。

---

## 3. 安全防护与回归验证

1. **AutoType 与类加载反序列化白名单安全**：
   - `elementType` 在解包还原为 `Class<?>` 时，严格复用已有的 `resolveType` 白名单校验逻辑（`ALLOWED_PACKAGE_PREFIXES`），杜绝恶意构造带有非受信类的 `e` 字段进行 RCE。
2. **测试用例覆盖**：
   - 在 `CachedValueTest` 中新增 `pojoListPreservesElementType`、`pojoSetPreservesElementType` 以及 `rejectsElementTypeOutsideWhitelist` 用例，验证 POJO 元素完整保留且非法类型被拦截；
   - 在 `CtfCompetitionCacheTest` 中新增 `testListChallengesWithCachedValueRoundTrip` 用例，模拟真实 `CachedValue` 往返序列化，验证 `listChallenges` 迭代与派生不再发生任何 `ClassCastException`；
   - `volur-framework`（191 个测试）与 `volur-competition`（45 个测试）全部通过。
