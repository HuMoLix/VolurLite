package com.humolix.volur.logging.repository;

import com.humolix.volur.logging.domain.entity.Syslog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SyslogRepository extends JpaRepository<Syslog, Long>, JpaSpecificationExecutor<Syslog> {

    long countByLogType(String logType);

    long countByLevel(String level);

    long countByModule(String module);

    long countByModuleOrLogType(String module, String logType);

}
