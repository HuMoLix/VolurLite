package com.humolix.volur.container.init;

import com.humolix.volur.container.service.admin.AdminImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 容器镜像自动化发现与集群节点同步器
 * 在平台启动时通过 Docker API 自动扫描加载本地主机镜像，并依次巡检集群子节点镜像元数据
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class ContainerImageStartupInitializer implements ApplicationRunner {

    private final AdminImageService adminImageService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("[ContainerImageStartupInitializer] 正在执行启动期 Docker 镜像探测与集群节点同步...");
            adminImageService.loadAndSyncClusterImages();
            log.info("[ContainerImageStartupInitializer] 启动期 Docker 镜像与集群节点探测已顺利完成");
        } catch (Exception e) {
            log.warn("[ContainerImageStartupInitializer] 启动期 Docker 镜像扫描提示: {}", e.getMessage());
        }
    }

}
