package com.humolix.volur.framework.common.config;

import com.humolix.volur.framework.common.thread.AsyncContextTaskDecorator;
import com.humolix.volur.framework.common.thread.AsyncProperties;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AsyncProperties.class)
@RequiredArgsConstructor
public class SchedulingConfig implements SchedulingConfigurer {

    private final AsyncProperties asyncProperties;
    private final AsyncContextTaskDecorator asyncContextTaskDecorator;
    private final VolurThreadManager volurThreadManager;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(volurTaskScheduler());
        log.info("[SchedulingConfig] 成功配置全局定时任务调度器 -> 引擎: Java 25 虚拟线程, 线程前缀: {}",
                asyncProperties.getScheduleThreadNamePrefix());
    }

    @Bean(name = {"volurTaskScheduler", "taskScheduler"})
    public TaskScheduler volurTaskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(asyncProperties.isVirtualThreadsEnabled());
        scheduler.setThreadNamePrefix(asyncProperties.getScheduleThreadNamePrefix());
        scheduler.setTaskDecorator(asyncContextTaskDecorator);
        scheduler.setTaskTerminationTimeout(asyncProperties.getTerminationTimeoutSeconds() * 1000L);
        scheduler.setCancelRemainingTasksOnClose(false);
        scheduler.setTargetTaskExecutor(volurThreadManager.getVirtualExecutor());
        scheduler.setErrorHandler(throwable ->
                log.error("[Volur-Scheduler] 定时任务执行异常: {}", throwable.getMessage(), throwable));
        return scheduler;
    }

}
