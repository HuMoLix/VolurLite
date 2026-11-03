package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.CompetitionLogContextDTO;

public interface CompetitionLogPersistenceService {

    /**
     * 异步落库单条竞赛操作日志（推入 Redis 缓冲队列并通过虚拟线程自适应微批落库）
     *
     * @param context 日志快照上下文
     */
    void saveLogAsync(CompetitionLogContextDTO context);

    /**
     * 强制刷新当前 Redis 缓冲区中的日志并批量持久化至数据库（保障查询强一致与实时性）
     */
    void flushBuffer();

}
