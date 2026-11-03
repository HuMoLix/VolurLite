package com.humolix.volur.notification.domain.enums;

import lombok.Getter;

@Getter
public enum BroadcastDisplayType {

    MODAL("MODAL", "居中阻断弹窗"),
    BANNER("BANNER", "顶部通知横幅"),
    TOAST("TOAST", "浮层轻提示"),
    DRAWER("DRAWER", "侧边栏抽屉");

    private final String code;
    private final String description;

    BroadcastDisplayType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
