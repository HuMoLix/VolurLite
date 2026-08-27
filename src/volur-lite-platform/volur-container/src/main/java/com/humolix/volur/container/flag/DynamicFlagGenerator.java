package com.humolix.volur.container.flag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 动态 Flag 模版生成引擎门面服务
 * 支持接收赛事/题目独立自定义 Flag 模版（如 flag{{{UUID}}}, ciscn{{{RANDOM:16}}} 等）并高效解析占位符生成动态 Flag
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicFlagGenerator {

    public static final String DEFAULT_FLAG_TEMPLATE = "flag{{UUID}}";

    private final FlagTemplateParser flagTemplateParser;

    /**
     * 根据指定的 Flag 模版与上下文生成动态 Flag
     *
     * @param template Flag 模版字符串（如为空则回退到默认 flag{{{UUID}}}）
     * @param context  生成上下文
     * @return 解析完成的动态 Flag
     */
    public String generateFlag(String template, DynamicFlagContext context) {
        String effectiveTemplate = StringUtils.hasText(template) ? template : DEFAULT_FLAG_TEMPLATE;
        return evaluateTemplate(effectiveTemplate, context);
    }

    /**
     * 使用默认模版生成动态 Flag
     *
     * @param context 生成上下文
     * @return 解析完成的动态 Flag
     */
    public String generateFlag(DynamicFlagContext context) {
        return generateFlag(DEFAULT_FLAG_TEMPLATE, context);
    }

    /**
     * 评估并解析指定模版
     */
    public String evaluateTemplate(String template, DynamicFlagContext context) {
        return flagTemplateParser.parseAndEvaluate(template, context);
    }

    /**
     * 提取模版中所有占位符
     */
    public Set<String> extractPlaceholders(String template) {
        return flagTemplateParser.extractPlaceholders(template);
    }

    /**
     * 校验模版格式合法性
     */
    public List<String> validateTemplate(String template) {
        return flagTemplateParser.findUnsupportedPlaceholders(template);
    }

    /**
     * 预览模版渲染效果
     */
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
