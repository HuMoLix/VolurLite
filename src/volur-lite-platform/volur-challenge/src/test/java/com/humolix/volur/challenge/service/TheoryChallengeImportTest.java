package com.humolix.volur.challenge.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeImportDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class TheoryChallengeImportTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    public void testParseAndValidateTemplateJson() throws Exception {
        File file = new File("H:/HuMoLix/github/VolurCTF2026/theory/模拟卷一_reverted.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(file.exists(), "Template file must exist");

        String content = Files.readString(file.toPath());
        List<TheoryChallengeImportDTO> list = objectMapper.readValue(content, new TypeReference<List<TheoryChallengeImportDTO>>() {});

        Assertions.assertNotNull(list);
        Assertions.assertEquals(300, list.size());

        int singleCount = 0;
        int multiCount = 0;
        int tfCount = 0;

        for (int i = 0; i < list.size(); i++) {
            TheoryChallengeImportDTO dto = list.get(i);
            int idx = i + 1;
            dto.validate(idx);

            TheoryType type = dto.resolveTheoryType();
            String opts = dto.resolveOptionsString(objectMapper);
            String ans = dto.resolveNormalizedAnswer();

            Assertions.assertNotNull(ans);
            Assertions.assertFalse(ans.isEmpty());

            if (type == TheoryType.SINGLE_CHOICE) {
                singleCount++;
                Assertions.assertNotNull(opts);
            } else if (type == TheoryType.MULTIPLE_CHOICE) {
                multiCount++;
                Assertions.assertNotNull(opts);
            } else if (type == TheoryType.TRUE_FALSE) {
                tfCount++;
                Assertions.assertTrue("TRUE".equals(ans) || "FALSE".equals(ans));
            }
        }

        Assertions.assertEquals(150, singleCount);
        Assertions.assertEquals(60, multiCount);
        Assertions.assertEquals(90, tfCount);
    }

    @Test
    public void testValidateMissingFields() {
        TheoryChallengeImportDTO dtoNoName = new TheoryChallengeImportDTO();
        dtoNoName.setContent("题干");
        dtoNoName.setType("SINGLE_CHOICE");
        dtoNoName.setStandardAnswer("A");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dtoNoName.validate(1));

        TheoryChallengeImportDTO dtoNoContent = new TheoryChallengeImportDTO();
        dtoNoContent.setName("题目");
        dtoNoContent.setType("SINGLE_CHOICE");
        dtoNoContent.setStandardAnswer("A");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dtoNoContent.validate(1));

        TheoryChallengeImportDTO dtoNoAnswer = new TheoryChallengeImportDTO();
        dtoNoAnswer.setName("题目");
        dtoNoAnswer.setContent("题干");
        dtoNoAnswer.setType("SINGLE_CHOICE");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dtoNoAnswer.validate(1));

        TheoryChallengeImportDTO dtoNoOptions = new TheoryChallengeImportDTO();
        dtoNoOptions.setName("题目");
        dtoNoOptions.setContent("题干");
        dtoNoOptions.setType("SINGLE_CHOICE");
        dtoNoOptions.setStandardAnswer("A");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dtoNoOptions.validate(1));
    }

}

