package com.humolix.volur.challenge.entity.converter;

import com.humolix.volur.challenge.entity.enums.SubmissionStatus;
import com.humolix.volur.framework.common.entity.converter.AbstractEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SubmissionStatusConverter extends AbstractEnumConverter<SubmissionStatus> {

    public SubmissionStatusConverter() {
        super(SubmissionStatus.class);
    }

}
