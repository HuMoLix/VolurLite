package com.humolix.volur.framework.common.validation.enums;

import lombok.Getter;

/**
 * 中国大陆电信运营商枚举（四大基础电信业务经营商）
 */
@Getter
public enum ChinaCarrier {

    /**
     * 四大基础电信运营商全集（中国移动、中国联通、中国电信、中国广电）
     */
    ALL("全部四大运营商", "All 4 Carriers"),

    /**
     * 中国移动通信集团有限公司 (China Mobile)
     */
    CHINA_MOBILE("中国移动", "China Mobile"),

    /**
     * 中国联合网络通信集团有限公司 (China Unicom)
     */
    CHINA_UNICOM("中国联通", "China Unicom"),

    /**
     * 中国电信集团有限公司 (China Telecom)
     */
    CHINA_TELECOM("中国电信", "China Telecom"),

    /**
     * 中国广播电视网络集团有限公司 (China Broadnet)
     */
    CHINA_BROADNET("中国广电", "China Broadnet");

    private final String description;
    private final String englishName;

    ChinaCarrier(String description, String englishName) {
        this.description = description;
        this.englishName = englishName;
    }
}
