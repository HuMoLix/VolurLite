package com.humolix.volur.notification.domain.enums;

import lombok.Getter;

@Getter
public enum RealtimeEventType {

    NOTIFICATION("notification", "个人站内信通知"),
    ANNOUNCEMENT("announcement", "全站系统公告"),
    PROMOTION("promotion", "运营活动广告"),
    COMPETITION_EVENT("competition_event", "竞赛动态事件"),
    TASK_EVENT("task_event", "异步任务状态流与进度更新"),
    HEARTBEAT("heartbeat", "保活心跳");

    private final String value;
    private final String description;

    RealtimeEventType(String value, String description) {
        this.value = value;
        this.description = description;
    }

}
