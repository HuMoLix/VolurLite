package com.humolix.volur.container.flag;

import com.humolix.volur.container.flag.resolver.FlagPlaceholderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高性能 Flag 模版解析器
 * 基于单次正则扫描与惰性求值策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlagTemplateParser {

    /**
     * 匹配形如 {UUID}, {RANDOM:16}, {TEAM_TOKEN} 的占位符
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_:]+)}");

    private final List<FlagPlaceholderResolver> resolvers;

    /**
     * 解析并评估模版生成最终 Flag
     *
     * @param template 模版字符串
     * @param context  生成上下文
     * @return 解析完成的 Flag
     */
    public String parseAndEvaluate(String template, DynamicFlagContext context) {
        if (!StringUtils.hasText(template)) {
            template = DynamicFlagGenerator.DEFAULT_FLAG_TEMPLATE;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder(template.length() + 64);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(placeholder, context);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 提取模版中所有的占位符名称
     *
     * @param template 模版字符串
     * @return 占位符集合（不含外层大括号）
     */
    public Set<String> extractPlaceholders(String template) {
        if (!StringUtils.hasText(template)) {
            return Collections.emptySet();
        }
        Set<String> placeholders = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    /**
     * 校验模版格式合法性
     *
     * @param template 模版字符串
     * @return 模版中未识别或不支持的占位符列表（若为空则说明全部合法）
     */
    public List<String> findUnsupportedPlaceholders(String template) {
        Set<String> placeholders = extractPlaceholders(template);
        List<String> unsupported = new ArrayList<>();
        for (String placeholder : placeholders) {
            boolean supported = resolvers.stream().anyMatch(r -> r.supports(placeholder));
            if (!supported) {
                unsupported.add(placeholder);
            }
        }
        return unsupported;
    }

    private String resolvePlaceholder(String placeholder, DynamicFlagContext context) {
        for (FlagPlaceholderResolver resolver : resolvers) {
            if (resolver.supports(placeholder)) {
                try {
                    return resolver.resolve(placeholder, context);
                } catch (Exception ex) {
                    log.error("Failed to resolve flag placeholder '{}': {}", placeholder, ex.getMessage(), ex);
                    return "ERROR";
                }
            }
        }
        // 若无匹配的解析器，保留原占位符以防误伤
        return "{" + placeholder + "}";
    }

}
