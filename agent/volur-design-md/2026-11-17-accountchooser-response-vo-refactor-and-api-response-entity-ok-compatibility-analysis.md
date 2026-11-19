# AccountChooserResponseVO 重构与 ApiResponseEntity ok 静态重载技术边界分析

## 1. 变更背景与架构动因

本期改动针对以下两项设计与规范议题进行重构及深入技术可行性论证：

1. **`AccountChooserProbeResponseVO` 重命名为 `AccountChooserResponseVO`**：
   - 伴随登录认证流从早期探测式（Probe）向标准 RESTful 资源获取架构的演进，实体命名去冗余化，彻底去除 `Probe` 动词标识，统一为 `AccountChooserResponseVO`；
   - 严格遵循全链路彻底重构铁律，同步完成 Controller、Service、ServiceImpl、单元测试以及前端 NEXT（`flow.ts`、`auth.ts`）的 1:1 强类型对齐。

2. **`ApiResponseEntity` 中是否能将 `okData` 重命名为 `ok` 的技术可行性论证**：
   - 深入剖析 Java 语言规范（JLS）在继承体系下的静态方法隐藏（Static Method Hiding）机制与泛型协变约束，明确说明无法将 `okData(T data)` 改为 `ok(T data)` 的底层根因。

---

## 2. 深入技术分析：ApiResponseEntity 能否将 okData 改造为 ok

### 2.1 结论
**不能。** 在当前的继承架构下（`ApiResponseEntity<T> extends ResponseEntity<ApiResponse<T>>`），无法声明 `public static <T> ApiResponseEntity<T> ok(T data)`。

### 2.2 底层根因剖析 (JLS 规范与编译阻断)

#### 1. 父类 `ResponseEntity` 的既有静态签名
Spring Framework 核心基类 `org.springframework.http.ResponseEntity` 中已定义以下公开静态泛型方法：
```java
public static <T> ResponseEntity<T> ok(T body) {
    return ok().body(body);
}
```

#### 2. Java 静态方法隐藏与返回类型协变规则
根据 **Java 语言规范 (JLS §8.4.8.2 Hiding by Class Methods)**：
> "If a class declares a static method m, then the declaration m is said to hide any method m', where the signature of m is a subsignature of the signature of m' as declared in or inherited by the superclasses and superinterfaces of the class... It is a compile-time error if a static method of a class could hide another method, but the return type of the static method is not compatible with the return type of the other method according to §8.4.5."

当子类 `ApiResponseEntity` 尝试声明同名且入参为 `<T>(T)` 的静态方法时：
```java
public static <T> ApiResponseEntity<T> ok(T data)
```
该方法会尝试隐藏父类 `ResponseEntity.<T>ok(T)`。此时，编译器强制要求子类静态方法的返回类型必须与父类方法的返回类型保持**协变兼容（Return-type-substitutable）**。

#### 3. 泛型不变性导致的类型不兼容
`ApiResponseEntity<T>` 的类定义为：
```java
public class ApiResponseEntity<T> extends ResponseEntity<ApiResponse<T>>
```
- `ApiResponseEntity<T>` 是 `ResponseEntity<ApiResponse<T>>` 的子类型；
- 在 Java 泛型不变性规则下，`ResponseEntity<ApiResponse<T>>` **绝不是** `ResponseEntity<T>` 的子类型（因为类型实参 `ApiResponse<T>` 与 `T` 并不相同）；
- 因此，`ApiResponseEntity<T>` 无法作为 `ResponseEntity<T>` 的协变返回类型替代。

#### 4. javac 编译器实测结果
当在 `ApiResponseEntity.java` 中将 `okData(T data)` 改为 `ok(T data)` 并执行编译时，javac 立即抛出编译失败阻断：
```text
[ERROR] com.humolix.volur.framework.common.api.ApiResponseEntity 中的 <T>ok(T) 无法隐藏 org.springframework.http.ResponseEntity 中的 <T>ok(T)
  返回类型 com.humolix.volur.framework.common.api.ApiResponseEntity<T> 与 org.springframework.http.ResponseEntity<T> 不兼容
```

### 2.3 为什么其他工厂方法未发生冲突？

1. **`ok(String messageKey, T data)`**：
   - 入参签名是 `(String, T)`。父类 `ResponseEntity` 并未声明过 `ok(String, T)` 静态方法，因此该方法属于子类的**全新方法重载（Overload）**，不触发隐藏父类静态方法的逻辑，完全合法。
2. **`created(T data)`**：
   - 父类 `ResponseEntity` 中的静态方法签名为 `created(URI location)`，入参为 `java.net.URI` 实体，与泛型 `(T data)` 签名不重合，不构成同名同参数隐藏，因此能够正常共存。
3. **`success(String messageKey)`**：
   - 避免了 `ok(String messageKey)` 在入参为 `String` 时与 `ok(T data)` 产生重载混淆，同时语义上明确表示无数据返回（`ApiResponseEntity<Void>`）。

### 2.4 架构建议
保持当前设计基准：
- 单参数携带数据统一使用：`ApiResponseEntity.okData(data)`
- 双参数携带国际化键与数据统一使用：`ApiResponseEntity.ok(messageKey, data)`
- 无数据成功响应统一使用：`ApiResponseEntity.success(messageKey)`

---

## 3. 改动实施清单

### 3.1 后端领域实体与服务层重构
1. **重命名实体类**：
   - `volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/AccountChooserProbeResponseVO.java` -> `AccountChooserResponseVO.java`
   - 更新类声明：`public class AccountChooserResponseVO implements Serializable`。
2. **控制器层**：
   - `SigninController.java`：更新 `getAccounts` 接口返回类型与泛型声明为 `ApiResponseEntity<AccountChooserResponseVO>`。
3. **服务层契约与实现**：
   - `SigninService.java`：更新方法签名 `AccountChooserResponseVO getAccounts(AccountChooserProbeRequestDTO request)` 及无参默认重载。
   - `SigninServiceImpl.java`：实现方法返回值与内部 Builder 构造统一变更为 `AccountChooserResponseVO`。
4. **单元测试与回归用例**：
   - `SigninControllerTest.java`：更新 Controller Mock 返回值为 `AccountChooserResponseVO`；
   - `SigninServiceImplTest.java`：更新 Service 单元断言类型为 `AccountChooserResponseVO`。

### 3.2 前端类型定义与 API 契约对齐
1. **类型层 (`flow.ts`)**：
   - 重命名 `AccountChooserProbeResponseVO` 为 `AccountChooserResponseVO`。
2. **API 请求层 (`auth.ts`)**：
   - `getAccountChooserApi` 响应泛型统一更新为 `Promise<ApiResponse<AccountChooserResponseVO>>`。

---

## 4. 验证与审计结果

1. **后端验证**：
   - 执行 `mvn test -f src/volur-lite-platform/pom.xml -pl volur-auth -am`；
   - `volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-security`, `volur-notification`, `volur-auth` 均顺利构建并通过全部 64 项测试。
2. **前端验证**：
   - 执行 `npm run build --prefix src/volur-lite-view-NEXT`（包含 `vue-tsc --noEmit` 与 Vite 生产构建），全量编译通过。
