package com.humolix.volur.notification.repository;

import com.humolix.volur.notification.domain.entity.BroadcastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long>, JpaSpecificationExecutor<BroadcastMessage> {

    long countByCategory(String category);

    long countByForcePopupTrue();

}
