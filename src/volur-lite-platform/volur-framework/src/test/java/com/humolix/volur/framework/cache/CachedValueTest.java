package com.humolix.volur.framework.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CachedValue} 信封的编解码契约。
 *
 * <p>这是 L2 的"数据格式"层，一旦出错会表现为"缓存反复 miss 或反复脏数据"，
 * 且因为被 try/catch 降级而<b>不会抛异常</b>，极难排查。因此这里覆盖得细一些。
 */
class CachedValueTest {

    @Test
    @DisplayName("null 值编码为结构化占位，可与'没有缓存'区分")
    void nullPlaceholderRoundTrip() {
        CachedValue cv = CachedValue.of(null);

        assertThat(cv.isNullPlaceholder()).isTrue();
        assertThat(cv.encode()).isEqualTo("{\"n\":true}");

        CachedValue decoded = CachedValue.decode(cv.encode());
        assertThat(decoded).isNotNull();
        assertThat(decoded.isNullPlaceholder()).isTrue();
        // 关键：占位值解出来就是 null，且不会尝试反序列化
        assertThat(decoded.unwrap(null)).isNull();
    }

    @Test
    @DisplayName("结构化占位不会与真实业务值冲突（对比字符串哨兵方案的缺陷）")
    void placeholderDoesNotCollideWithRealValue() {
        // 旧实现用 "__NULL__" 字符串做哨兵：若业务值恰好等于该字符串就会被误判为 null。
        // 信封方案下，真实值 "__NULL__" 编码后带 t/v 字段，与占位 {"n":true} 完全不同。
        CachedValue realValue = CachedValue.of("__NULL__");

        assertThat(realValue.isNullPlaceholder()).isFalse();
        assertThat(realValue.encode()).isNotEqualTo(CachedValue.of(null).encode());

        CachedValue decoded = CachedValue.decode(realValue.encode());
        assertThat(decoded.isNullPlaceholder()).isFalse();
        assertThat(decoded.unwrap(null)).isEqualTo("__NULL__");
    }

    @Test
    @DisplayName("String / Integer / BigDecimal / List / Map 均可无损往返")
    void primitiveAndCollectionRoundTrip() {
        assertRoundTrip("hello");
        assertRoundTrip(42);
        assertRoundTrip(1234567890123L);
        assertRoundTrip(new BigDecimal("3.14159"));
        assertRoundTrip(List.of("a", "b", "c"));
        assertRoundTrip(Map.of("k1", "v1", "k2", 2));
        assertRoundTrip(Duration.ofSeconds(30));
    }

    private void assertRoundTrip(Object value) {
        CachedValue cv = CachedValue.of(value);
        CachedValue decoded = CachedValue.decode(cv.encode());

        assertThat(decoded).as("解码 %s", value).isNotNull();
        assertThat(decoded.isNullPlaceholder()).isFalse();
        assertThat(decoded.unwrap(null)).as("往返 %s", value).isEqualTo(value);
    }

    /** 模拟一个项目里的领域对象（白名单内的包前缀）。 */
    public static class SampleEntity {
        public long id;
        public String name;

        public SampleEntity() {
        }

        public SampleEntity(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SampleEntity other && other.id == id
                    && java.util.Objects.equals(other.name, name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }
    }

    @Test
    @DisplayName("自定义 POJO 字段完整还原")
    void pojoRoundTripUsesFallbackPath() {
        assertRoundTrip(new SampleEntity(7L, "volur"));
    }

    @Test
    @DisplayName("缓存普通 String 值不会因标量而解码失败")
    void plainStringValueIsNotBrokenByScalarParsing() {
        for (String value : List.of("hello", "123", "true", "null", "", "{\"a\":1}")) {
            CachedValue cv = CachedValue.of(value);
            CachedValue decoded = CachedValue.decode(cv.encode());

            assertThat(decoded).as("字符串 %s", value).isNotNull();
            assertThat(decoded.unwrap(null))
                    .as("字符串 %s 必须原样还原且不被类型强转", value)
                    .isEqualTo(value);
        }
    }

    @Test
    @DisplayName("JDK 不可变集合（List.of/Map.of/Set.of）可往返")
    void jdkImmutableCollectionsRoundTrip() {
        assertRoundTrip(List.of("a", "b", "c"));
        assertRoundTrip(List.of());
        assertRoundTrip(Map.of("k1", "v1", "k2", 2));
        assertRoundTrip(Map.of());
        assertRoundTrip(Set.of("x", "y"));
        assertRoundTrip(java.util.Collections.unmodifiableList(List.of(1, 2, 3)));
        assertRoundTrip(java.util.Collections.singletonList("only"));
        assertRoundTrip(new java.util.ArrayList<>(List.of("mutable")));
        assertRoundTrip(new java.util.LinkedHashMap<>(Map.of("k", "v")));
    }

    @Test
    @DisplayName("数组可往返（类型名归一化为 Object[]，且白名单按组件类型校验）")
    void arrayRoundTrip() {
        CachedValue cv = CachedValue.of(new String[]{"a", "b"});
        CachedValue decoded = CachedValue.decode(cv.encode());

        assertThat(decoded).isNotNull();
        Object back = decoded.unwrap(null);
        assertThat(back).isInstanceOf(Object[].class);
        assertThat((Object[]) back).containsExactly("a", "b");
    }

    @Test
    @DisplayName("白名单校验会穿透数组外壳，不因支持数组而放宽")
    void arrayWhitelistCheckUnwrapsComponentType() {
        String tampered = "{\"n\":false,\"t\":\"[Ljavax.script.ScriptEngineManager;\",\"v\":\"[]\"}";
        CachedValue cv = CachedValue.decode(tampered);

        assertThat(cv).isNotNull();
        assertThatThrownBy(() -> cv.unwrap(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("白名单");
    }

    @Test
    @DisplayName("白名单外的类型在反序列化时被拒绝——防 autoType 式 RCE")
    void rejectsTypeOutsideWhitelist() {
        String tampered = "{\"n\":false,\"t\":\"javax.script.ScriptEngineManager\",\"v\":\"{}\"}";
        CachedValue cv = CachedValue.decode(tampered);

        assertThat(cv).isNotNull();
        assertThatThrownBy(() -> cv.unwrap(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("白名单");
    }

    @Test
    @DisplayName("显式声明 valueType 时绕过信封内类型，用于类改名场景")
    void preferredTypeOverridesEnvelopeType() {
        String json = "{\"n\":false,\"t\":\"javax.script.ScriptEngineManager\",\"v\":\"\\\"x\\\"\"}";
        CachedValue cv = CachedValue.decode(json);

        assertThat(cv.unwrap(String.class)).isEqualTo("x");
    }

    @Test
    @DisplayName("类型名缺失或无法加载时抛出明确异常，而不是静默返回 null")
    void rejectsMissingOrUnknownType() {
        CachedValue noType = new CachedValue(false, null, "{}");
        assertThatThrownBy(() -> noType.unwrap(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少类型信息");

        CachedValue unknown = new CachedValue(false, "com.humolix.volur.NotExist", "{}");
        assertThatThrownBy(() -> unknown.unwrap(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法加载");
    }

    @Test
    @DisplayName("畸形 JSON 解码返回 null（调用方按 miss 处理），不抛异常")
    void malformedInputDegradesToNull() {
        assertThat(CachedValue.decode(null)).isNull();
        assertThat(CachedValue.decode("")).isNull();
        assertThat(CachedValue.decode("not-json-at-all{{{")).isNull();
    }

    @Test
    @DisplayName("List 中的 POJO 元素可精确还原其泛型类型，不退化为 LinkedHashMap")
    void pojoListPreservesElementType() {
        List<SampleEntity> list = List.of(new SampleEntity(1L, "first"), new SampleEntity(2L, "second"));
        CachedValue cv = CachedValue.of(list);
        CachedValue decoded = CachedValue.decode(cv.encode());

        assertThat(decoded).isNotNull();
        Object back = decoded.unwrap(null);
        assertThat(back).isInstanceOf(List.class);
        List<?> backList = (List<?>) back;
        assertThat(backList).hasSize(2);
        assertThat(backList.get(0)).isInstanceOf(SampleEntity.class);
        assertThat(backList.get(0)).isEqualTo(new SampleEntity(1L, "first"));
        assertThat(backList.get(1)).isInstanceOf(SampleEntity.class);
        assertThat(backList.get(1)).isEqualTo(new SampleEntity(2L, "second"));
    }

    @Test
    @DisplayName("Set 中的 POJO 元素可精确还原其泛型类型")
    void pojoSetPreservesElementType() {
        Set<SampleEntity> set = Set.of(new SampleEntity(1L, "item"));
        CachedValue cv = CachedValue.of(set);
        CachedValue decoded = CachedValue.decode(cv.encode());

        assertThat(decoded).isNotNull();
        Object back = decoded.unwrap(null);
        assertThat(back).isInstanceOf(Set.class);
        Set<?> backSet = (Set<?>) back;
        assertThat(backSet).hasSize(1);
        assertThat(backSet.iterator().next()).isInstanceOf(SampleEntity.class);
    }

    @Test
    @DisplayName("白名单外的 elementType 在反序列化时被拒绝")
    void rejectsElementTypeOutsideWhitelist() {
        String tampered = "{\"n\":false,\"t\":\"java.util.List\",\"e\":\"javax.script.ScriptEngineManager\",\"v\":\"[]\"}";
        CachedValue cv = CachedValue.decode(tampered);

        assertThat(cv).isNotNull();
        assertThatThrownBy(() -> cv.unwrap(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("白名单");
    }
}
