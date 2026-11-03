package com.humolix.volur.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingVO {

    private String group;
    private String key;
    private String value;
    private String valueType;
    private Boolean isPublic;
    private String description;

}
