package com.humolix.volur.framework.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L2 缓存值信封。
 *
 * <p><b>为什么需要信封而不是直接存 payload？</b> 三个原因：
 * <ol>
 *   <li><b>区分"缓存了 null"与"没有缓存"</b>——前者是防穿透的关键（避免反复回源查询不存在的数据），
 *       后者是普通 miss。若只用 payload 是否为 null 判断，两者无法区分。</li>
 *   <li><b>携带类型信息</b>——L2 是跨进程的，反序列化必须知道目标类型。</li>
 *   <li><b>避免字符串哨兵冲突</b>——项目现有实现用 {@code "__NULL__"} 字符串占位，
 *       若真实值恰好等于该字符串会被误判。信封格式与任何业务值都不会冲突。</li>
 * </ol>
 *
 * <p><b>为什么不用 JDK 序列化？</b> Redis 里会变成二进制乱码导致 {@code redis-cli} 无法排查；
 * 存在反序列化 gadget 风险；类改包名或加字段即全量失效。
 *
 * <p><b>为什么不用 fastjson2 的 autoType？</b> 历史上多次反序列化 RCE。这里改为
 * 显式记录 FQCN + <b>包名前缀白名单校验</b>，不做多态推断。
 *
 * <p>L2 中的实际形态：
 * <pre>
 * {"n":false,"t":"com.humolix.volur.user.domain.entity.User","v":"{\"id\":123,...}"}
 * </pre>
 * 其中 {@code v} 是 payload 的 JSON 字符串（双重编码）。这会带来约 10% 的转义开销，
 * 换来的是"信封结构"与"payload 结构"完全解耦，避免字段名冲突导致的解析歧义。
 *
 * @param nullValue 是否为"缓存的 null"占位
 * @param type      payload 的运行时类型全限定名
 * @param json      payload 的 JSON 字符串
 */
public record CachedValue(boolean nullValue, String type, String json) {

    private static final Logger log = LoggerFactory.getLogger(CachedValue.class);

    /** 空值占位单例。 */
    private static final CachedValue NULL_PLACEHOLDER = new CachedValue(true, null, null);

    /**
     * 允许反序列化的包前缀白名单。
     *
     * <p>只放行本项目自有类型与 JDK 基础类型。若缓存中出现了白名单外的类型名
     * （可能是被篡改或历史遗留数据），解码会失败并按 miss 处理，不会触发任意类加载。
     */
    private static final String[] ALLOWED_PACKAGE_PREFIXES = {
            "com.humolix.volur.",
            "java.lang.",
            "java.time.",
            "java.util.",
            "java.math."
    };

    /**
     * 由业务值构造信封。
     *
     * @param value 业务值，允许为 {@code null}
     */
    public static CachedValue of(Object value) {
        if (value == null) {
            return NULL_PLACEHOLDER;
        }
        return new CachedValue(false, normalizeType(value), JSON.toJSONString(value, JSONWriter.Feature.ReferenceDetection));
    }

    /**
     * 归一化要写入信封的类型名。
     *
     * <p><b>为什么不能直接写 {@code value.getClass().getName()}？</b>
     * 因为 JDK 的不可变集合（{@code List.of(...)} / {@code Map.of(...)} / {@code Set.of(...)}）
     * 在运行时的具体类是<b>包级私有的内部实现类</b>，例如
     * {@code java.util.ImmutableCollections$ListN}。把这种名字写进信封后，
     * 读取时 fastjson2 会尝试反射构造它并直接抛
     * {@code NullPointerException: this.constructor is null}。
     *
     * <p>后果非常隐蔽且严重：由于反序列化失败被按 miss 降级，
     * 表现为<b>该缓存彻底失效</b>——每次请求都要"解码失败 → 删除 L2 → 回源 → 再写 L2"，
     * 比不加缓存还慢，而且日志里只有一条 WARN。
     * 而 {@code List.of} / {@code Map.of} 在本项目里是构造权限列表、字典项的高频写法，
     * 所以这不是理论问题。
     *
     * <p>解法是把集合一律归一化到<b>接口类型</b>：接口名既可被 {@code Class.forName} 解析，
     * 又不会在构造时踩到私有实现类。反序列化时 fastjson2 会自行选择合适的具体实现，
     * 而 {@link #unwrap} 的快路径（JSONArray/JSONObject 本身就是 List/Map）
     * 更是直接命中、连构造都省了。
     *
     * <p>需要精确具体类型（如某个自定义 {@code List} 子类）时，
     * 应在配置里用 {@code valueType} 显式声明，而不是依赖运行时类名。
     */
    private static String normalizeType(Object value) {
        Class<?> type = value.getClass();
        if (type.isArray()) {
            // 数组的运行时类名形如 [Ljava.lang.String;，无法从包前缀判断安全性，
            // 统一按 Object[] 处理；元素类型由 JSON 自身决定。
            return Object[].class.getName();
        }
        if (value instanceof Map) {
            return Map.class.getName();
        }
        if (value instanceof Set) {
            return Set.class.getName();
        }
        if (value instanceof List) {
            return List.class.getName();
        }
        if (value instanceof Collection) {
            return Collection.class.getName();
        }
        return type.getName();
    }

    /**
     * 从 L2 的原始 JSON 解码。
     *
     * @param text Redis 中读到的字符串
     * @return 信封；解析失败返回 {@code null}（调用方应按 miss 处理并回源）
     */
    public static CachedValue decode(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            JSONObject obj = JSON.parseObject(text);
            if (obj == null) {
                return null;
            }
            Boolean isNull = obj.getBoolean("n");
            if (Boolean.TRUE.equals(isNull)) {
                return NULL_PLACEHOLDER;
            }
            return new CachedValue(false, obj.getString("t"), obj.getString("v"));
        } catch (Exception ex) {
            log.warn("[volur-cache] L2 值解码失败，按 miss 处理。raw={}", abbreviate(text), ex);
            return null;
        }
    }

    /** 编码为可写入 Redis 的字符串。 */
    public String encode() {
        if (nullValue) {
            return "{\"n\":true}";
        }
        JSONObject obj = new JSONObject(3);
        obj.put("n", false);
        obj.put("t", type);
        obj.put("v", json);
        return obj.toJSONString();
    }

    /**
     * 还原业务值。
     *
     * <p><b>为什么不直接用 {@code JSON.parseObject(json, target)}？</b>
     * 因为 fastjson2 的这个重载是面向"JSON 对象"的：当 payload 是标量时
     * （例如缓存一个普通 {@code String}，JSON 形如 {@code "hello"}），
     * 它会在内部反射构造器查找时抛 {@code NullPointerException}
     * （{@code this.constructor} 为 null）。这个异常会被上层按"解码失败"降级成 miss，
     * 表现是<b>该缓存对字符串类型的值完全失效、每次都回源</b>——而且不报错，极难发现。
     *
     * <p>因此这里先走通用的 {@link JSON#parse}：它返回 JSON 语义下最贴切的类型
     * （标量→包装类型、数组→JSONArray、对象→JSONObject）。若该结果已经是目标类型
     * （含 {@code List}/{@code Map} 这类由 JSONArray/JSONObject 天然实现的接口），
     * 直接返回即可，同时避免了"字符串 '123' 被强转成数字"这类隐式类型漂移。
     * 只有目标类型确实需要构造（如自定义 POJO）时才回落到 {@code parseObject}。
     *
     * @param preferredType 配置中声明的目标类型，优先于信封内记录的类型。
     *                      用于处理类改名、或需要固定具体类型的场景；可为 {@code null}
     * @return 业务值；{@code nullValue} 为 true 时返回 {@code null}
     * @throws IllegalStateException 类型不在白名单内，或类无法加载
     */
    public Object unwrap(Class<?> preferredType) {
        if (nullValue) {
            return null;
        }
        Class<?> target = preferredType != null ? preferredType : resolveType(type);
        Object parsed = JSON.parse(json);
        if (parsed == null || target.isInstance(parsed)) {
            return parsed;
        }
        return JSON.parseObject(json, target);
    }

    /** 判断该值是否为"缓存的 null"。 */
    public boolean isNullPlaceholder() {
        return nullValue;
    }

    private static Class<?> resolveType(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            throw new IllegalStateException("缓存值缺少类型信息，无法反序列化");
        }
        if (!isAllowedType(fqcn)) {
            throw new IllegalStateException("缓存值类型不在白名单内，拒绝反序列化: " + fqcn);
        }
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("缓存值类型无法加载（可能已改名或删除）: " + fqcn, ex);
        }
    }

    /**
     * 判断类型名是否在白名单内。
     *
     * <p>数组类型名形如 {@code [Ljava.lang.String;} 或 {@code [[I}，无法用包前缀直接匹配。
     * 这里先剥离维度前缀与 {@code L...;} 外壳，再对<b>组件类型</b>做同样判断——
     * 于是 {@code [Ljava.lang.String;} 通过，而
     * {@code [Ljavax.script.ScriptEngineManager;} 依旧被拒绝，
     * 支持数组并没有放宽安全边界。
     */
    private static boolean isAllowedType(String fqcn) {
        if (fqcn.startsWith("[")) {
            String component = fqcn;
            while (component.startsWith("[")) {
                component = component.substring(1);
            }
            if (component.startsWith("L") && component.endsWith(";")) {
                return isAllowedType(component.substring(1, component.length() - 1));
            }
            // 基本类型数组（[I、[[J 等）：组件是 JVM 基本类型，不存在类加载风险
            return !component.isEmpty();
        }
        for (String prefix : ALLOWED_PACKAGE_PREFIXES) {
            if (fqcn.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
