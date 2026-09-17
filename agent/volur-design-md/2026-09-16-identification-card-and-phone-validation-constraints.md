# 身份证与手机号 Bean Validation 约束注解设计与实现规范文档

**记录日期**：2026-09-16  
**作者/执行者**：Agent (Antigravity)  
**涉及模块**：
- `volur-framework`：通用校验注解、运营商枚举与校验器实现
- `volur-competition`：竞赛报名人员数据传输对象 (`TeamMemberSignupDTO`) 与单元测试
- `volur-lite-view`：前端校验工具函数库 (`src/utils/validation.ts`)、战队报名弹窗 (`TeamFormModal.vue`) 与多语言国际化配置

---

## 1. 变更背景与需求概述

在 Volur Lite 平台的竞赛管理模块中，战队报名及成员名册提交流程中包含参赛人员的二代居民身份证号（`idCard`）与手机号（`phone`）等关键身份信息。
原有的 `TeamMemberSignupDTO` 中仅对 `idCard` 与 `phone` 配置了 `@NotBlank` 非空校验，而对电子邮箱 `email` 则同时应用了 `@NotBlank` 与 `@Email` 格式校验；前端 `TeamFormModal.vue` 原先仅使用松散正则 `/^1[3-9]\d{9}$/` 且对身份证号码未做格式校验。这导致前端或恶意客户端提交非法格式的身份证号码（如校验码不匹配、非法出生日期、虚假省份代码）或非法的手机号时，无法在前后端形成一致性的严格预校验拦截，给业务数据质量及真实性带来隐患。

为此，依据平台前后端校验规范（`view-Rules.md` 铁律 8），参考 `jakarta.validation.constraints.Email` 的设计模式，进行前后端 1:1 严格对齐设计与实现：
1. **后端校验体系**：
   - **`@IdentificationCard`**：中国大陆第二代居民身份证有效性校验注解（GB 11643-1999）；
   - **`@Phone`**：中国大陆手机号码有效性校验注解（严格限定中国移动、中国联通、中国电信、中国广电四大基础电信运营商号段）；
   - 将上述两项校验注解应用于 `TeamMemberSignupDTO` 中，并补齐单元测试。
2. **前端校验体系**：
   - 新增通用校验库 `src/utils/validation.ts`，导出 `isValidIdCard`、`isValidChinaPhone`、`detectChinaCarrier` 等；
   - 在 `TeamFormModal.vue` 中对成员名册的手机号与身份证进行提交前严格前置校验；
   - 补充中英文国际化文案 `validation_id_card_format`。

---

## 2. 架构设计与详细实现

### 2.1 架构分层设计
身份证与手机号校验是全平台通用的核心校验能力（用户中心 `volur-user`、竞赛中心 `volur-competition`、认证中心 `volur-auth` 等模块均有强需求）。因此将注解定义与校验器下沉收敛至全局基础设施库 `volur-framework`：
- `com.humolix.volur.framework.common.validation.enums.ChinaCarrier`
- `com.humolix.volur.framework.common.validation.annotation.IdentificationCard`
- `com.humolix.volur.framework.common.validation.annotation.Phone`
- `com.humolix.volur.framework.common.validation.validator.IdentificationCardValidator`
- `com.humolix.volur.framework.common.validation.validator.PhoneValidator`

同时在前端新增与后端逻辑 100% 镜像对齐的 TypeScript 模块：
- `src/utils/validation.ts`：包含四大运营商正则检测与二代身份证行政区划、平闰年生日、加权模 11 校验码算法。

### 2.2 中国二代身份证校验规范 (GB 11643-1999)
前后端严格遵循国家标准 **GB 11643-1999**（公民身份号码）：
1. **长度与正则预检**：必须为 18 位定长；前 17 位为数字，第 18 位为数字或 `X`/`x`；首位不得为 0；
2. **行政区划代码合法性**：依据 GB/T 2260 校验前两位省级行政区划代码（包含 34 个省级行政区代码：11-15、21-23、31-37、41-46、50-54、61-65、71、81、82）；
3. **出生日期合法性**：提取第 7~14 位 `YYYYMMDD`，基于平年/闰年日历（Java `LocalDate.of`，前端 `Date` 年月日比对）校验平年/闰年、月（01-12）、日（01-31）有效性，拦截 1900 年以前超期年份与未来日期；
4. **ISO 7064:1983.MOD 11-2 加权校验码计算**：
   - 权重向量：`[7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]`
   - 余数映射：`['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']`
   - 对比第 18 位字符（忽略大小写）。
5. **Bean Validation 与前端规范**：后端对 `null` 或全空白字符放行（返回 `true`），由 `@NotBlank` 承担非空职责；前端在非空通过后执行格式校验。

### 2.3 四大运营商手机号校验规范
严格限定中国四大基础电信业务经营商（中国移动、中国联通、中国电信、中国广电）正式分配的公众移动通信号段，并排除虚拟运营商（MVNO 170/171/162/165/167）等非四大发行商号段：
- **中国移动**：`134[0-8]`, `135-139`, `147`, `148`, `150-152`, `157-159`, `172`, `178`, `182-184`, `187-188`, `195`, `197`, `198`；
- **中国联通**：`130-132`, `145`, `146`, `155`, `156`, `166`, `175`, `176`, `185`, `186`, `196`；
- **中国电信**：`133`, `1349`, `149`, `153`, `173`, `174`, `177`, `180`, `181`, `189`, `190`, `191`, `193`, `199`；
- **中国广电**：`192`；
- **运营商范围配置**：支持注解属性 `carriers()` 指定允许的运营商范围（默认为 `ChinaCarrier.ALL`），并提供 `detectCarrier(phone)` 静态工具方法供业务层复用。

### 2.4 DTO 字段应用 (`TeamMemberSignupDTO`)
在 `TeamMemberSignupDTO` 中：
```java
    @NotBlank(message = "手机号不能为空")
    @Phone(message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "电子邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    @IdentificationCard(message = "身份证号格式不正确")
    private String idCard;
```

### 2.5 前端表单前置校验与归一化处理 (`TeamFormModal.vue`)
- 在提交前遍历 `members` 数组，在提示非空后分别调用 `isValidChinaPhone(phone)` 与 `isValidIdCard(idCard)` 进行强格式核验；
- 提交前将身份证号码进行 `.toUpperCase()` 归一化，确保合法的小写 `x` 在提交 DTO 时转为标准大写 `X`。

---

## 3. 测试覆盖与验证

1. **`IdentificationCardValidatorTest`**：
   - 覆盖合法数字尾号、合法 X/x 尾号二代身份证；
   - 覆盖出生日期提取、性别判定提取（第 17 位奇数男、偶数女）；
   - 覆盖 15 位一代证、长度异常、非法字符、非法省份代码、非法闰年、未来日期、校验码不匹配等全部反例；
   - 覆盖 `null` 与空白字符测试。
2. **`PhoneValidatorTest`**：
   - 覆盖中国移动、中国联通、中国电信、中国广电四大运营商核心号段，验证格式与运营商探测；
   - 覆盖 170/171/162/165/167 等虚拟运营商号段及非法长度/字符号码拦截；
   - 覆盖指定运营商集合过滤能力；
   - 覆盖 `null` 与空白字符测试。
3. **`TeamMemberSignupDTOValidationTest`**：
   - 完整 DTO 实体级 Jakarta Validator 校验；
   - 验证合法信息通过校验，非法手机号与非法身份证号分别精准触发对应错误文案；
   - 验证空白字符同时触发 `@NotBlank`。
4. **前端构建与类型校验**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit`），全工程 TypeScript 编译与 Vite 生产打包 0 错误通过。

---

## 4. 影响范围与审计结论

- **前后端一致性**：与前端 `TeamFormModal.vue` 中的手机与身份证校验形成严密闭环，防止接口被绕过；
- **向下兼容性**：遵循标准 Jakarta Validation 规范，现有合法数据与接口请求均不受破坏；
- **平台可复用性**：新增的校验注解作为框架公共组件，可在后续用户认证、实名认证等业务中无缝推广。
