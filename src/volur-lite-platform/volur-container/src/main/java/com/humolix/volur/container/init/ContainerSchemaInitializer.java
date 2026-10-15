package com.humolix.volur.container.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 容器模块数据库表结构自适应调整与升级器
 * 确保历史数据库中的 ctf_challenge_instance 表满足独立容器运行要求（将 challenge_id 调整为 NULL 允许）
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class ContainerSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE ctf_challenge_instance MODIFY COLUMN challenge_id BIGINT NULL");
            log.info("[ContainerSchemaInitializer] 成功更新 ctf_challenge_instance 表结构: challenge_id 列已支持 NULL");
        } catch (Exception e) {
            log.debug("[ContainerSchemaInitializer] 执行表结构自适应调整提示: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE ctf_challenge_instance ADD COLUMN container_type VARCHAR(32) NULL");
            log.info("[ContainerSchemaInitializer] 成功更新 ctf_challenge_instance 表结构: 添加 container_type 字段");
        } catch (Exception e) {
            log.debug("[ContainerSchemaInitializer] 添加 container_type 字段提示: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE ctf_challenge_instance ADD COLUMN team_id BIGINT NULL");
            log.info("[ContainerSchemaInitializer] 成功更新 ctf_challenge_instance 表结构: 添加 team_id 字段");
        } catch (Exception e) {
            log.debug("[ContainerSchemaInitializer] 添加 team_id 字段提示: {}", e.getMessage());
        }
    }

}
