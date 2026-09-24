package com.humolix.volur.logging.service.impl;

import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.logging.domain.entity.Syslog;
import com.humolix.volur.logging.repository.SyslogRepository;
import com.humolix.volur.logging.service.SyslogPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyslogPersistenceServiceImpl implements SyslogPersistenceService, SmartLifecycle {

    public static final String VREDIS_LOGGING_SYSLOG_BUFFER_KEY = "vredis:logging:syslog:buffer";
    private static final int BATCH_SIZE = 50;

    private final SyslogRepository syslogRepository;
    private final VolurThreadManager volurThreadManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private volatile boolean running = false;

    @Override
    public void saveLogAsync(Syslog syslog) {
        if (syslog == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(syslog);
            Long size = stringRedisTemplate.opsForList().rightPush(VREDIS_LOGGING_SYSLOG_BUFFER_KEY, json);

            // 自适应触发: 达到批量阈值时立即异步触发落库
            if (size != null && size >= BATCH_SIZE) {
                volurThreadManager.runAsync(this::drainBuffer);
            }
        } catch (Exception e) {
            log.warn("[Syslog] 写入 Redis 日志缓冲失败，降级为虚拟线程直写: {}", e.getMessage());
            volurThreadManager.runAsync(() -> {
                try {
                    syslogRepository.save(syslog);
                } catch (Exception ex) {
                    log.error("[Syslog] 降级写入系统日志异常: {}", ex.getMessage());
                }
            });
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Syslog saveLog(Syslog syslog) {
        if (syslog == null) {
            return null;
        }
        try {
            return syslogRepository.save(syslog);
        } catch (Exception ex) {
            log.error("[Syslog] 同步写入系统审计日志异常 -> action: {}, error: {}", syslog.getAction(), ex.getMessage());
            return null;
        }
    }

    /**
     * 200ms 高频微批处理定时轮询器（由 SchedulingConfig 配置的 Java 25 虚拟线程调度）
     */
    @Scheduled(fixedDelay = 200)
    public void drainBuffer() {
        try {
            while (true) {
                List<String> rawLogs = stringRedisTemplate.opsForList().leftPop(VREDIS_LOGGING_SYSLOG_BUFFER_KEY, BATCH_SIZE);
                if (CollectionUtils.isEmpty(rawLogs)) {
                    break;
                }
                processBatch(rawLogs);
                if (rawLogs.size() < BATCH_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[Syslog] 消费 Redis 日志缓冲区批量入库异常: {}", e.getMessage());
        }
    }

    @Override
    public void flushBuffer() {
        drainBuffer();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processBatch(List<String> rawLogs) {
        List<Syslog> entities = new ArrayList<>(rawLogs.size());

        for (String raw : rawLogs) {
            try {
                Syslog item = objectMapper.readValue(raw, Syslog.class);
                if (item != null) {
                    entities.add(item);
                }
            } catch (Exception e) {
                log.warn("[Syslog] 反序列化日志条目失败: {}", e.getMessage());
            }
        }

        if (!entities.isEmpty()) {
            syslogRepository.saveAll(entities);
            log.debug("[Syslog] 成功批量落库 {} 条系统日志", entities.size());
        }
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;
        log.info("[Syslog] 服务优雅停机，正在执行 Redis 系统日志缓冲区收尾落库...");
        drainBuffer();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        // 赋予大于 LettuceConnectionFactory(phase=0) 的高优先级，确保在 Redis 连接池关闭前执行收尾
        return Integer.MAX_VALUE - 1000;
    }

}
