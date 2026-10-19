package com.humolix.volur.logging.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Syslog {

    /**
     * 业务模块名称 (如: SYSTEM, AUTH, USER, CHALLENGE, COMPETITION, CONTAINER, NOTIFICATION)
     */
    String module() default "";

    /**
     * 操作动作描述 (如: 更新系统设置, 发布全站公告, 创建题目等)
     */
    String value() default "";

    /**
     * 是否记录请求入参
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResponse() default false;

    /**
     * 日志分类类型 (如: OPERATE, AUTH, SECURITY, SYSTEM)
     */
    String logType() default "OPERATE";

}
