package com.humolix.volur.challenge.service.admin.theory.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTheoryChallengeServiceImpl implements AdminTheoryChallengeService {

    private final TheoryChallengeRepository theoryRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;
    private final VolurThreadManager volurThreadManager;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<TheoryChallengeListVO> listChallenges(Integer page, Integer size, TheoryType type, String category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<TheoryChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("content"), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(theoryRepository.findAll(spec, pageable).map(challengeMapper::toTheoryListVO));
    }

    @Override
    public TheoryChallengeDetailVO getChallenge(Long id) {
        TheoryChallenge challenge = theoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "理论题目不存在"));
        return challengeMapper.toTheoryDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(TheoryChallengeCreateRequestDTO request, Long authorId) {
        if (theoryRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        TheoryChallenge challenge = TheoryChallenge.builder()
                .name(request.getName().trim())
                .category(request.getCategory())
                .type(request.getType())
                .content(request.getContent())
                .options(request.getOptions())
                .standardAnswer(request.getStandardAnswer().trim())
                .explanation(request.getExplanation())
                .score(request.getScore() != null ? request.getScore() : 5)
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .author(author)
                .build();

        return theoryRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, TheoryChallengeUpdateRequestDTO request) {
        TheoryChallenge challenge = theoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "理论题目不存在"));

        if (StringUtils.hasText(request.getName()) && !challenge.getName().equals(request.getName().trim())) {
            if (theoryRepository.existsByName(request.getName().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName().trim());
        }

        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getContent() != null) challenge.setContent(request.getContent());
        if (request.getOptions() != null) challenge.setOptions(request.getOptions());
        if (request.getStandardAnswer() != null) challenge.setStandardAnswer(request.getStandardAnswer().trim());
        if (request.getExplanation() != null) challenge.setExplanation(request.getExplanation());
        if (request.getScore() != null) challenge.setScore(request.getScore());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());

        theoryRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        theoryRepository.deleteById(id);
    }

    @Override
    public ChallengeImportResultVO importChallenges(MultipartFile file, Long authorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的理论题库文件 (JSON 或 ZIP)");
        }

        return volurThreadManager.supplyCompute(() -> doProcessTheoryImport(file, authorId)).join();
    }

    private ChallengeImportResultVO doProcessTheoryImport(MultipartFile file, Long authorId) {
        List<String> errors = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        String jsonContent = null;
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                        jsonContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("[AdminTheoryChallengeService] 解压理论题库 ZIP 失败", e);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "解析 ZIP 压缩包失败: " + e.getMessage());
            }
        } else {
            try {
                jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("[AdminTheoryChallengeService] 读取理论题库 JSON 失败", e);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "读取 JSON 文件失败: " + e.getMessage());
            }
        }

        if (!StringUtils.hasText(jsonContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未读取到有效的 JSON 题目数据内容");
        }

        List<JsonNode> items = new ArrayList<>();
        try {
            JsonNode parsed = objectMapper.readTree(jsonContent);
            if (parsed.isArray()) {
                for (JsonNode node : parsed) {
                    items.add(node);
                }
            } else if (parsed.isObject()) {
                if (parsed.hasNonNull("questions") && parsed.get("questions").isArray()) {
                    for (JsonNode node : parsed.get("questions")) {
                        items.add(node);
                    }
                } else {
                    items.add(parsed);
                }
            } else {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题目格式不符合规范");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminTheoryChallengeService] 解析理论题库 JSON 语法错误", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析题目 JSON 语法错误: " + e.getMessage());
        }

        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            try {
                String name = item.hasNonNull("name") ? item.get("name").asText() : null;
                if (!StringUtils.hasText(name) && item.hasNonNull("title")) {
                    name = item.get("title").asText();
                }
                if (!StringUtils.hasText(name)) {
                    errors.add("第 " + (i + 1) + " 条题目名称不能为空");
                    failedCount++;
                    continue;
                }

                name = name.trim();
                if (theoryRepository.existsByName(name)) {
                    errors.add("题目【" + name + "】导入失败: 已存在同名题目");
                    failedCount++;
                    continue;
                }

                String content = item.hasNonNull("content") ? item.get("content").asText() : null;
                if (!StringUtils.hasText(content) && item.hasNonNull("question")) {
                    content = item.get("question").asText();
                }
                if (!StringUtils.hasText(content)) {
                    errors.add("题目【" + name + "】题干内容不能为空");
                    failedCount++;
                    continue;
                }

                String standardAnswer = item.hasNonNull("standardAnswer") ? item.get("standardAnswer").asText() : null;
                if (!StringUtils.hasText(standardAnswer) && item.hasNonNull("answer")) {
                    standardAnswer = item.get("answer").asText();
                }
                if (!StringUtils.hasText(standardAnswer)) {
                    errors.add("题目【" + name + "】标准答案不能为空");
                    failedCount++;
                    continue;
                }

                TheoryType type = parseTheoryType(item.hasNonNull("type") ? item.get("type").asText() : null);
                String options = item.hasNonNull("options") ? item.get("options").asText() : null;
                if (options == null && item.hasNonNull("choices")) {
                    JsonNode choicesNode = item.get("choices");
                    options = choicesNode.isTextual() ? choicesNode.asText() : choicesNode.toString();
                }

                String explanation = item.hasNonNull("explanation") ? item.get("explanation").asText() : null;
                Integer score = item.hasNonNull("score") && item.get("score").isNumber() ? item.get("score").asInt() : 5;
                Integer difficulty = item.hasNonNull("difficulty") && item.get("difficulty").isNumber() ? item.get("difficulty").asInt() : 1;
                String category = item.hasNonNull("category") ? item.get("category").asText() : null;
                if (!StringUtils.hasText(category)) {
                    category = "理论知识";
                }

                TheoryChallenge entity = TheoryChallenge.builder()
                        .name(name)
                        .category(category.trim())
                        .type(type)
                        .content(content)
                        .options(options)
                        .standardAnswer(standardAnswer.trim())
                        .explanation(explanation)
                        .score(score)
                        .difficulty(difficulty)
                        .author(author)
                        .build();

                TheoryChallenge saved = theoryRepository.save(entity);
                importedIds.add(saved.getId());
                successCount++;
            } catch (Exception ex) {
                log.error("[AdminTheoryChallengeService] 导入单个理论题目失败: index={}", i, ex);
                errors.add("第 " + (i + 1) + " 条题目处理失败: " + ex.getMessage());
                failedCount++;
            }
        }

        return ChallengeImportResultVO.builder()
                .totalCount(items.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .importedChallengeIds(importedIds)
                .build();
    }

    @Override
    public void exportChallenges(List<Long> challengeIds, HttpServletResponse response) {
        List<TheoryChallenge> list;
        if (challengeIds != null && !challengeIds.isEmpty()) {
            list = theoryRepository.findAllById(challengeIds);
        } else {
            list = theoryRepository.findAll();
        }

        if (list.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "没有找到可供导出的理论题目");
        }

        ArrayNode array = objectMapper.createArrayNode();
        for (TheoryChallenge tc : list) {
            ObjectNode obj = objectMapper.createObjectNode();
            obj.put("name", tc.getName());
            obj.put("category", tc.getCategory());
            obj.put("type", tc.getType() != null ? tc.getType().name() : "SINGLE_CHOICE");
            obj.put("content", tc.getContent());
            obj.put("options", tc.getOptions());
            obj.put("standardAnswer", tc.getStandardAnswer());
            obj.put("explanation", tc.getExplanation());
            obj.put("score", tc.getScore());
            obj.put("difficulty", tc.getDifficulty());
            array.add(obj);
        }

        try {
            response.setContentType("application/json;charset=UTF-8");
            String fileName = "theory-challenges-export.json";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(array);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("[AdminTheoryChallengeService] 导出理论题目 JSON 失败", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出理论题目失败: " + e.getMessage());
        }
    }

    private TheoryType parseTheoryType(String typeStr) {
        if (!StringUtils.hasText(typeStr)) {
            return TheoryType.SINGLE_CHOICE;
        }
        String t = typeStr.trim().toUpperCase();
        return switch (t) {
            case "SINGLE", "SINGLE_CHOICE", "单选", "单选题" -> TheoryType.SINGLE_CHOICE;
            case "MULTI", "MULTIPLE", "MULTIPLE_CHOICE", "多选", "多选题" -> TheoryType.MULTIPLE_CHOICE;
            case "JUDGE", "TRUE_FALSE", "BOOLEAN", "判断", "判断题" -> TheoryType.TRUE_FALSE;
            case "ESSAY", "TEXT", "QA", "问答", "问答题", "简答题" -> TheoryType.ESSAY;
            default -> TheoryType.SINGLE_CHOICE;
        };
    }

}
