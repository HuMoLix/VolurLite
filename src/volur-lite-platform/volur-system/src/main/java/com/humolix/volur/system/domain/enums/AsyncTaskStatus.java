package com.humolix.volur.system.domain.enums;

import lombok.Getter;

@Getter
public enum AsyncTaskStatus {

    PENDING("PENDING", "等待排队"),
    RUNNING("RUNNING", "正在执行"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String description;

    AsyncTaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

}
