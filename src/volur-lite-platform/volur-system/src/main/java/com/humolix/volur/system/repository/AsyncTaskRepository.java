package com.humolix.volur.system.repository;

import com.humolix.volur.system.domain.entity.AsyncTaskEntity;
import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTaskEntity, Long>, JpaSpecificationExecutor<AsyncTaskEntity> {

    long countByStatus(AsyncTaskStatus status);

    List<AsyncTaskEntity> findByStatusIn(Collection<AsyncTaskStatus> statuses);

    @Modifying
    @Query("DELETE FROM AsyncTaskEntity t WHERE t.status = :status")
    int deleteByStatus(@Param("status") AsyncTaskStatus status);

    @Modifying
    @Query("DELETE FROM AsyncTaskEntity t WHERE t.status IN (:statuses)")
    int deleteByStatusIn(@Param("statuses") Collection<AsyncTaskStatus> statuses);

    @Modifying
    @Query("DELETE FROM AsyncTaskEntity t WHERE t.status IN (com.humolix.volur.system.domain.enums.AsyncTaskStatus.SUCCESS, com.humolix.volur.system.domain.enums.AsyncTaskStatus.FAILED, com.humolix.volur.system.domain.enums.AsyncTaskStatus.CANCELLED)")
    int clearAllFinishedTasks();

}
