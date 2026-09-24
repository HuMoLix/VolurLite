package com.humolix.volur.framework.common.validation.provider;

/**
 * 邮箱域名白名单后缀提供者接口
 * <p>
 * 解耦通用框架层与业务配置模块，用于在参数校验阶段动态获取系统设定的邮箱白名单规则。
 * </p>
 */
@FunctionalInterface
public interface EmailWhitelistProvider {

    /**
     * 获取允许注册或使用的邮箱域名后缀（逗号分隔的字符串，如 "edu.cn,humolix.com"）
     *
     * @return 邮箱域名后缀列表字符串，为空或 null 时表示不限制
     */
    String getEmailWhitelistSuffixes();
}
