package com.humolix.volur.competition.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrganizationType {

    SCHOOL("学校"),
    ENTERPRISE("企业单位"),
    MILITARY("军工"),
    ENERGY("能源"),
    FINANCE("金融"),
    RESEARCH("科研院所"),
    OTHER("其他");

    private final String description;

}
