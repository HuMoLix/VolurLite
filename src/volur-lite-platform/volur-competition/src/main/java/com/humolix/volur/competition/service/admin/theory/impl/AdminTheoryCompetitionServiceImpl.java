package com.humolix.volur.competition.service.admin.theory.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.challenge.domain.entity.theory.TheoryBank;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.repository.theory.TheoryBankRepository;
import com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository;
import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionConfigDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionPaperGenerateRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionBank;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionConfig;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.vo.theory.TheoryBankSummaryVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCompetitionConfigVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperQuestionAdminVO;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionBankRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionConfigRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionPaperRepository;
import com.humolix.volur.competition.service.admin.theory.AdminTheoryCompetitionService;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTheoryCompetitionServiceImpl implements AdminTheoryCompetitionService {

    private final CompetitionRepository competitionRepository;
    private final TheoryCompetitionConfigRepository configRepository;
    private final TheoryCompetitionBankRepository compBankRepository;
    private final TheoryCompetitionPaperRepository paperRepository;
    private final TheoryCompetitionAnswerRepository answerRepository;
    private final TheoryBankRepository bankRepository;
    private final TheoryChallengeRepository challengeRepository;
    private final ObjectMapper objectMapper;

    private record PaperQuestionSlot(TheoryChallenge question, int score) {}

    @Override
    public TheoryCompetitionConfigVO getConfig(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (competition.getType() != CompetitionType.THEORY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛非理论答题赛制");
        }

        List<TheoryCompetitionBank> compBanks = compBankRepository.findByCompetitionId(competitionId);
        List<TheoryBankSummaryVO> bankSummaries = compBanks.stream().map(cb -> {
            TheoryBank b = cb.getBank();
            return TheoryBankSummaryVO.builder()
                    .id(b.getId())
                    .name(b.getName())
                    .questionCount(challengeRepository.countByBankId(b.getId()))
                    .build();
        }).toList();

        List<Long> bankIds = compBanks.stream().map(cb -> cb.getBank().getId()).toList();

        TheoryCompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);

        if (config == null) {
            return TheoryCompetitionConfigVO.builder()
                    .competitionId(competitionId)
                    .bankIds(bankIds)
                    .banks(bankSummaries)
                    .singleChoiceCount(0)
                    .singleChoiceScore(2)
                    .multipleChoiceCount(0)
                    .multipleChoiceScore(4)
                    .trueFalseCount(0)
                    .trueFalseScore(2)
                    .essayCount(0)
                    .essayScore(10)
                    .isRandom(true)
                    .shuffleOptions(true)
                    .totalQuestions(0)
                    .totalScore(0)
                    .build();
        }

        List<Long> savedBankIds = bankIds;
        if (config.getBankIds() != null && !config.getBankIds().isBlank()) {
            try {
                savedBankIds = objectMapper.readValue(config.getBankIds(), new TypeReference<List<Long>>() {});
            } catch (Exception ignored) {}
        }

        return TheoryCompetitionConfigVO.builder()
                .competitionId(competitionId)
                .bankIds(savedBankIds)
                .banks(bankSummaries)
                .singleChoiceCount(config.getSingleChoiceCount())
                .singleChoiceScore(config.getSingleChoiceScore())
                .multipleChoiceCount(config.getMultipleChoiceCount())
                .multipleChoiceScore(config.getMultipleChoiceScore())
                .trueFalseCount(config.getTrueFalseCount())
                .trueFalseScore(config.getTrueFalseScore())
                .essayCount(config.getEssayCount())
                .essayScore(config.getEssayScore())
                .isRandom(config.getIsRandom())
                .shuffleOptions(config.getShuffleOptions() != null ? config.getShuffleOptions() : true)
                .totalQuestions(config.getTotalQuestions())
                .totalScore(config.getTotalScore())
                .build();
    }

    @Override
    @Transactional
    public TheoryCompetitionConfigVO saveConfig(Long competitionId, TheoryCompetitionConfigDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (competition.getType() != CompetitionType.THEORY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛非理论答题赛制");
        }

        List<Long> bankIds = request.getBankIds();
        if (bankIds == null || bankIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择至少一个引用的理论题库");
        }

        List<TheoryBank> banks = bankRepository.findAllById(bankIds);
        if (banks.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "所选引用的题库不存在");
        }

        compBankRepository.deleteByCompetitionId(competitionId);
        List<TheoryCompetitionBank> compBanks = banks.stream()
                .map(b -> TheoryCompetitionBank.builder().competition(competition).bank(b).build())
                .toList();
        compBankRepository.saveAll(compBanks);

        TheoryCompetitionConfig config = configRepository.findByCompetitionId(competitionId)
                .orElse(TheoryCompetitionConfig.builder().competition(competition).build());

        try {
            config.setBankIds(objectMapper.writeValueAsString(bankIds));
        } catch (Exception e) {
            config.setBankIds(bankIds.toString());
        }

        int scCount = request.getSingleChoiceCount() != null ? Math.max(0, request.getSingleChoiceCount()) : 0;
        int scScore = request.getSingleChoiceScore() != null ? Math.max(0, request.getSingleChoiceScore()) : 2;
        int mcCount = request.getMultipleChoiceCount() != null ? Math.max(0, request.getMultipleChoiceCount()) : 0;
        int mcScore = request.getMultipleChoiceScore() != null ? Math.max(0, request.getMultipleChoiceScore()) : 4;
        int tfCount = request.getTrueFalseCount() != null ? Math.max(0, request.getTrueFalseCount()) : 0;
        int tfScore = request.getTrueFalseScore() != null ? Math.max(0, request.getTrueFalseScore()) : 2;
        int esCount = request.getEssayCount() != null ? Math.max(0, request.getEssayCount()) : 0;
        int esScore = request.getEssayScore() != null ? Math.max(0, request.getEssayScore()) : 10;

        int totalQuestions = scCount + mcCount + tfCount + esCount;
        int totalScore = scCount * scScore + mcCount * mcScore + tfCount * tfScore + esCount * esScore;

        config.setSingleChoiceCount(scCount);
        config.setSingleChoiceScore(scScore);
        config.setMultipleChoiceCount(mcCount);
        config.setMultipleChoiceScore(mcScore);
        config.setTrueFalseCount(tfCount);
        config.setTrueFalseScore(tfScore);
        config.setEssayCount(esCount);
        config.setEssayScore(esScore);
        config.setIsRandom(request.getIsRandom() == null || request.getIsRandom());
        config.setShuffleOptions(request.getShuffleOptions() == null || request.getShuffleOptions());
        config.setTotalQuestions(totalQuestions);
        config.setTotalScore(totalScore);

        configRepository.save(config);

        return getConfig(competitionId);
    }

    @Override
    @Transactional
    public int generatePaper(Long competitionId, TheoryCompetitionPaperGenerateRequestDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (competition.getType() != CompetitionType.THEORY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛非理论答题赛制");
        }

        if (competition.getStatus() == CompetitionStatus.RUNNING && answerRepository.existsByCompetitionId(competitionId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛正在进行且已有选手提交答卷，严禁重新生成试卷");
        }

        List<Long> bankIds = request.getBankIds();
        if (bankIds == null || bankIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择至少一个引用的题库");
        }

        List<TheoryBank> banks = bankRepository.findAllById(bankIds);
        if (banks.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "所选引用的题库不存在");
        }

        List<TheoryCompetitionPaper> papersToSave = new ArrayList<>();
        boolean isRandom = request.getIsRandom() == null || request.getIsRandom();

        if (request.getSelectedQuestionIds() != null && !request.getSelectedQuestionIds().isEmpty()) {
            List<TheoryChallenge> selected = challengeRepository.findAllById(request.getSelectedQuestionIds());
            int order = 1;
            for (TheoryChallenge tc : selected) {
                papersToSave.add(TheoryCompetitionPaper.builder()
                        .competition(competition)
                        .challenge(tc)
                        .score(tc.getScore() != null ? tc.getScore() : 5)
                        .sortOrder(order++)
                        .build());
            }
        } else {
            List<TheoryChallenge> allCandidates = challengeRepository.findByBankIdIn(bankIds).stream().distinct().toList();
            Map<TheoryType, List<TheoryChallenge>> grouped = allCandidates.stream()
                    .collect(Collectors.groupingBy(TheoryChallenge::getType));

            List<PaperQuestionSlot> selectedQuestions = new ArrayList<>();

            int scCount = request.getSingleChoiceCount() != null ? Math.max(0, request.getSingleChoiceCount()) : 0;
            int scScore = request.getSingleChoiceScore() != null ? request.getSingleChoiceScore() : 2;
            extractQuestions(grouped.getOrDefault(TheoryType.SINGLE_CHOICE, List.of()), scCount, scScore, isRandom, "单项选择题", selectedQuestions);

            int mcCount = request.getMultipleChoiceCount() != null ? Math.max(0, request.getMultipleChoiceCount()) : 0;
            int mcScore = request.getMultipleChoiceScore() != null ? request.getMultipleChoiceScore() : 4;
            extractQuestions(grouped.getOrDefault(TheoryType.MULTIPLE_CHOICE, List.of()), mcCount, mcScore, isRandom, "多项选择题", selectedQuestions);

            int tfCount = request.getTrueFalseCount() != null ? Math.max(0, request.getTrueFalseCount()) : 0;
            int tfScore = request.getTrueFalseScore() != null ? request.getTrueFalseScore() : 2;
            extractQuestions(grouped.getOrDefault(TheoryType.TRUE_FALSE, List.of()), tfCount, tfScore, isRandom, "判断题", selectedQuestions);

            int esCount = request.getEssayCount() != null ? Math.max(0, request.getEssayCount()) : 0;
            int esScore = request.getEssayScore() != null ? request.getEssayScore() : 10;
            extractQuestions(grouped.getOrDefault(TheoryType.ESSAY, List.of()), esCount, esScore, isRandom, "主观问答题", selectedQuestions);

            if (selectedQuestions.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "抽取的试卷题目总数为0，请至少配置一种题型的抽取数量");
            }

            int order = 1;
            for (PaperQuestionSlot slot : selectedQuestions) {
                papersToSave.add(TheoryCompetitionPaper.builder()
                        .competition(competition)
                        .challenge(slot.question())
                        .score(slot.score())
                        .sortOrder(order++)
                        .build());
            }
        }

        paperRepository.deleteByCompetitionId(competitionId);
        paperRepository.saveAll(papersToSave);

        compBankRepository.deleteByCompetitionId(competitionId);
        List<TheoryCompetitionBank> compBanks = banks.stream()
                .map(b -> TheoryCompetitionBank.builder().competition(competition).bank(b).build())
                .toList();
        compBankRepository.saveAll(compBanks);

        int totalScore = papersToSave.stream().mapToInt(TheoryCompetitionPaper::getScore).sum();
        TheoryCompetitionConfig config = configRepository.findByCompetitionId(competitionId)
                .orElse(TheoryCompetitionConfig.builder().competition(competition).build());

        try {
            config.setBankIds(objectMapper.writeValueAsString(bankIds));
        } catch (Exception e) {
            config.setBankIds(bankIds.toString());
        }

        config.setSingleChoiceCount(request.getSingleChoiceCount() != null ? request.getSingleChoiceCount() : 0);
        config.setSingleChoiceScore(request.getSingleChoiceScore() != null ? request.getSingleChoiceScore() : 2);
        config.setMultipleChoiceCount(request.getMultipleChoiceCount() != null ? request.getMultipleChoiceCount() : 0);
        config.setMultipleChoiceScore(request.getMultipleChoiceScore() != null ? request.getMultipleChoiceScore() : 4);
        config.setTrueFalseCount(request.getTrueFalseCount() != null ? request.getTrueFalseCount() : 0);
        config.setTrueFalseScore(request.getTrueFalseScore() != null ? request.getTrueFalseScore() : 2);
        config.setEssayCount(request.getEssayCount() != null ? request.getEssayCount() : 0);
        config.setEssayScore(request.getEssayScore() != null ? request.getEssayScore() : 10);
        config.setIsRandom(isRandom);
        config.setTotalQuestions(papersToSave.size());
        config.setTotalScore(totalScore);

        configRepository.save(config);

        return papersToSave.size();
    }

    @Override
    public List<TheoryPaperQuestionAdminVO> listPaperQuestions(Long competitionId) {
        List<TheoryCompetitionPaper> papers = paperRepository.findByCompetitionIdWithChallenge(competitionId);
        return papers.stream().map(p -> {
            TheoryChallenge c = p.getChallenge();
            return TheoryPaperQuestionAdminVO.builder()
                    .paperId(p.getId())
                    .challengeId(c != null ? c.getId() : null)
                    .name(c != null ? c.getName() : "-")
                    .bankName(c != null && c.getBank() != null ? c.getBank().getName() : "-")
                    .category(c != null ? c.getCategory() : "-")
                    .type(c != null ? c.getType() : null)
                    .score(p.getScore())
                    .sortOrder(p.getSortOrder())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void removePaperQuestion(Long competitionId, Long paperId) {
        TheoryCompetitionPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "试卷题目不存在"));

        if (!paper.getCompetition().getId().equals(competitionId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "试卷题目不属于该竞赛");
        }

        paperRepository.delete(paper);

        configRepository.findByCompetitionId(competitionId).ifPresent(cfg -> {
            List<TheoryCompetitionPaper> remaining = paperRepository.findByCompetitionIdOrderBySortOrderAsc(competitionId);
            cfg.setTotalQuestions(remaining.size());
            cfg.setTotalScore(remaining.stream().mapToInt(TheoryCompetitionPaper::getScore).sum());
            configRepository.save(cfg);
        });
    }

    private void extractQuestions(List<TheoryChallenge> source, int count, int score, boolean isRandom, String typeName, List<PaperQuestionSlot> target) {
        if (count <= 0) return;
        if (source.size() < count) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "所选题库中 " + typeName + " 数量不足，当前仅有 " + source.size() + " 道，需要 " + count + " 道");
        }
        List<TheoryChallenge> copy = new ArrayList<>(source);
        if (isRandom) {
            Collections.shuffle(copy);
        }
        for (int i = 0; i < count; i++) {
            target.add(new PaperQuestionSlot(copy.get(i), score));
        }
    }

}
