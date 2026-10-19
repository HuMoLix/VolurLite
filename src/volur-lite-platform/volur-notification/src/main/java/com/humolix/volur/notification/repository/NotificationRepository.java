package com.humolix.volur.notification.repository;

import com.humolix.volur.notification.domain.entity.Notification;
import com.humolix.volur.notification.domain.enums.NotificationStatus;
import com.humolix.volur.notification.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    long countByReceiverIdAndStatus(Long receiverId, NotificationStatus status);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findByReceiverIdAndTypeAndActionDataContaining(Long receiverId, NotificationType type, String keyword);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readTime = :now WHERE n.receiverId = :receiverId AND n.status = 'UNREAD'")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId, @Param("now") Instant now);

}
