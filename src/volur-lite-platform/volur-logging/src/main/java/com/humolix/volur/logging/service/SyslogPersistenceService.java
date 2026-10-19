package com.humolix.volur.logging.service;

import com.humolix.volur.logging.domain.entity.Syslog;

public interface SyslogPersistenceService {

    void saveLogAsync(Syslog syslog);

    Syslog saveLog(Syslog syslog);

    void flushBuffer();

}
