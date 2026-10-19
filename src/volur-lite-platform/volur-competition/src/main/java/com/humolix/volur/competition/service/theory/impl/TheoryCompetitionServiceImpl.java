package com.humolix.volur.competition.service.theory.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository;
import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.dto.theory.TheoryAnswerItemDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryCheatReportDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryProctorHeartbeatDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionOperationLog;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionAnswer;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionBank;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionConfig;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.entity.theory.TheoryUserPaper;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.theory.AdminTheoryCandidateMonitorVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCheatReportVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperQuestionVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryProctorHeartbeatVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionOperationLogRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.domain.dto.theory.TheoryProctorSnapshotDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryProctorRtcSignalDTO;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionBankRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionConfigRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionPaperRepository;
import com.humolix.volur.competition.repository.theory.TheoryUserPaperRepository;
import com.humolix.volur.competition.service.theory.TheoryCompetitionService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.notification.service.RealtimePushService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStatus(allowed = {CompetitionStatus.RUNNING}, types = {CompetitionType.THEORY})
@Transactional(readOnly = true)
public class TheoryCompetitionServiceImpl implements TheoryCompetitionService {

    private static final String VREDIS_COMPETITION_THEORY_CHEAT_KEY_PREFIX = "vredis:competition:theory:cheat:";
    private static final Duration VREDIS_COMPETITION_THEORY_CHEAT_TTL = Duration.ofHours(24);
    private static final int DEFAULT_MAX_ALLOWED_VIOLATIONS = 3;

    private static final String VREDIS_COMPETITION_THEORY_TICKET_KEY_PREFIX = "vredis:competition:theory:ticket:";
    private static final Duration VREDIS_COMPETITION_THEORY_TICKET_TTL = Duration.ofHours(6);

    private static final String VREDIS_COMPETITION_THEORY_MONITOR_USERS_KEY_PREFIX = "vredis:competition:theory:monitor:users:";
    private static final String VREDIS_COMPETITION_THEORY_MONITOR_USER_KEY_PREFIX = "vredis:competition:theory:monitor:user:";
    private static final Duration VREDIS_COMPETITION_THEORY_MONITOR_TTL = Duration.ofHours(24);

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final TheoryCompetitionConfigRepository configRepository;
    private final TheoryCompetitionBankRepository compBankRepository;
    private final TheoryChallengeRepository challengeRepository;
    private final TheoryUserPaperRepository userPaperRepository;
    private final TheoryCompetitionPaperRepository paperRepository;
    private final TheoryCompetitionAnswerRepository answerRepository;
    private final CompetitionOperationLogRepository operationLogRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RealtimePushService realtimePushService;

    private record PaperQuestionSlot(TheoryChallenge question, int score) {}
    private record OptionEntry(String key, String text) {}
    private record ShuffledOptionResult(String shuffledOptionsJson, String shuffledStandardAnswer) {}

    @Override
    @Transactional
    public TheoryPaperExamVO getPaper(Long competitionId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
        validateProctorTicket(competitionId, userId);

        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);

        TheoryUserPaper userPaper = userPaperRepository.findByCompetitionIdAndUserId(competitionId, userId)
                .orElseGet(() -> generateUserPaper(competitionId, userId));

        if (!isAdmin && Boolean.TRUE.equals(userPaper.getIsSubmitted())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队答题已结束，无法查看试卷题目");
        }

        List<TheoryCompetitionPaper> papers = paperRepository.findByUserPaperIdWithChallenge(userPaper.getId());
        if (papers.isEmpty()) {
            papers = paperRepository.findByCompetitionIdAndUserIdOrderBySortOrderAsc(competitionId, userId);
        }

        if (!Boolean.TRUE.equals(userPaper.getIsSubmitted()) && !answerRepository.existsByCompetitionIdAndUserId(competitionId, userId)) {
            if (!isPaperTypeOrdered(papers)) {
                paperRepository.deleteByUserPaperId(userPaper.getId());
                userPaperRepository.delete(userPaper);
                userPaper = generateUserPaper(competitionId, userId);
                papers = paperRepository.findByUserPaperIdWithChallenge(userPaper.getId());
            }
        }

        List<TheoryPaperQuestionVO> questionVOs = competitionMapper.toTheoryPaperQuestionVOList(papers);

        int totalScore = userPaper.getTotalScore() != null ? userPaper.getTotalScore() : papers.stream().mapToInt(TheoryCompetitionPaper::getScore).sum();
        boolean isSubmitted = Boolean.TRUE.equals(userPaper.getIsSubmitted());
        int myTotalScore = userPaper.getMyTotalScore() != null ? userPaper.getMyTotalScore() : 0;

        if (userId != null) {
            List<TheoryCompetitionAnswer> myAnswers = answerRepository.findByCompetitionIdAndUserId(competitionId, userId);
            if (!myAnswers.isEmpty()) {
                isSubmitted = true;
                myTotalScore = myAnswers.stream().mapToInt(TheoryCompetitionAnswer::getScoreObtained).sum();
                Map<Long, String> answerMap = myAnswers.stream()
                        .collect(Collectors.toMap(a -> a.getPaper().getId(), a -> a.getUserAnswer() != null ? a.getUserAnswer() : "", (k1, k2) -> k2));

                for (TheoryPaperQuestionVO q : questionVOs) {
                    q.setMyAnswer(answerMap.get(q.getPaperId()));
                }
            }
        }

        return TheoryPaperExamVO.builder()
                .paperId(userPaper.getId())
                .competitionId(competitionId)
                .totalQuestions(questionVOs.size())
                .totalScore(totalScore)
                .isSubmitted(isSubmitted)
                .myTotalScore(isAdmin ? myTotalScore : 0)
                .questions(questionVOs)
                .build();
    }

    @Override
    @Transactional
    public TheoryPaperResultVO submitPaper(Long competitionId, Long userId, TheoryPaperSubmitRequestDTO request) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean isAdmin = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        if (team == null && !isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        if (team != null && team.getStatus() == TeamStatus.ENDED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您的战队答题已结束，严禁重复提交");
        }

        TheoryUserPaper userPaper = userPaperRepository.findByCompetitionIdAndUserId(competitionId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "未找到您的考试试卷记录，请刷新后重试"));

        if (Boolean.TRUE.equals(userPaper.getIsSubmitted())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "您已提交过本次理论考试试卷，严禁重复提交");
        }

        List<TheoryCompetitionPaper> papers = paperRepository.findByUserPaperIdWithChallenge(userPaper.getId());
        if (papers.isEmpty()) {
            papers = paperRepository.findByCompetitionIdAndUserIdOrderBySortOrderAsc(competitionId, userId);
        }
        if (papers.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "本场理论试卷未包含任何试题");
        }

        Competition competition = userPaper.getCompetition();

        Map<Long, String> inputAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(TheoryAnswerItemDTO::getPaperId, a -> a.getAnswer() != null ? a.getAnswer().trim() : "", (k1, k2) -> k2));

        int totalScore = 0;
        int scoreObtained = 0;
        int correctCount = 0;
        List<TheoryCompetitionAnswer> answersToSave = new ArrayList<>();

        for (TheoryCompetitionPaper paper : papers) {
            int paperScore = paper.getScore() != null ? paper.getScore() : 5;
            totalScore += paperScore;

            String submittedAnswer = inputAnswers.getOrDefault(paper.getId(), "");
            TheoryChallenge rawChallenge = paper.getChallenge();
            String stdAnswer = paper.getShuffledStandardAnswer() != null && !paper.getShuffledStandardAnswer().isBlank()
                    ? paper.getShuffledStandardAnswer().trim()
                    : (rawChallenge != null && rawChallenge.getStandardAnswer() != null ? rawChallenge.getStandardAnswer().trim() : "");

            boolean isCorrect = evaluateAnswer(rawChallenge, submittedAnswer, stdAnswer);
            int currentScore = isCorrect ? paperScore : 0;
            if (isCorrect) {
                correctCount++;
                scoreObtained += currentScore;
            }

            TheoryCompetitionAnswer answerRecord = answerRepository.findByPaperIdAndUserId(paper.getId(), userId)
                    .orElse(TheoryCompetitionAnswer.builder()
                            .competition(competition)
                            .paper(paper)
                            .team(team)
                            .user(user)
                            .build());

            answerRecord.setUserAnswer(submittedAnswer);
            answerRecord.setIsCorrect(isCorrect);
            answerRecord.setScoreObtained(currentScore);
            answersToSave.add(answerRecord);
        }

        answerRepository.saveAll(answersToSave);

        userPaper.setIsSubmitted(true);
        userPaper.setMyTotalScore(scoreObtained);
        userPaperRepository.save(userPaper);

        if (team != null) {
            team.setStatus(TeamStatus.ENDED);
            teamRepository.save(team);
        }

        double accuracyRate = papers.isEmpty() ? 0.0 : (double) correctCount / papers.size() * 100.0;

        return TheoryPaperResultVO.builder()
                .totalQuestions(papers.size())
                .correctCount(isAdmin ? correctCount : null)
                .totalScore(isAdmin ? totalScore : null)
                .scoreObtained(isAdmin ? scoreObtained : null)
                .accuracyRate(isAdmin ? (Math.round(accuracyRate * 100.0) / 100.0) : null)
                .build();
    }

    @Override
    @Transactional
    public TheoryCheatReportVO reportCheatEvent(Long competitionId, Long userId, TheoryCheatReportDTO request) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (Boolean.FALSE.equals(user.getStatus())) {
            return TheoryCheatReportVO.builder()
                    .violationCount(DEFAULT_MAX_ALLOWED_VIOLATIONS)
                    .maxAllowedViolations(DEFAULT_MAX_ALLOWED_VIOLATIONS)
                    .isBanned(true)
                    .message("账号已被封禁")
                    .build();
        }

        String redisKey = VREDIS_COMPETITION_THEORY_CHEAT_KEY_PREFIX + competitionId + ":" + userId;
        Long currentViolations = stringRedisTemplate.opsForValue().increment(redisKey);
        stringRedisTemplate.expire(redisKey, VREDIS_COMPETITION_THEORY_CHEAT_TTL);

        int count = currentViolations != null ? currentViolations.intValue() : 1;
        boolean isBanned = count >= DEFAULT_MAX_ALLOWED_VIOLATIONS;

        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        String teamName = team != null ? team.getTeamName() : null;
        Long teamId = team != null ? team.getId() : null;

        CompetitionOperationLog log = CompetitionOperationLog.builder()
                .competitionId(competitionId)
                .userId(userId)
                .username(user.getUsername())
                .teamId(teamId)
                .teamName(teamName)
                .action(isBanned ? "理论考试违规超限封禁" : "理论考试违规告警: " + request.getEventType())
                .logType("THEORY_CHEAT")
                .params(request.getDetail())
                .status(!isBanned)
                .build();
        operationLogRepository.save(log);

        if (isBanned) {
            user.setStatus(false);
            userRepository.save(user);

            return TheoryCheatReportVO.builder()
                    .violationCount(count)
                    .maxAllowedViolations(DEFAULT_MAX_ALLOWED_VIOLATIONS)
                    .isBanned(true)
                    .message("违规行为已达 " + count + " 次，超过上限 " + DEFAULT_MAX_ALLOWED_VIOLATIONS + " 次，账号已被系统安全封禁！")
                    .build();
        }

        return TheoryCheatReportVO.builder()
                .violationCount(count)
                .maxAllowedViolations(DEFAULT_MAX_ALLOWED_VIOLATIONS)
                .isBanned(false)
                .message("警告：检测到违规危险行为（" + request.getEventType() + "），当前违规 " + count + " 次，累计 " + DEFAULT_MAX_ALLOWED_VIOLATIONS + " 次将封禁！")
                .build();
    }

    private synchronized TheoryUserPaper generateUserPaper(Long competitionId, Long userId) {
        Optional<TheoryUserPaper> existing = userPaperRepository.findByCompetitionIdAndUserId(competitionId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);

        TheoryCompetitionConfig config = configRepository.findByCompetitionId(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "管理员尚未配置本场理论竞赛的赛题规则"));

        List<Long> bankIds = new ArrayList<>();
        if (config.getBankIds() != null && !config.getBankIds().isBlank()) {
            try {
                bankIds = objectMapper.readValue(config.getBankIds(), new TypeReference<List<Long>>() {});
            } catch (Exception ignored) {}
        }
        if (bankIds.isEmpty()) {
            List<TheoryCompetitionBank> compBanks = compBankRepository.findByCompetitionId(competitionId);
            bankIds = compBanks.stream().map(cb -> cb.getBank().getId()).toList();
        }
        if (bankIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "管理员尚未为本场理论考核配置引用题库");
        }

        List<TheoryChallenge> allCandidates = challengeRepository.findByBankIdIn(bankIds).stream().distinct().toList();
        if (allCandidates.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "引用的理论题库中暂无可用的考核试题");
        }

        Map<TheoryType, List<TheoryChallenge>> grouped = allCandidates.stream()
                .collect(Collectors.groupingBy(TheoryChallenge::getType));

        boolean isRandom = Boolean.TRUE.equals(config.getIsRandom());
        boolean shuffleOptions = Boolean.TRUE.equals(config.getShuffleOptions());

        List<PaperQuestionSlot> selectedQuestions = new ArrayList<>();

        extractQuestions(grouped.getOrDefault(TheoryType.SINGLE_CHOICE, List.of()), config.getSingleChoiceCount(), config.getSingleChoiceScore(), isRandom, "单项选择题", selectedQuestions);
        extractQuestions(grouped.getOrDefault(TheoryType.MULTIPLE_CHOICE, List.of()), config.getMultipleChoiceCount(), config.getMultipleChoiceScore(), isRandom, "多项选择题", selectedQuestions);
        extractQuestions(grouped.getOrDefault(TheoryType.TRUE_FALSE, List.of()), config.getTrueFalseCount(), config.getTrueFalseScore(), isRandom, "判断题", selectedQuestions);
        extractQuestions(grouped.getOrDefault(TheoryType.ESSAY, List.of()), config.getEssayCount(), config.getEssayScore(), isRandom, "主观问答题", selectedQuestions);

        if (selectedQuestions.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "抽取的试卷题目总数为0，请联系管理员配置各题型抽题数量");
        }

        int totalScore = selectedQuestions.stream().mapToInt(PaperQuestionSlot::score).sum();

        TheoryUserPaper userPaper = TheoryUserPaper.builder()
                .competition(competition)
                .user(user)
                .team(team)
                .totalQuestions(selectedQuestions.size())
                .totalScore(totalScore)
                .isSubmitted(false)
                .myTotalScore(0)
                .build();
        userPaper = userPaperRepository.save(userPaper);

        List<TheoryCompetitionPaper> papersToSave = new ArrayList<>();
        int order = 1;
        for (PaperQuestionSlot slot : selectedQuestions) {
            TheoryChallenge tc = slot.question();
            String shuffledOptions = null;
            String shuffledStandardAnswer = null;

            if (shuffleOptions && (tc.getType() == TheoryType.SINGLE_CHOICE || tc.getType() == TheoryType.MULTIPLE_CHOICE)) {
                ShuffledOptionResult shuffled = shuffleChallengeOptions(tc);
                if (shuffled != null) {
                    shuffledOptions = shuffled.shuffledOptionsJson();
                    shuffledStandardAnswer = shuffled.shuffledStandardAnswer();
                }
            }

            papersToSave.add(TheoryCompetitionPaper.builder()
                    .competition(competition)
                    .userPaper(userPaper)
                    .user(user)
                    .challenge(tc)
                    .score(slot.score())
                    .sortOrder(order++)
                    .shuffledOptions(shuffledOptions)
                    .shuffledStandardAnswer(shuffledStandardAnswer)
                    .build());
        }

        paperRepository.saveAll(papersToSave);

        return userPaper;
    }

    private void extractQuestions(List<TheoryChallenge> source, Integer count, Integer score, boolean isRandom, String typeName, List<PaperQuestionSlot> target) {
        if (count == null || count <= 0) return;
        int itemScore = score != null ? score : 2;
        if (source.size() < count) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "所选题库中 " + typeName + " 数量不足，当前题库仅有 " + source.size() + " 道，需要抽取 " + count + " 道");
        }
        List<TheoryChallenge> copy = new ArrayList<>(source);
        if (isRandom) {
            Collections.shuffle(copy);
        }
        for (int i = 0; i < count; i++) {
            target.add(new PaperQuestionSlot(copy.get(i), itemScore));
        }
    }

    private ShuffledOptionResult shuffleChallengeOptions(TheoryChallenge challenge) {
        if (challenge == null || challenge.getOptions() == null || challenge.getOptions().isBlank()) {
            return null;
        }
        String raw = challenge.getOptions().trim();
        List<OptionEntry> entries = parseOptionEntries(raw);
        if (entries.size() <= 1) {
            return null;
        }

        List<OptionEntry> shuffled = new ArrayList<>(entries);
        Collections.shuffle(shuffled);

        Map<String, String> oldToNew = new HashMap<>();
        List<String> newOptionStrings = new ArrayList<>();

        for (int i = 0; i < shuffled.size(); i++) {
            String newKey = String.valueOf((char) ('A' + i));
            OptionEntry entry = shuffled.get(i);
            oldToNew.put(entry.key.toUpperCase(), newKey);
            newOptionStrings.add(newKey + ". " + entry.text);
        }

        String shuffledOptionsJson;
        try {
            shuffledOptionsJson = objectMapper.writeValueAsString(newOptionStrings);
        } catch (Exception e) {
            shuffledOptionsJson = String.join("\n", newOptionStrings);
        }

        String originalStd = challenge.getStandardAnswer() != null ? challenge.getStandardAnswer().trim() : "";
        String newStd = mapStandardAnswer(challenge.getType(), originalStd, oldToNew);

        return new ShuffledOptionResult(shuffledOptionsJson, newStd);
    }

    private List<OptionEntry> parseOptionEntries(String raw) {
        List<OptionEntry> result = new ArrayList<>();
        if (raw.startsWith("[")) {
            try {
                List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
                for (int i = 0; i < list.size(); i++) {
                    String s = list.get(i);
                    if (s == null) continue;
                    String trimmed = s.trim();
                    String key = String.valueOf((char) ('A' + i));
                    String text = trimmed;
                    if (trimmed.length() >= 2 && Character.isLetter(trimmed.charAt(0)) && (trimmed.charAt(1) == '.' || trimmed.charAt(1) == '、' || trimmed.charAt(1) == ':' || trimmed.charAt(1) == ' ')) {
                        key = String.valueOf(Character.toUpperCase(trimmed.charAt(0)));
                        text = trimmed.substring(2).trim();
                    }
                    result.add(new OptionEntry(key, text));
                }
                return result;
            } catch (Exception ignored) {}
        } else if (raw.startsWith("{")) {
            try {
                Map<String, String> map = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, String>>() {});
                for (Map.Entry<String, String> e : map.entrySet()) {
                    result.add(new OptionEntry(e.getKey().trim().toUpperCase(), e.getValue() != null ? e.getValue().trim() : ""));
                }
                return result;
            } catch (Exception ignored) {}
        }

        String[] lines = raw.split("\\r?\\n");
        int idx = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String key = String.valueOf((char) ('A' + idx));
            String text = trimmed;
            if (trimmed.length() >= 2 && Character.isLetter(trimmed.charAt(0)) && (trimmed.charAt(1) == '.' || trimmed.charAt(1) == '、' || trimmed.charAt(1) == ':' || trimmed.charAt(1) == ' ')) {
                key = String.valueOf(Character.toUpperCase(trimmed.charAt(0)));
                text = trimmed.substring(2).trim();
            }
            result.add(new OptionEntry(key, text));
            idx++;
        }
        return result;
    }

    private String mapStandardAnswer(TheoryType type, String originalStd, Map<String, String> oldToNew) {
        if (!StringUtils.hasText(originalStd)) {
            return originalStd;
        }
        List<Character> originalLetters = new ArrayList<>();
        for (char c : originalStd.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                originalLetters.add(c);
            }
        }
        if (originalLetters.isEmpty()) {
            return originalStd;
        }

        List<String> newLetters = new ArrayList<>();
        for (char c : originalLetters) {
            String oldKey = String.valueOf(c);
            String mapped = oldToNew.getOrDefault(oldKey, oldKey);
            if (!newLetters.contains(mapped)) {
                newLetters.add(mapped);
            }
        }
        Collections.sort(newLetters);

        if (type == TheoryType.MULTIPLE_CHOICE) {
            return String.join(",", newLetters);
        } else {
            return newLetters.isEmpty() ? originalStd : newLetters.getFirst();
        }
    }

    private CompetitionTeam getMyApprovedTeam(Long competitionId, Long userId) {
        if (userId == null) return null;
        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionTeam team = memberOpt.get().getTeam();
            if (team != null && team.getStatus() == TeamStatus.BANNED) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
            }
            if (team != null && (team.getStatus() == TeamStatus.AUDIT_PASSED || team.getStatus() == TeamStatus.ENDED)) {
                return team;
            }
        }
        return null;
    }

    private void validateUserParticipationOrAdmin(Long competitionId, Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        boolean isAdmin = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);

        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionTeam team = memberOpt.get().getTeam();
            if (team != null && team.getStatus() == TeamStatus.BANNED) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队已被封禁，无法参加此场比赛");
            }
            if (team != null && team.getStatus() == TeamStatus.ENDED && !isAdmin) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "您的战队答题已结束，无法查看试卷题目");
            }
            if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                return;
            }
            if (isAdmin) {
                return;
            }
        }
        if (!isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }
    }

    private boolean evaluateAnswer(TheoryChallenge challenge, String submitted, String standard) {
        if (!StringUtils.hasText(submitted) || !StringUtils.hasText(standard)) {
            return false;
        }
        if (challenge != null && challenge.getType() == TheoryType.MULTIPLE_CHOICE) {
            String normSub = extractSortedLetters(submitted);
            String normStd = extractSortedLetters(standard);
            return normSub.equalsIgnoreCase(normStd);
        }
        if (challenge != null && challenge.getType() == TheoryType.TRUE_FALSE) {
            Boolean subBool = parseBooleanAnswer(submitted);
            Boolean stdBool = parseBooleanAnswer(standard);
            if (subBool != null && stdBool != null) {
                return subBool.equals(stdBool);
            }
        }
        return submitted.trim().equalsIgnoreCase(standard.trim());
    }

    private String extractSortedLetters(String ans) {
        List<Character> list = new ArrayList<>();
        for (char c : ans.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                if (!list.contains(c)) {
                    list.add(c);
                }
            }
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (char c : list) {
            sb.append(c);
        }
        return sb.toString();
    }

    private Boolean parseBooleanAnswer(String ans) {
        String u = ans.trim().toUpperCase();
        if (u.equals("TRUE") || u.equals("正确") || u.equals("对") || u.equals("T") || u.equals("A") || u.equals("YES")) {
            return Boolean.TRUE;
        }
        if (u.equals("FALSE") || u.equals("错误") || u.equals("错") || u.equals("F") || u.equals("B") || u.equals("NO")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean isPaperTypeOrdered(List<TheoryCompetitionPaper> papers) {
        if (papers == null || papers.size() <= 1) {
            return true;
        }
        Set<TheoryType> seenTypes = new HashSet<>();
        TheoryType currentType = null;
        for (TheoryCompetitionPaper p : papers) {
            TheoryType type = p.getChallenge() != null ? p.getChallenge().getType() : null;
            if (type != null && type != currentType) {
                if (seenTypes.contains(type)) {
                    return false;
                }
                seenTypes.add(type);
                currentType = type;
            }
        }
        return true;
    }

    private void validateProctorTicket(Long competitionId, Long userId) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN)) {
            return;
        }
        String ticketKey = VREDIS_COMPETITION_THEORY_TICKET_KEY_PREFIX + competitionId + ":" + userId;
        Boolean hasTicket = stringRedisTemplate.hasKey(ticketKey);
        if (!Boolean.TRUE.equals(hasTicket)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "尚未通过考场全屏与屏幕共享环境检查，请先前往监考准入页面完成检查");
        }
    }

    @Override
    public void registerProctorReady(Long competitionId, Long userId) {
        validateUserParticipationOrAdmin(competitionId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        String ticketKey = VREDIS_COMPETITION_THEORY_TICKET_KEY_PREFIX + competitionId + ":" + userId;
        stringRedisTemplate.opsForValue().set(ticketKey, "ACTIVE", VREDIS_COMPETITION_THEORY_TICKET_TTL);

        String usersKey = VREDIS_COMPETITION_THEORY_MONITOR_USERS_KEY_PREFIX + competitionId;
        stringRedisTemplate.opsForSet().add(usersKey, userId.toString());
        stringRedisTemplate.expire(usersKey, VREDIS_COMPETITION_THEORY_MONITOR_TTL);

        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        String teamName = team != null ? team.getTeamName() : null;
        Long teamId = team != null ? team.getId() : null;

        String userKey = VREDIS_COMPETITION_THEORY_MONITOR_USER_KEY_PREFIX + competitionId + ":" + userId;
        AdminTheoryCandidateMonitorVO initMonitor = AdminTheoryCandidateMonitorVO.builder()
                .userId(userId)
                .username(user.getUsername())
                .teamId(teamId)
                .teamName(teamName)
                .isOnline(true)
                .fullscreen(true)
                .screenActive(true)
                .violationCount(getViolationCount(competitionId, userId))
                .isBanned(Boolean.FALSE.equals(user.getStatus()))
                .answeredCount(0)
                .lastHeartbeatTime(System.currentTimeMillis())
                .connectedSince(System.currentTimeMillis())
                .build();

        try {
            stringRedisTemplate.opsForValue().set(userKey, objectMapper.writeValueAsString(initMonitor), VREDIS_COMPETITION_THEORY_MONITOR_TTL);
        } catch (Exception ignored) {}
    }

    @Override
    public TheoryProctorHeartbeatVO recordProctorHeartbeat(Long competitionId, Long userId, TheoryProctorHeartbeatDTO request) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        int violationCount = getViolationCount(competitionId, userId);
        boolean isBanned = Boolean.FALSE.equals(user.getStatus()) || violationCount >= DEFAULT_MAX_ALLOWED_VIOLATIONS;

        String userKey = VREDIS_COMPETITION_THEORY_MONITOR_USER_KEY_PREFIX + competitionId + ":" + userId;
        String raw = stringRedisTemplate.opsForValue().get(userKey);
        AdminTheoryCandidateMonitorVO monitor = null;
        if (StringUtils.hasText(raw)) {
            try {
                monitor = objectMapper.readValue(raw, AdminTheoryCandidateMonitorVO.class);
            } catch (Exception ignored) {}
        }

        long now = System.currentTimeMillis();
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);

        if (monitor == null) {
            monitor = AdminTheoryCandidateMonitorVO.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .teamId(team != null ? team.getId() : null)
                    .teamName(team != null ? team.getTeamName() : null)
                    .connectedSince(now)
                    .build();
        }

        if (monitor.getLastHeartbeatTime() != null && monitor.getLastHeartbeatTime() > 0) {
            long diff = now - monitor.getLastHeartbeatTime();
            if (diff > 25_000) {
                CompetitionOperationLog log = CompetitionOperationLog.builder()
                        .competitionId(competitionId)
                        .userId(userId)
                        .username(user.getUsername())
                        .teamId(team != null ? team.getId() : null)
                        .teamName(team != null ? team.getTeamName() : null)
                        .action("理论考试监考心跳中断预警")
                        .logType("THEORY_CHEAT")
                        .params("心跳间隔异常: " + (diff / 1000) + "秒，疑似刷新或阻断")
                        .status(false)
                        .build();
                operationLogRepository.save(log);
            }
        }

        monitor.setIsOnline(true);
        monitor.setFullscreen(Boolean.TRUE.equals(request.getFullscreen()));
        monitor.setScreenActive(Boolean.TRUE.equals(request.getScreenActive()));
        if (StringUtils.hasText(request.getScreenFrame())) {
            monitor.setScreenFrame(request.getScreenFrame());
        }
        if (request.getAnsweredCount() != null) {
            monitor.setAnsweredCount(request.getAnsweredCount());
        }
        monitor.setViolationCount(violationCount);
        monitor.setIsBanned(isBanned);
        monitor.setLastHeartbeatTime(now);

        try {
            stringRedisTemplate.opsForValue().set(userKey, objectMapper.writeValueAsString(monitor), VREDIS_COMPETITION_THEORY_MONITOR_TTL);
        } catch (Exception ignored) {}

        String usersKey = VREDIS_COMPETITION_THEORY_MONITOR_USERS_KEY_PREFIX + competitionId;
        stringRedisTemplate.opsForSet().add(usersKey, userId.toString());

        return TheoryProctorHeartbeatVO.builder()
                .acknowledged(true)
                .isBanned(isBanned)
                .violationCount(violationCount)
                .serverTime(now)
                .build();
    }

    @Override
    public List<AdminTheoryCandidateMonitorVO> listCandidateMonitors(Long competitionId) {
        String usersKey = VREDIS_COMPETITION_THEORY_MONITOR_USERS_KEY_PREFIX + competitionId;
        Set<String> userIds = stringRedisTemplate.opsForSet().members(usersKey);
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminTheoryCandidateMonitorVO> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (String uidStr : userIds) {
            String userKey = VREDIS_COMPETITION_THEORY_MONITOR_USER_KEY_PREFIX + competitionId + ":" + uidStr;
            String raw = stringRedisTemplate.opsForValue().get(userKey);
            if (StringUtils.hasText(raw)) {
                try {
                    AdminTheoryCandidateMonitorVO m = objectMapper.readValue(raw, AdminTheoryCandidateMonitorVO.class);
                    if (m.getLastHeartbeatTime() == null || (now - m.getLastHeartbeatTime() > 15_000)) {
                        m.setIsOnline(false);
                    } else {
                        m.setIsOnline(true);
                    }
                    list.add(m);
                } catch (Exception ignored) {}
            }
        }

        list.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getIsOnline()) != Boolean.TRUE.equals(b.getIsOnline())) {
                return Boolean.TRUE.equals(b.getIsOnline()) ? 1 : -1;
            }
            return Long.compare(
                    b.getLastHeartbeatTime() != null ? b.getLastHeartbeatTime() : 0,
                    a.getLastHeartbeatTime() != null ? a.getLastHeartbeatTime() : 0
            );
        });

        return list;
    }

    private int getViolationCount(Long competitionId, Long userId) {
        String redisKey = VREDIS_COMPETITION_THEORY_CHEAT_KEY_PREFIX + competitionId + ":" + userId;
        String val = stringRedisTemplate.opsForValue().get(redisKey);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    @Override
    public void uploadHighResSnapshot(Long competitionId, Long userId, TheoryProctorSnapshotDTO request) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        if (request == null || !StringUtils.hasText(request.getScreenFrame())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "快照画面帧不能为空");
        }

        String userKey = VREDIS_COMPETITION_THEORY_MONITOR_USER_KEY_PREFIX + competitionId + ":" + userId;
        String raw = stringRedisTemplate.opsForValue().get(userKey);
        AdminTheoryCandidateMonitorVO monitor = null;
        if (StringUtils.hasText(raw)) {
            try {
                monitor = objectMapper.readValue(raw, AdminTheoryCandidateMonitorVO.class);
            } catch (Exception ignored) {}
        }

        long now = request.getTimestamp() != null && request.getTimestamp() > 0 ? request.getTimestamp() : System.currentTimeMillis();
        if (monitor == null) {
            User user = userRepository.findById(userId).orElse(null);
            CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
            monitor = AdminTheoryCandidateMonitorVO.builder()
                    .userId(userId)
                    .username(user != null ? user.getUsername() : String.valueOf(userId))
                    .teamId(team != null ? team.getId() : null)
                    .teamName(team != null ? team.getTeamName() : null)
                    .connectedSince(now)
                    .build();
        }

        monitor.setHighResFrame(request.getScreenFrame());
        monitor.setHighResTimestamp(now);
        monitor.setIsOnline(true);
        monitor.setLastHeartbeatTime(now);

        try {
            stringRedisTemplate.opsForValue().set(userKey, objectMapper.writeValueAsString(monitor), VREDIS_COMPETITION_THEORY_MONITOR_TTL);
        } catch (Exception ignored) {}

        String usersKey = VREDIS_COMPETITION_THEORY_MONITOR_USERS_KEY_PREFIX + competitionId;
        stringRedisTemplate.opsForSet().add(usersKey, userId.toString());

        realtimePushService.broadcastAll("THEORY_PROCTOR_SNAPSHOT_UPDATED", Map.of(
                "competitionId", competitionId,
                "userId", userId,
                "reason", request.getReason() != null ? request.getReason() : "INSPECT",
                "timestamp", now
        ));
    }

    @Override
    public void requestCandidateHighResSnapshot(Long competitionId, Long adminUserId, Long candidateUserId) {
        if (candidateUserId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "目标考生ID不能为空");
        }
        realtimePushService.sendToUser(candidateUserId, "THEORY_PROCTOR_SNAPSHOT_REQUEST", Map.of(
                "competitionId", competitionId,
                "adminId", adminUserId != null ? adminUserId : 0L,
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    public void forwardRtcSignal(Long competitionId, Long senderUserId, TheoryProctorRtcSignalDTO signal) {
        if (signal == null || signal.getTargetUserId() == null || !StringUtils.hasText(signal.getType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "信令参数不完整");
        }
        realtimePushService.sendToUser(signal.getTargetUserId(), "THEORY_PROCTOR_RTC_SIGNAL", Map.of(
                "competitionId", competitionId,
                "senderUserId", senderUserId,
                "signal", signal
        ));
    }

}

