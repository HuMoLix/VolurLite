package com.humolix.volur.framework.common.config;

import com.humolix.volur.framework.common.thread.AsyncContextTaskDecorator;
import com.humolix.volur.framework.common.thread.AsyncProperties;
import com.humolix.volur.framework.common.thread.VolurAsyncExceptionHandler;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncProperties asyncProperties;
    private final AsyncContextTaskDecorator asyncContextTaskDecorator;
    private final VolurAsyncExceptionHandler volurAsyncExceptionHandler;
    private final VolurThreadManager volurThreadManager;

    @Override
    public Executor getAsyncExecutor() {
        return volurVirtualExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return volurAsyncExceptionHandler;
    }

    @Bean(name = "volurVirtualExecutor")
    public AsyncTaskExecutor volurVirtualExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(asyncProperties.getThreadNamePrefix());
        executor.setVirtualThreads(asyncProperties.isVirtualThreadsEnabled());
        executor.setTaskDecorator(asyncContextTaskDecorator);
        executor.setTaskTerminationTimeout(asyncProperties.getTerminationTimeoutSeconds() * 1000L);
        executor.setCancelRemainingTasksOnClose(false);
        if (asyncProperties.getMaxConcurrentVirtualTasks() > 0) {
            executor.setConcurrencyLimit(asyncProperties.getMaxConcurrentVirtualTasks());
        }
        return executor;
    }

    @Bean(name = "volurComputeExecutor")
    public Executor volurComputeExecutor() {
        return volurThreadManager.getComputeExecutor();
    }

}
