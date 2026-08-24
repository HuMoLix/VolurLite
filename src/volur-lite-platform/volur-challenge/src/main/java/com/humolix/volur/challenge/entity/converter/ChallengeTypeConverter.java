package com.humolix.volur.challenge.entity.converter;

import com.humolix.volur.challenge.entity.enums.ChallengeType;
import com.humolix.volur.framework.common.entity.converter.AbstractEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChallengeTypeConverter extends AbstractEnumConverter<ChallengeType> {

    public ChallengeTypeConverter() {
        super(ChallengeType.class);
    }

}
