package com.humolix.volur.challenge.service.admin.theory.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.humolix.volur.challenge.domain.dto.theory.TheoryBankCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryBankUpdateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeImportDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryBankImportDTO;
import com.humolix.volur.challenge.domain.entity.theory.TheoryBank;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankListVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.theory.TheoryBankRepository;
import com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTheoryChallengeServiceImpl implements AdminTheoryChallengeService {

    private final TheoryBankRepository bankRepository;
    private final TheoryChallengeRepository theoryRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;
    private final VolurThreadManager volurThreadManager;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<TheoryBankListVO> listBanks(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<TheoryBank> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                Join<TheoryBank, User> authorJoin = root.join("author", JoinType.LEFT);
                Predicate nameMatch = cb.like(root.get("name"), pattern);
                Predicate userMatch = cb.like(authorJoin.get("username"), pattern);
                Predicate nickMatch = cb.like(authorJoin.get("nickname"), pattern);
                predicates.add(cb.or(nameMatch, userMatch, nickMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TheoryBank> bankPage = bankRepository.findAll(spec, pageable);
        List<TheoryBank> banks = bankPage.getContent();
        List<Long> bankIds = banks.stream().map(TheoryBank::getId).toList();
        Map<Long, Long> questionCountMap = new HashMap<>();
        if (!bankIds.isEmpty()) {
            List<Object[]> counts = theoryRepository.countQuestionsByBankIds(bankIds);
            for (Object[] row : counts) {
                questionCountMap.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        List<TheoryBankListVO> voList = banks.stream().map(bank -> {
            TheoryBankListVO vo = challengeMapper.toTheoryBankListVO(bank);
            vo.setQuestionCount(questionCountMap.getOrDefault(bank.getId(), 0L));
            return vo;
        }).toList();

        return PageResponse.of(voList, bankPage.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Override
    public TheoryBankDetailVO getBank(Long id) {
        TheoryBank bank = bankRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题库不存在"));
        TheoryBankDetailVO vo = challengeMapper.toTheoryBankDetailVO(bank);
        vo.setQuestionCount(theoryRepository.countByBankId(bank.getId()));
        return vo;
    }

    @Override
    @Transactional
    public Long createBank(TheoryBankCreateRequestDTO request, Long authorId) {
        if (bankRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题库名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        TheoryBank bank = TheoryBank.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : "通用")
                .author(author)
                .build();

        return bankRepository.save(bank).getId();
    }

    @Override
    @Transactional
    public void updateBank(Long id, TheoryBankUpdateRequestDTO request) {
        TheoryBank bank = bankRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题库不存在"));

        if (StringUtils.hasText(request.getName()) && !bank.getName().equals(request.getName().trim())) {
            if (bankRepository.existsByNameAndIdNot(request.getName().trim(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题库名称已存在");
            }
            bank.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            bank.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getCategory())) {
            bank.setCategory(request.getCategory().trim());
        }

        bankRepository.save(bank);
    }

    @Override
    @Transactional
    public void deleteBank(Long id) {
        TheoryBank bank = bankRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题库不存在"));

        theoryRepository.deleteByBankId(bank.getId());
        bankRepository.deleteById(bank.getId());
    }

    @Override
    public ChallengeImportResultVO importBanks(MultipartFile file, Long authorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的题库文件 (JSON 或 ZIP)");
        }

        return volurThreadManager.supplyCompute(() -> doProcessBanksImport(file, authorId)).join();
    }

    private ChallengeImportResultVO doProcessBanksImport(MultipartFile file, Long authorId) {
        List<String> errors = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();

        User author = authorId != null ? userRepository.findById(authorId).orElse(null) : null;
        String jsonContent = extractJsonContent(file);

        if (!StringUtils.hasText(jsonContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未读取到有效的 JSON 题库数据内容");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (root == null || root.isNull() || root.isMissingNode()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题库数据内容为空");
            }

            if (root.isArray()) {
                if (root.isEmpty()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题库数据为空");
                }
                JsonNode first = root.get(0);
                if (first != null && (first.hasNonNull("questions") || first.hasNonNull("challenges") || first.hasNonNull("items"))) {
                    List<TheoryBankImportDTO> bankDTOs = objectMapper.convertValue(root, new TypeReference<List<TheoryBankImportDTO>>() {});
                    for (int bIdx = 0; bIdx < bankDTOs.size(); bIdx++) {
                        TheoryBankImportDTO bDto = bankDTOs.get(bIdx);
                        importSingleBank(bDto, author, importedIds, errors, bIdx + 1);
                    }
                } else {
                    String baseBankName = resolveFileNameWithoutExt(file.getOriginalFilename());
                    if (!StringUtils.hasText(baseBankName)) {
                        baseBankName = "导入题库_" + System.currentTimeMillis();
                    }
                    TheoryBank bank = getOrCreateBank(baseBankName, "从文件导入的题库", "通用", author);
                    List<TheoryChallengeImportDTO> qList = objectMapper.convertValue(root, new TypeReference<List<TheoryChallengeImportDTO>>() {});
                    importQuestionsIntoBank(qList, bank, author, importedIds, errors);
                }
            } else if (root.isObject()) {
                if (root.hasNonNull("banks") && root.get("banks").isArray()) {
                    List<TheoryBankImportDTO> bankDTOs = objectMapper.convertValue(root.get("banks"), new TypeReference<List<TheoryBankImportDTO>>() {});
                    for (int bIdx = 0; bIdx < bankDTOs.size(); bIdx++) {
                        TheoryBankImportDTO bDto = bankDTOs.get(bIdx);
                        importSingleBank(bDto, author, importedIds, errors, bIdx + 1);
                    }
                } else if (root.hasNonNull("questions") && root.get("questions").isArray()) {
                    TheoryBankImportDTO bDto = objectMapper.convertValue(root, TheoryBankImportDTO.class);
                    if (!StringUtils.hasText(bDto.getName())) {
                        bDto.setName(resolveFileNameWithoutExt(file.getOriginalFilename()));
                    }
                    importSingleBank(bDto, author, importedIds, errors, 1);
                } else if (root.hasNonNull("content") || root.hasNonNull("name") || root.hasNonNull("question")) {
                    String baseBankName = resolveFileNameWithoutExt(file.getOriginalFilename());
                    TheoryBank bank = getOrCreateBank(baseBankName, "导入题库", "通用", author);
                    TheoryChallengeImportDTO qDto = objectMapper.convertValue(root, TheoryChallengeImportDTO.class);
                    importQuestionsIntoBank(List.of(qDto), bank, author, importedIds, errors);
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题库数据结构不符合规范");
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminTheoryChallengeService] 解析题库导入 JSON 语法错误", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析题库 JSON 语法错误: " + e.getMessage());
        }

        int successCount = importedIds.size();
        int failedCount = errors.size();

        return ChallengeImportResultVO.builder()
                .totalCount(successCount + failedCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .importedChallengeIds(importedIds)
                .build();
    }

    private void importSingleBank(TheoryBankImportDTO bDto, User author, List<Long> importedIds, List<String> errors, int bankIndex) {
        if (bDto == null) {
            errors.add("第 " + bankIndex + " 个题库数据为空");
            return;
        }
        String bankName = StringUtils.hasText(bDto.getName()) ? bDto.getName().trim() : "导入题库_" + System.currentTimeMillis();
        String desc = bDto.getDescription();
        String cat = StringUtils.hasText(bDto.getCategory()) ? bDto.getCategory().trim() : "通用";

        TheoryBank bank = getOrCreateBank(bankName, desc, cat, author);
        if (bDto.getQuestions() != null && !bDto.getQuestions().isEmpty()) {
            importQuestionsIntoBank(bDto.getQuestions(), bank, author, importedIds, errors);
        }
    }

    private void importQuestionsIntoBank(List<TheoryChallengeImportDTO> qList, TheoryBank bank, User author, List<Long> importedIds, List<String> errors) {
        if (qList == null || qList.isEmpty()) {
            return;
        }
        Set<String> processedNames = new HashSet<>();
        for (int i = 0; i < qList.size(); i++) {
            TheoryChallengeImportDTO item = qList.get(i);
            int idx = i + 1;
            try {
                if (item == null) {
                    throw new IllegalArgumentException("第 " + idx + " 条题目数据为空");
                }
                item.validate(idx);

                String trimmedName = item.getName().trim();
                if (!processedNames.add(trimmedName.toLowerCase())) {
                    throw new IllegalArgumentException("题库【" + bank.getName() + "】下第 " + idx + " 条题目【" + trimmedName + "】在本次导入中重复");
                }
                if (theoryRepository.existsByName(trimmedName)) {
                    throw new IllegalArgumentException("题库【" + bank.getName() + "】下第 " + idx + " 条题目【" + trimmedName + "】在数据库中已存在同名题目");
                }

                TheoryChallenge entity = buildChallengeEntity(item, bank, author);
                TheoryChallenge saved = theoryRepository.save(entity);
                importedIds.add(saved.getId());
            } catch (Exception ex) {
                errors.add(ex.getMessage());
            }
        }
    }

    private TheoryBank getOrCreateBank(String name, String description, String category, User author) {
        return bankRepository.findByName(name.trim()).orElseGet(() ->
                bankRepository.save(TheoryBank.builder()
                        .name(name.trim())
                        .description(description)
                        .category(category)
                        .author(author)
                        .build())
        );
    }

    private String resolveFileNameWithoutExt(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "导入题库_" + System.currentTimeMillis();
        }
        String name = originalFilename;
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.trim();
    }

    @Override
    public void exportBanks(List<Long> bankIds, HttpServletResponse response) {
        List<TheoryBank> banks;
        if (bankIds != null && !bankIds.isEmpty()) {
            banks = bankRepository.findAllById(bankIds);
        } else {
            banks = bankRepository.findAll();
        }

        if (banks.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "没有找到可供导出的题库");
        }

        ArrayNode rootArray = objectMapper.createArrayNode();
        for (TheoryBank bank : banks) {
            ObjectNode bObj = objectMapper.createObjectNode();
            bObj.put("id", bank.getId());
            bObj.put("name", bank.getName());
            bObj.put("description", bank.getDescription());
            bObj.put("category", bank.getCategory());

            List<TheoryChallenge> questions = theoryRepository.findByBankId(bank.getId());
            ArrayNode qArray = objectMapper.createArrayNode();
            for (TheoryChallenge tc : questions) {
                ObjectNode qObj = objectMapper.createObjectNode();
                qObj.put("name", tc.getName());
                qObj.put("category", tc.getCategory());
                qObj.put("type", tc.getType() != null ? tc.getType().name() : "SINGLE_CHOICE");
                qObj.put("content", tc.getContent());
                qObj.put("options", tc.getOptions());
                qObj.put("standardAnswer", tc.getStandardAnswer());
                qObj.put("explanation", tc.getExplanation());
                qObj.put("score", tc.getScore());
                qObj.put("difficulty", tc.getDifficulty());
                qArray.add(qObj);
            }
            bObj.set("questions", qArray);
            rootArray.add(bObj);
        }

        writeJsonResponse(response, "theory-banks-export.json", rootArray);
    }

    @Override
    public PageResponse<TheoryChallengeListVO> listBankQuestions(Long bankId, Integer page, Integer size, TheoryType type, String category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<TheoryChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("bank").get("id"), bankId));
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
    public ChallengeImportResultVO importQuestionsToBank(Long bankId, MultipartFile file, Long authorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的题目文件 (JSON 或 ZIP)");
        }

        TheoryBank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定题库不存在"));

        return volurThreadManager.supplyCompute(() -> doProcessQuestionsImport(bank, file, authorId)).join();
    }

    private ChallengeImportResultVO doProcessQuestionsImport(TheoryBank bank, MultipartFile file, Long authorId) {
        List<String> errors = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();

        User author = authorId != null ? userRepository.findById(authorId).orElse(null) : null;
        String jsonContent = extractJsonContent(file);

        if (!StringUtils.hasText(jsonContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未读取到有效的 JSON 题目数据内容");
        }

        List<TheoryChallengeImportDTO> items = parseToChallengeImportDTOList(jsonContent);
        if (items.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未包含任何可导入的题目数据");
        }

        Set<String> processedNames = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            TheoryChallengeImportDTO item = items.get(i);
            int idx = i + 1;
            try {
                if (item == null) {
                    throw new IllegalArgumentException("第 " + idx + " 条题目数据为空");
                }
                item.validate(idx);

                String trimmedName = item.getName().trim();
                if (!processedNames.add(trimmedName.toLowerCase())) {
                    throw new IllegalArgumentException("第 " + idx + " 条题目【" + trimmedName + "】在本次导入文件中重复出现");
                }
                if (theoryRepository.existsByName(trimmedName)) {
                    throw new IllegalArgumentException("第 " + idx + " 条题目【" + trimmedName + "】在数据库中已存在同名题目");
                }

                TheoryChallenge entity = buildChallengeEntity(item, bank, author);
                TheoryChallenge saved = theoryRepository.save(entity);
                importedIds.add(saved.getId());
            } catch (Exception ex) {
                errors.add(ex.getMessage());
            }
        }

        int successCount = importedIds.size();
        int failedCount = errors.size();

        return ChallengeImportResultVO.builder()
                .totalCount(items.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .importedChallengeIds(importedIds)
                .build();
    }

    @Override
    public void exportQuestionsByBank(Long bankId, List<Long> questionIds, HttpServletResponse response) {
        List<TheoryChallenge> list;
        if (questionIds != null && !questionIds.isEmpty()) {
            list = theoryRepository.findAllById(questionIds).stream()
                    .filter(q -> q.getBank() != null && q.getBank().getId().equals(bankId))
                    .toList();
        } else {
            list = theoryRepository.findByBankId(bankId);
        }

        if (list.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "当前题库无题目可供导出");
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

        writeJsonResponse(response, "bank-" + bankId + "-questions-export.json", array);
    }

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

        TheoryBank bank = null;
        if (request.getBankId() != null) {
            bank = bankRepository.findById(request.getBankId()).orElse(null);
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
                .bank(bank)
                .author(author)
                .build();

        return theoryRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, TheoryChallengeUpdateRequestDTO request) {
        TheoryChallenge challenge = theoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "理论题目不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !challenge.getName().equals(request.getName())) {
            if (theoryRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName());
        }

        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getContent() != null) challenge.setContent(request.getContent());
        if (request.getOptions() != null) challenge.setOptions(request.getOptions());
        if (request.getStandardAnswer() != null) challenge.setStandardAnswer(request.getStandardAnswer().trim());
        if (request.getExplanation() != null) challenge.setExplanation(request.getExplanation());
        if (request.getScore() != null) challenge.setScore(request.getScore());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());

        if (request.getBankId() != null) {
            TheoryBank bank = bankRepository.findById(request.getBankId()).orElse(null);
            challenge.setBank(bank);
        }

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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的理论题目文件 (JSON 或 ZIP)");
        }

        return volurThreadManager.supplyCompute(() -> doProcessTheoryImport(file, authorId)).join();
    }

    private ChallengeImportResultVO doProcessTheoryImport(MultipartFile file, Long authorId) {
        List<String> errors = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();

        User author = authorId != null ? userRepository.findById(authorId).orElse(null) : null;
        String jsonContent = extractJsonContent(file);

        if (!StringUtils.hasText(jsonContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未读取到有效的 JSON 题目数据内容");
        }

        List<TheoryChallengeImportDTO> items = parseToChallengeImportDTOList(jsonContent);
        TheoryBank defaultBank = bankRepository.findByName("默认题库").orElseGet(() ->
                bankRepository.save(TheoryBank.builder().name("默认题库").category("通用").author(author).build())
        );

        importQuestionsIntoBank(items, defaultBank, author, importedIds, errors);

        int successCount = importedIds.size();
        int failedCount = errors.size();

        return ChallengeImportResultVO.builder()
                .totalCount(successCount + failedCount)
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

        writeJsonResponse(response, "theory-challenges-export.json", array);
    }

    private List<TheoryChallengeImportDTO> parseToChallengeImportDTOList(String jsonContent) {
        if (!StringUtils.hasText(jsonContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未读取到有效的 JSON 题目数据内容");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (root == null || root.isNull() || root.isMissingNode()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题目数据内容为空");
            }

            if (root.isArray()) {
                if (root.isEmpty()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 题目数组为空");
                }
                return objectMapper.convertValue(root, new TypeReference<List<TheoryChallengeImportDTO>>() {});
            } else if (root.isObject()) {
                if (root.hasNonNull("questions") && root.get("questions").isArray()) {
                    return objectMapper.convertValue(root.get("questions"), new TypeReference<List<TheoryChallengeImportDTO>>() {});
                } else if (root.hasNonNull("items") && root.get("items").isArray()) {
                    return objectMapper.convertValue(root.get("items"), new TypeReference<List<TheoryChallengeImportDTO>>() {});
                } else if (root.hasNonNull("challenges") && root.get("challenges").isArray()) {
                    return objectMapper.convertValue(root.get("challenges"), new TypeReference<List<TheoryChallengeImportDTO>>() {});
                } else if (root.hasNonNull("content") || root.hasNonNull("name") || root.hasNonNull("question")) {
                    TheoryChallengeImportDTO single = objectMapper.convertValue(root, TheoryChallengeImportDTO.class);
                    return List.of(single);
                } else {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 数据结构不符合规范，未找到题目列表字段");
                }
            } else {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 数据必须为题目数组或包含题目列表的对象");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminTheoryChallengeService] 解析题目 JSON 语法错误", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析题目 JSON 语法错误: " + e.getMessage());
        }
    }

    private TheoryChallenge buildChallengeEntity(TheoryChallengeImportDTO dto, TheoryBank bank, User author) {
        String name = dto.getName().trim();
        String content = dto.getContent().trim();
        TheoryType type = dto.resolveTheoryType();
        String options = dto.resolveOptionsString(objectMapper);
        String standardAnswer = dto.resolveNormalizedAnswer();
        String explanation = StringUtils.hasText(dto.getExplanation()) ? dto.getExplanation().trim() : null;
        Integer score = dto.getScore() != null && dto.getScore() > 0 ? dto.getScore() : 5;
        Integer difficulty = dto.getDifficulty() != null && dto.getDifficulty() > 0 ? dto.getDifficulty() : 1;

        String category = StringUtils.hasText(dto.getCategory()) ? dto.getCategory().trim() : null;
        if (!StringUtils.hasText(category)) {
            category = bank != null && StringUtils.hasText(bank.getCategory()) ? bank.getCategory().trim() : "理论知识";
        }

        return TheoryChallenge.builder()
                .name(name)
                .category(category)
                .type(type)
                .content(content)
                .options(options)
                .standardAnswer(standardAnswer)
                .explanation(explanation)
                .score(score)
                .difficulty(difficulty)
                .bank(bank)
                .author(author)
                .build();
    }

    private String extractJsonContent(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                        return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                log.error("[AdminTheoryChallengeService] 解压 ZIP 失败", e);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "解析 ZIP 压缩包失败: " + e.getMessage());
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ZIP 包中未找到 .json 题目或题库文件");
        } else {
            try {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("[AdminTheoryChallengeService] 读取 JSON 失败", e);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "读取 JSON 文件失败: " + e.getMessage());
            }
        }
    }

    private void writeJsonResponse(HttpServletResponse response, String fileName, ArrayNode array) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(array);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("[AdminTheoryChallengeService] 导出 JSON 失败", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出 JSON 失败: " + e.getMessage());
        }
    }

}
