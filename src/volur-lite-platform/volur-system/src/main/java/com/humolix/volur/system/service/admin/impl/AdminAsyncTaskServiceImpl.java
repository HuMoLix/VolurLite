package com.humolix.volur.system.service.admin.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.system.domain.dto.AdminAsyncTaskPageRequestDTO;
import com.humolix.volur.system.domain.entity.AsyncTaskEntity;
import com.humolix.volur.system.domain.enums.AsyncTaskStatus;
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskStatisticsVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;
import com.humolix.volur.system.event.AsyncTaskPushEvent;
import com.humolix.volur.system.mapper.AsyncTaskMapper;
import com.humolix.volur.system.repository.AsyncTaskRepository;
import com.humolix.volur.system.service.admin.AdminAsyncTaskService;
import com.humolix.volur.system.service.admin.AsyncTaskContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAsyncTaskServiceImpl implements AdminAsyncTaskService {

    private final AsyncTaskRepository asyncTaskRepository;
    private final AsyncTaskMapper asyncTaskMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ThreadPoolTaskExecutor volurAsyncTaskExecutor;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    private final Set<Long> cancelledTaskIds = ConcurrentHashMap.newKeySet();

    @Override
    public AdminAsyncTaskSubmitVO submitTask(
            AsyncTaskType taskType,
            String taskName,
            Long operatorId,
            String operatorName,
            Object parameters,
            Consumer<AsyncTaskContext> taskRunnable
    ) {
        if (taskType == null || !StringUtils.hasText(taskName)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务类型与任务名称不能为空");
        }

        String paramsJson = null;
        if (parameters != null) {
            try {
                paramsJson = parameters instanceof String s ? s : objectMapper.writeValueAsString(parameters);
            } catch (Exception e) {
                paramsJson = String.valueOf(parameters);
            }
        }

        AsyncTaskEntity entity = AsyncTaskEntity.builder()
                .taskName(taskName)
                .taskType(taskType)
                .status(AsyncTaskStatus.PENDING)
                .progress(0)
                .statusMessage("任务已排队等待执行")
                .parameters(paramsJson)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build();

        // 1. 在全新独立物理事务中执行 persist(saveAndFlush)
        // 注意：不预先赋值 id，让 @SnowflakeGenerated 在 persist 时自动生成，
        // 从而使 Spring Data JPA 正确判定 isNew=true 并走 em.persist 而非 em.merge，
        // 彻底杜绝 Hibernate 7 在 merge 不存在实体时抛出 StaleObjectStateException
        TransactionTemplate requiresNewTemplate = new TransactionTemplate(transactionManager);
        requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AsyncTaskEntity savedEntity = requiresNewTemplate.execute(status -> asyncTaskRepository.saveAndFlush(entity));

        if (savedEntity == null || savedEntity.getId() == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "初始化异步任务失败");
        }

        Long taskId = savedEntity.getId();

        log.info("[AsyncTask] 创建并提交异步任务 -> id: {}, name: {}, type: {}, operator: {}",
                taskId, taskName, taskType, operatorName);

        publishPushEvent(savedEntity);

        // 2. 调度后台异步执行
        // 若当前处于业务外层事务中，必须在事务 commit 完成后才派发后台线程执行，
        // 杜绝后台线程与外层事务并发读写同一实体引发的竞态冲突
        Runnable backgroundTaskExecution = () -> runTaskInternal(taskId, taskRunnable);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    volurAsyncTaskExecutor.execute(backgroundTaskExecution);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        log.warn("[AsyncTask] 上游业务事务未正常提交 (status={}), 取消异步任务调度 -> taskId: {}", status, taskId);
                        failTaskInternal(taskId, null, "上游业务事务执行失败或回滚，任务取消执行");
                    }
                }
            });
        } else {
            volurAsyncTaskExecutor.execute(backgroundTaskExecution);
        }

        return AdminAsyncTaskSubmitVO.builder()
                .taskId(taskId)
                .taskName(taskName)
                .taskType(taskType)
                .status(AsyncTaskStatus.PENDING)
                .build();
    }

    private void runTaskInternal(Long taskId, Consumer<AsyncTaskContext> taskRunnable) {
        if (cancelledTaskIds.remove(taskId)) {
            log.info("[AsyncTask] 任务启动前已被取消 -> taskId: {}", taskId);
            return;
        }

        Instant startedAt = Instant.now();
        boolean started = markTaskRunning(taskId, startedAt);
        if (!started) {
            return;
        }

        AsyncTaskContext context = new AsyncTaskContext() {
            @Override
            public Long getTaskId() {
                return taskId;
            }

            @Override
            public void updateProgress(int progress, String statusMessage) {
                AdminAsyncTaskServiceImpl.this.updateProgress(taskId, progress, statusMessage);
            }

            @Override
            public boolean isCancelled() {
                return cancelledTaskIds.contains(taskId);
            }
        };

        try {
            taskRunnable.accept(context);

            // 检查执行期间是否被取消
            if (context.isCancelled()) {
                handleCancelledCompletion(taskId, startedAt);
            } else {
                completeTaskInternal(taskId, startedAt, null);
            }
        } catch (Throwable ex) {
            log.error("[AsyncTask] 任务执行发生异常 -> taskId: {}, message: {}", taskId, ex.getMessage(), ex);
            failTaskInternal(taskId, startedAt, ex.getMessage() != null ? ex.getMessage() : ex.toString());
        } finally {
            cancelledTaskIds.remove(taskId);
        }
    }

    private boolean markTaskRunning(Long taskId, Instant startedAt) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Boolean.TRUE.equals(template.execute(status -> {
            AsyncTaskEntity entity = asyncTaskRepository.findById(taskId).orElse(null);
            if (entity == null) {
                log.warn("[AsyncTask] 任务未找到，跳过执行 -> taskId: {}", taskId);
                return false;
            }

            if (entity.getStatus() == AsyncTaskStatus.CANCELLED) {
                log.info("[AsyncTask] 任务已被取消，跳过执行 -> taskId: {}", taskId);
                return false;
            }

            entity.setStatus(AsyncTaskStatus.RUNNING);
            entity.setStartedAt(startedAt);
            entity.setProgress(Math.max(entity.getProgress(), 1));
            entity.setStatusMessage("正在执行异步任务...");
            asyncTaskRepository.saveAndFlush(entity);
            publishPushEvent(entity);
            return true;
        }));
    }

    private void handleCancelledCompletion(Long taskId, Instant startedAt) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            asyncTaskRepository.findById(taskId).ifPresent(entity -> {
                Instant finishedAt = Instant.now();
                entity.setStatus(AsyncTaskStatus.CANCELLED);
                entity.setFinishedAt(finishedAt);
                if (startedAt != null) {
                    entity.setExecutionDurationMs(Duration.between(startedAt, finishedAt).toMillis());
                }
                entity.setStatusMessage("任务已被用户取消中断");
                asyncTaskRepository.saveAndFlush(entity);
                publishPushEvent(entity);
                log.info("[AsyncTask] 任务已被取消中断 -> taskId: {}", taskId);
            });
            return null;
        });
    }

    private void completeTaskInternal(Long taskId, Instant startedAt, Object resultData) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            asyncTaskRepository.findById(taskId).ifPresent(entity -> {
                if (entity.getStatus() == AsyncTaskStatus.CANCELLED) {
                    return;
                }
                Instant finishedAt = Instant.now();
                entity.setStatus(AsyncTaskStatus.SUCCESS);
                entity.setProgress(100);
                entity.setFinishedAt(finishedAt);
                if (startedAt != null) {
                    entity.setExecutionDurationMs(Duration.between(startedAt, finishedAt).toMillis());
                } else if (entity.getStartedAt() != null) {
                    entity.setExecutionDurationMs(Duration.between(entity.getStartedAt(), finishedAt).toMillis());
                }
                entity.setStatusMessage("任务执行成功完成");
                if (resultData != null) {
                    try {
                        entity.setResultData(resultData instanceof String s ? s : objectMapper.writeValueAsString(resultData));
                    } catch (Exception ignored) {
                        entity.setResultData(String.valueOf(resultData));
                    }
                }
                asyncTaskRepository.saveAndFlush(entity);
                publishPushEvent(entity);
                log.info("[AsyncTask] 任务执行成功 -> taskId: {}, 耗时: {}ms", taskId, entity.getExecutionDurationMs());
            });
            return null;
        });
    }

    private void failTaskInternal(Long taskId, Instant startedAt, String errorMessage) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            asyncTaskRepository.findById(taskId).ifPresent(entity -> {
                if (entity.getStatus() == AsyncTaskStatus.CANCELLED) {
                    return;
                }
                Instant finishedAt = Instant.now();
                entity.setStatus(AsyncTaskStatus.FAILED);
                entity.setFinishedAt(finishedAt);
                if (startedAt != null) {
                    entity.setExecutionDurationMs(Duration.between(startedAt, finishedAt).toMillis());
                } else if (entity.getStartedAt() != null) {
                    entity.setExecutionDurationMs(Duration.between(entity.getStartedAt(), finishedAt).toMillis());
                }
                entity.setStatusMessage("任务执行失败: " + (errorMessage != null ? errorMessage : "未知错误"));
                entity.setErrorMessage(errorMessage);
                asyncTaskRepository.saveAndFlush(entity);
                publishPushEvent(entity);
                log.warn("[AsyncTask] 任务执行标记失败 -> taskId: {}, 原因: {}", taskId, errorMessage);
            });
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAsyncTaskVO> listTasks(AdminAsyncTaskPageRequestDTO request) {
        Pageable pageable = request.toPageable(AsyncTaskEntity.class, "createdAt", Sort.Direction.DESC);

        Specification<AsyncTaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String keywordLike = "%" + request.getKeyword().trim() + "%";
                Predicate taskNameLike = cb.like(root.get("taskName"), keywordLike);
                Predicate operatorLike = cb.like(root.get("operatorName"), keywordLike);
                Predicate errorLike = cb.like(root.get("errorMessage"), keywordLike);
                predicates.add(cb.or(taskNameLike, operatorLike, errorLike));
            }

            if (request.getTaskType() != null) {
                predicates.add(cb.equal(root.get("taskType"), request.getTaskType()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartTime()));
            }

            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AsyncTaskEntity> page = asyncTaskRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(asyncTaskMapper::toVO));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAsyncTaskStatisticsVO getStatistics() {
        return AdminAsyncTaskStatisticsVO.builder()
                .totalCount(asyncTaskRepository.count())
                .runningCount(asyncTaskRepository.countByStatus(AsyncTaskStatus.RUNNING))
                .pendingCount(asyncTaskRepository.countByStatus(AsyncTaskStatus.PENDING))
                .successCount(asyncTaskRepository.countByStatus(AsyncTaskStatus.SUCCESS))
                .failedCount(asyncTaskRepository.countByStatus(AsyncTaskStatus.FAILED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAsyncTaskVO getTaskDetail(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务ID不能为空");
        }
        AsyncTaskEntity entity = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到指定的异步任务"));
        return asyncTaskMapper.toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务ID不能为空");
        }
        AsyncTaskEntity entity = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到指定的异步任务"));

        if (entity.getStatus() != AsyncTaskStatus.PENDING && entity.getStatus() != AsyncTaskStatus.RUNNING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前任务已结束（" + entity.getStatus().getDescription() + "），无法取消");
        }

        cancelledTaskIds.add(taskId);
        Instant finishedAt = Instant.now();
        entity.setStatus(AsyncTaskStatus.CANCELLED);
        entity.setFinishedAt(finishedAt);
        if (entity.getStartedAt() != null) {
            entity.setExecutionDurationMs(Duration.between(entity.getStartedAt(), finishedAt).toMillis());
        }
        entity.setStatusMessage("任务已被管理员手动取消");
        asyncTaskRepository.save(entity);
        publishPushEvent(entity);
        log.info("[AsyncTask] 手动取消任务 -> taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务ID不能为空");
        }
        AsyncTaskEntity entity = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到指定的异步任务"));

        if (entity.getStatus() == AsyncTaskStatus.RUNNING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务正在执行中，请先取消后再删除");
        }

        asyncTaskRepository.delete(entity);
        log.info("[AsyncTask] 删除任务记录 -> taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(String status) {
        if (StringUtils.hasText(status)) {
            try {
                AsyncTaskStatus targetStatus = AsyncTaskStatus.valueOf(status.trim().toUpperCase());
                if (targetStatus == AsyncTaskStatus.RUNNING || targetStatus == AsyncTaskStatus.PENDING) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "禁止清空未完成的任务队列");
                }
                int deleted = asyncTaskRepository.deleteByStatus(targetStatus);
                log.info("[AsyncTask] 清空指定状态任务历史 -> status: {}, 共清理 {} 条记录", targetStatus, deleted);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "无效的任务状态: " + status);
            }
        } else {
            int deleted = asyncTaskRepository.clearAllFinishedTasks();
            log.info("[AsyncTask] 清空所有已结束任务历史记录，共清理 {} 条记录", deleted);
        }
    }

    @Override
    public void updateProgress(Long taskId, int progress, String statusMessage) {
        if (taskId == null) return;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            asyncTaskRepository.findById(taskId).ifPresent(entity -> {
                if (entity.getStatus() == AsyncTaskStatus.CANCELLED) {
                    return;
                }
                entity.setProgress(Math.min(100, Math.max(0, progress)));
                if (StringUtils.hasText(statusMessage)) {
                    entity.setStatusMessage(statusMessage);
                }
                asyncTaskRepository.saveAndFlush(entity);
                publishPushEvent(entity);
            });
            return null;
        });
    }

    @Override
    public void completeTask(Long taskId, Object resultData) {
        completeTaskInternal(taskId, null, resultData);
    }

    @Override
    public void failTask(Long taskId, String errorMessage) {
        failTaskInternal(taskId, null, errorMessage);
    }

    private void publishPushEvent(AsyncTaskEntity entity) {
        try {
            AdminAsyncTaskVO vo = asyncTaskMapper.toVO(entity);
            eventPublisher.publishEvent(new AsyncTaskPushEvent(this, vo));
        } catch (Exception e) {
            log.warn("[AsyncTask] 发送任务实时推送事件异常 -> taskId: {}, error: {}", entity.getId(), e.getMessage());
        }
    }

}
