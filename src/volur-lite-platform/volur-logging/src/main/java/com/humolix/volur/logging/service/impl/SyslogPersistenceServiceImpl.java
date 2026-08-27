package com.humolix.volur.logging.service.impl;

import com.humolix.volur.logging.domain.entity.Syslog;
import com.humolix.volur.logging.repository.SyslogRepository;
import com.humolix.volur.logging.service.SyslogPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyslogPersistenceServiceImpl implements SyslogPersistenceService {

    private final SyslogRepository syslogRepository;

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLogAsync(Syslog syslog) {
        if (syslog == null) {
            return;
        }
        try {
            syslogRepository.save(syslog);
        } catch (Exception ex) {
            log.error("[Syslog] 异步写入系统审计日志异常 -> action: {}, error: {}", syslog.getAction(), ex.getMessage());
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

}
