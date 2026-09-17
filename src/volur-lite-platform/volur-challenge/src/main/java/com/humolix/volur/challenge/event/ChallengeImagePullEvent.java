package com.humolix.volur.challenge.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 赛题镜像拉取事件
 * 当 CTF / AWD 等题目创建或导入时发现包含容器镜像定义，异步触发该事件通知容器引擎中心执行后台镜像拉取任务
 */
@Getter
public class ChallengeImagePullEvent extends ApplicationEvent {

    private final String imageName;
    private final Long challengeId;
    private final String challengeName;
    private final Long operatorId;
    private final String operatorName;

    public ChallengeImagePullEvent(Object source, String imageName, Long challengeId, String challengeName, Long operatorId, String operatorName) {
        super(source);
        this.imageName = imageName;
        this.challengeId = challengeId;
        this.challengeName = challengeName;
        this.operatorId = operatorId;
        this.operatorName = operatorName != null ? operatorName : "ADMIN";
    }

}
