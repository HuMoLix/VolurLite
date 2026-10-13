package com.humolix.volur.challenge.domain.dto.theory;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheoryChallengeImportDTO {

    @Trimmed
    @JsonAlias({"title", "questionTitle"})
    private String name;

    @Trimmed
    @JsonAlias({"question", "prompt"})
    private String content;

    @Trimmed
    @JsonAlias({"questionType"})
    private String type;

    @JsonAlias({"choices", "selections"})
    private JsonNode options;

    @Trimmed
    @JsonAlias({"answer", "correctAnswer"})
    private String standardAnswer;

    @Trimmed
    private String category;

    @Trimmed
    private String explanation;

    private Integer score;

    private Integer difficulty;

    public TheoryType resolveTheoryType() {
        if (!StringUtils.hasText(type)) {
            return TheoryType.SINGLE_CHOICE;
        }
        String t = type.trim().toUpperCase();
        return switch (t) {
            case "SINGLE", "SINGLE_CHOICE", "单选", "单选题" -> TheoryType.SINGLE_CHOICE;
            case "MULTI", "MULTIPLE", "MULTIPLE_CHOICE", "多选", "多选题" -> TheoryType.MULTIPLE_CHOICE;
            case "JUDGE", "TRUE_FALSE", "BOOLEAN", "判断", "判断题" -> TheoryType.TRUE_FALSE;
            case "ESSAY", "TEXT", "QA", "问答", "问答题", "简答题" -> TheoryType.ESSAY;
            default -> TheoryType.SINGLE_CHOICE;
        };
    }

    public String resolveOptionsString(ObjectMapper mapper) {
        if (options == null || options.isNull()) {
            TheoryType theoryType = resolveTheoryType();
            if (theoryType == TheoryType.TRUE_FALSE) {
                return "[{\"key\":\"A\",\"val\":\"正确\"},{\"key\":\"B\",\"val\":\"错误\"}]";
            }
            return null;
        }
        if (options.isTextual()) {
            return options.asText().trim();
        }
        try {
            return mapper.writeValueAsString(options);
        } catch (Exception e) {
            return options.toString();
        }
    }

    public String resolveNormalizedAnswer() {
        if (!StringUtils.hasText(standardAnswer)) {
            return "";
        }
        TheoryType theoryType = resolveTheoryType();
        String ans = standardAnswer.trim();

        if (theoryType == TheoryType.TRUE_FALSE) {
            String upper = ans.toUpperCase();
            if (upper.equals("正确") || upper.equals("对") || upper.equals("TRUE") || upper.equals("T") || upper.equals("A")) {
                return "TRUE";
            }
            if (upper.equals("错误") || upper.equals("错") || upper.equals("FALSE") || upper.equals("F") || upper.equals("B")) {
                return "FALSE";
            }
            return upper;
        }

        if (theoryType == TheoryType.MULTIPLE_CHOICE) {
            List<String> letters = new ArrayList<>();
            for (char ch : ans.toUpperCase().toCharArray()) {
                if (ch >= 'A' && ch <= 'Z') {
                    String s = String.valueOf(ch);
                    if (!letters.contains(s)) {
                        letters.add(s);
                    }
                }
            }
            if (!letters.isEmpty()) {
                Collections.sort(letters);
                return String.join(",", letters);
            }
            return ans.toUpperCase();
        }

        return ans.toUpperCase();
    }

    public void validate(int index) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("第 " + index + " 条题目缺少题目名称 (name)");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("第 " + index + " 条题目【" + name.trim() + "】缺少题干内容 (content)");
        }
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("第 " + index + " 条题目【" + name.trim() + "】缺少题目类型 (type)");
        }
        if (!StringUtils.hasText(standardAnswer)) {
            throw new IllegalArgumentException("第 " + index + " 条题目【" + name.trim() + "】缺少标准答案 (standardAnswer)");
        }
        TheoryType theoryType = resolveTheoryType();
        if (theoryType == TheoryType.SINGLE_CHOICE || theoryType == TheoryType.MULTIPLE_CHOICE) {
            if (options == null || options.isNull() || (options.isObject() && options.isEmpty()) || (options.isArray() && options.isEmpty())) {
                throw new IllegalArgumentException("第 " + index + " 条选择题【" + name.trim() + "】缺少选项配置 (options)");
            }
        }
    }

}
