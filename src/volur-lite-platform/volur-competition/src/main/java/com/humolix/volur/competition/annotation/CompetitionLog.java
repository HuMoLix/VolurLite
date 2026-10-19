package com.humolix.volur.competition.annotation;

import java.lang.annotation.*;

/**
 * 竞赛运行操作日志记录注解
 * 用于标记竞赛生命周期中的核心操作（如容器调度、Flag 提交、队伍变更等），
 * 与全局系统日志（Syslog）实现物理与逻辑隔离。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CompetitionLog {

    /**
     * 日志业务类别 (如: CONTAINER, FLAG, TEAM, DEFENSE, THEORY)
     */
    String logType() default "CONTAINER";

    /**
     * 操作动作描述 (如: 启动靶机容器, 提交 CTF 赛题 Flag 等)
     */
    String action() default "";

    /**
     * 是否记录请求入参
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResponse() default false;

}
