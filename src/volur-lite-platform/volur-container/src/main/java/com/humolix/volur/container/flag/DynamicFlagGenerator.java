package com.humolix.volur.container.flag;

import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 动态 Flag 生成器门面服务
 * 支持根据系统设置中配置的自定义模版高效解析占位符并生成动态 Flag
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicFlagGenerator {

    public static final String DEFAULT_FLAG_TEMPLATE = "flag{{{UUID}}}";
    public static final String SETTING_DYNAMIC_FLAG_TEMPLATE = "DYNAMIC_FLAG_TEMPLATE";
    public static final String SETTING_FLAG_TEMPLATE = "FLAG_TEMPLATE";
    public static final String SETTING_FLAG_PREFIX = "FLAG_PREFIX";
    public static final String SETTING_FLAG_SUFFIX = "FLAG_SUFFIX";

    private final SystemSettingService systemSettingService;
    private final FlagTemplateParser flagTemplateParser;

    public String generateFlag(DynamicFlagContext context) {
        String template = getEffectiveTemplate();
        return evaluateTemplate(template, context);
    }

    public String evaluateTemplate(String template, DynamicFlagContext context) {
        return flagTemplateParser.parseAndEvaluate(template, context);
    }

    public String getEffectiveTemplate() {
        String template = systemSettingService.getSettingValue(SETTING_DYNAMIC_FLAG_TEMPLATE, null);
        if (!StringUtils.hasText(template)) {
            template = systemSettingService.getSettingValue(SETTING_FLAG_TEMPLATE, null);
        }
        if (!StringUtils.hasText(template)) {
            String prefix = systemSettingService.getSettingValue(SETTING_FLAG_PREFIX, "flag{");
            String suffix = systemSettingService.getSettingValue(SETTING_FLAG_SUFFIX, "}");
            template = prefix + "{UUID}" + suffix;
        }
        return template;
    }

    public Set<String> extractPlaceholders(String template) {
        return flagTemplateParser.extractPlaceholders(template);
    }

    public List<String> validateTemplate(String template) {
        return flagTemplateParser.findUnsupportedPlaceholders(template);
    }

    public String previewFlag(String template) {
        DynamicFlagContext sampleContext = DynamicFlagContext.builder()
                .userId(1001L)
                .username("Alice")
                .challengeId(2001L)
                .challengeKey("web_sql_injection")
                .competitionId(101L)
                .stageId(1L)
                .teamId(501L)
                .teamToken("VOLUR_TEAM_DEMO")
                .build();
        return evaluateTemplate(template, sampleContext);
    }

}
