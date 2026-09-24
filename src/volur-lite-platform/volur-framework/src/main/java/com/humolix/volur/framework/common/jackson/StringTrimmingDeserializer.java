package com.humolix.volur.framework.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.humolix.volur.framework.common.annotation.Trimmed;

import java.io.IOException;

/**
 * Jackson 字符串就地高效清洗反序列化器
 * <p>
 * 基于 {@link ContextualDeserializer} 实现启动期一次性参数解析与实例缓存，运行时零反射开销。
 * </p>
 */
public class StringTrimmingDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    private final boolean toLowerCase;
    private final boolean emptyToNull;

    public StringTrimmingDeserializer() {
        this(false, false);
    }

    public StringTrimmingDeserializer(boolean toLowerCase, boolean emptyToNull) {
        this.toLowerCase = toLowerCase;
        this.emptyToNull = emptyToNull;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        String result = value.trim();
        if (toLowerCase) {
            result = result.toLowerCase();
        }
        if (emptyToNull && result.isEmpty()) {
            return null;
        }
        return result;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }
        Trimmed annotation = property.getAnnotation(Trimmed.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Trimmed.class);
        }
        if (annotation != null) {
            return new StringTrimmingDeserializer(annotation.toLowerCase(), annotation.emptyToNull());
        }
        return this;
    }
}
