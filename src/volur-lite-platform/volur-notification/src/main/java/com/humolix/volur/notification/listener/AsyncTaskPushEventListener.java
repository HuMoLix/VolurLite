package com.humolix.volur.notification.listener;

import com.humolix.volur.notification.domain.enums.RealtimeEventType;
import com.humolix.volur.notification.service.RealtimePushService;
import com.humolix.volur.system.event.AsyncTaskPushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskPushEventListener {

    private final RealtimePushService realtimePushService;

    @Async
    @EventListener
    public void onAsyncTaskPush(AsyncTaskPushEvent event) {
        if (event == null || event.getTaskVO() == null) {
            return;
        }
        log.debug("[Realtime-Task] 广播异步任务状态流: id={}, name={}, status={}, progress={}%",
                event.getTaskVO().getId(), event.getTaskVO().getTaskName(),
                event.getTaskVO().getStatus(), event.getTaskVO().getProgress());

        realtimePushService.broadcastAll(RealtimeEventType.TASK_EVENT.getValue(), event.getTaskVO());
    }

}
