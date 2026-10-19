package com.humolix.volur.container.flag.resolver;

import com.humolix.volur.container.flag.DynamicFlagContext;

/**
 * 动态 Flag 占位符解析策略接口
 */
public interface FlagPlaceholderResolver {

    /**
     * 判断当前解析器是否支持处理指定的占位符
     *
     * @param placeholder 占位符标识（不含外层大括号，例如 "UUID", "RANDOM:16", "USER_ID"）
     * @return 是否支持解析
     */
    boolean supports(String placeholder);

    /**
     * 惰性解析并生成对应的替换值
     *
     * @param placeholder 占位符标识
     * @param context     生成上下文参数
     * @return 解析生成的字符串值
     */
    String resolve(String placeholder, DynamicFlagContext context);

}
