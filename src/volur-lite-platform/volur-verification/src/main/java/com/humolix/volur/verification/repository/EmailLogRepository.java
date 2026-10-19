package com.humolix.volur.verification.repository;

import com.humolix.volur.verification.domain.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long>, JpaSpecificationExecutor<EmailLog> {

    long countByStatus(String status);

    long countByStatusIn(Collection<String> statuses);

    long countByType(String type);

    long countByCreatedAtAfter(Instant time);

    Optional<EmailLog> findByMessageId(String messageId);

    List<EmailLog> findTop5ByRecipientAndStatusInOrderByCreatedAtDesc(String recipient, Collection<String> statuses);

}
