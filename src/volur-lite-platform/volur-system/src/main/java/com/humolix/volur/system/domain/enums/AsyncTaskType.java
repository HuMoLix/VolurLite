package com.humolix.volur.system.domain.enums;

import lombok.Getter;

@Getter
public enum AsyncTaskType {

    IMAGE_TRANSFER("IMAGE_TRANSFER", "镜像节点转移"),
    IMAGE_PULL("IMAGE_PULL", "镜像节点拉取"),
    IMAGE_BACKUP("IMAGE_BACKUP", "镜像备份容灾"),
    IMAGE_IMPORT("IMAGE_IMPORT", "镜像文件导入"),
    IMAGE_SYNC("IMAGE_SYNC", "全节点镜像同步"),
    CONTAINER_BATCH_STOP("CONTAINER_BATCH_STOP", "容器批量停止"),
    EMAIL_BATCH_SEND("EMAIL_BATCH_SEND", "批量邮件投递"),
    SYSTEM_CLEANUP("SYSTEM_CLEANUP", "系统数据清理"),
    OTHER("OTHER", "通用异步任务");

    private final String value;
    private final String description;

    AsyncTaskType(String value, String description) {
        this.value = value;
        this.description = description;
    }

}
