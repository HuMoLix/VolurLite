package com.humolix.volur.framework.domain.converter;

import com.humolix.volur.framework.domain.enums.EnumEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public abstract class AbstractEnumConverter<E extends Enum<E> & EnumEntity> implements AttributeConverter<E, Integer> {

    private final Class<E> clazz;

    protected AbstractEnumConverter(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return EnumEntity.fromCode(clazz, dbData);
    }

}
