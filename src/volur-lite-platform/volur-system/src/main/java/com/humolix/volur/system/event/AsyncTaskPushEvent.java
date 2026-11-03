package com.humolix.volur.system.event;

import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AsyncTaskPushEvent extends ApplicationEvent {

    private final AdminAsyncTaskVO taskVO;

    public AsyncTaskPushEvent(Object source, AdminAsyncTaskVO taskVO) {
        super(source);
        this.taskVO = taskVO;
    }

}
