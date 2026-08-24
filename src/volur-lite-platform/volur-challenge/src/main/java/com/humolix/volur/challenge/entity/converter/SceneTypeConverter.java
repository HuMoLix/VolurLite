package com.humolix.volur.challenge.entity.converter;

import com.humolix.volur.challenge.entity.enums.SceneType;
import com.humolix.volur.framework.common.entity.converter.AbstractEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SceneTypeConverter extends AbstractEnumConverter<SceneType> {

    public SceneTypeConverter() {
        super(SceneType.class);
    }

}
