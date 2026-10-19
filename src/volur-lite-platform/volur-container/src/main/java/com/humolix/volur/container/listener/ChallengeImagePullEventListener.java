package com.humolix.volur.container.listener;

import com.humolix.volur.challenge.event.ChallengeImagePullEvent;
import com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO;
import com.humolix.volur.container.service.admin.AdminImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 赛题镜像拉取事件监听器
 * 接收来自题库管理（新增/批量导入）的镜像拉取事件，自动转交容器镜像服务提交后台异步任务并广播进度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeImagePullEventListener {

    private final AdminImageService adminImageService;

    @Async("volurVirtualExecutor")
    @EventListener
    public void onChallengeImagePull(ChallengeImagePullEvent event) {
        if (!StringUtils.hasText(event.getImageName())) {
            return;
        }
        String imageName = event.getImageName().trim();
        log.info("[ChallengeImagePullEventListener] 捕获赛题关联镜像拉取事件: challengeId={}, challengeName={}, image={}, 正在触发后台异步任务拉取...",
                event.getChallengeId(), event.getChallengeName(), imageName);
        try {
            adminImageService.pullImage(new AdminImagePullRequestDTO(imageName));
            log.info("[ChallengeImagePullEventListener] 赛题关联镜像异步拉取任务已成功提交至异步任务中心: {}", imageName);
        } catch (Exception e) {
            log.error("[ChallengeImagePullEventListener] 提交镜像拉取异步任务失败: image={}, error={}", imageName, e.getMessage(), e);
        }
    }

}
