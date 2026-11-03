package com.humolix.volur.framework.common.jackson;

import com.humolix.volur.framework.common.annotation.Trimmed;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StringTrimmingDeserializer extends ValueDeserializer<String> {

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
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
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
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
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
