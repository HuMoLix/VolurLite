package com.humolix.volur.verification.repository;

import com.humolix.volur.verification.domain.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long>, JpaSpecificationExecutor<EmailLog> {

    long countByStatus(String status);

    long countByType(String type);

    long countByCreatedAtAfter(Instant time);

}
