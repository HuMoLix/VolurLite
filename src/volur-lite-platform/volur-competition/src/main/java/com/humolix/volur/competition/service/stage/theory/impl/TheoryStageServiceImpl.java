package com.humolix.volur.competition.service.stage.theory.impl;

import com.humolix.volur.challenge.domain.entity.stage.theory.TheoryChallenge;
import com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning;
import com.humolix.volur.competition.domain.dto.stage.theory.TheoryAnswerItemDTO;
import com.humolix.volur.competition.domain.dto.stage.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.stage.theory.TheoryCompetitionAnswer;
import com.humolix.volur.competition.domain.entity.stage.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperQuestionVO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.stage.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.repository.stage.theory.TheoryCompetitionPaperRepository;
import com.humolix.volur.competition.service.stage.theory.TheoryStageService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@RequiredCompetitionStageRunning(stage = CompetitionStage.THEORY)
public class TheoryStageServiceImpl implements TheoryStageService {

    private final CompetitionMemberRepository memberRepository;
    private final TheoryCompetitionPaperRepository paperRepository;
    private final TheoryCompetitionAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final CompetitionMapper competitionMapper;

    @Override
    public TheoryPaperExamVO getPaper(Long competitionId, Long userId) {
        List<TheoryCompetitionPaper> papers = paperRepository.findByCompetitionIdWithChallenge(competitionId);
        List<TheoryPaperQuestionVO> questionVOs = competitionMapper.toTheoryPaperQuestionVOList(papers);

        int totalScore = papers.stream().mapToInt(TheoryCompetitionPaper::getScore).sum();
        boolean isSubmitted = false;
        int myTotalScore = 0;

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
                .competitionId(competitionId)
                .totalQuestions(questionVOs.size())
                .totalScore(totalScore)
                .isSubmitted(isSubmitted)
                .myTotalScore(myTotalScore)
                .questions(questionVOs)
                .build();
    }

    @Override
    @Transactional
    public TheoryPaperResultVO submitPaper(Long competitionId, Long userId, TheoryPaperSubmitRequestDTO request) {
        CompetitionTeam team = getMyApprovedTeam(competitionId, userId);
        if (team == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍");
        }

        List<TheoryCompetitionPaper> papers = paperRepository.findByCompetitionIdWithChallenge(competitionId);
        if (papers.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "本场竞赛未配置理论试卷");
        }

        Competition competition = papers.getFirst().getCompetition();

        Map<Long, TheoryCompetitionPaper> paperMap = papers.stream()
                .collect(Collectors.toMap(TheoryCompetitionPaper::getId, p -> p));

        Map<Long, String> inputAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(TheoryAnswerItemDTO::getPaperId, a -> a.getAnswer() != null ? a.getAnswer().trim() : "", (k1, k2) -> k2));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        int totalScore = 0;
        int scoreObtained = 0;
        int correctCount = 0;
        List<TheoryCompetitionAnswer> answersToSave = new ArrayList<>();

        for (TheoryCompetitionPaper paper : papers) {
            int paperScore = paper.getScore() != null ? paper.getScore() : 5;
            totalScore += paperScore;

            String submittedAnswer = inputAnswers.getOrDefault(paper.getId(), "");
            TheoryChallenge rawChallenge = paper.getChallenge();
            String stdAnswer = rawChallenge != null && rawChallenge.getStandardAnswer() != null ? rawChallenge.getStandardAnswer().trim() : "";

            boolean isCorrect = StringUtils.hasText(submittedAnswer) && submittedAnswer.equalsIgnoreCase(stdAnswer);
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

        double accuracyRate = papers.isEmpty() ? 0.0 : (double) correctCount / papers.size() * 100.0;

        return TheoryPaperResultVO.builder()
                .totalQuestions(papers.size())
                .correctCount(correctCount)
                .totalScore(totalScore)
                .scoreObtained(scoreObtained)
                .accuracyRate(Math.round(accuracyRate * 100.0) / 100.0)
                .build();
    }

    private CompetitionTeam getMyApprovedTeam(Long competitionId, Long userId) {
        if (userId == null) return null;
        Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(competitionId, userId);
        if (memberOpt.isPresent()) {
            CompetitionTeam team = memberOpt.get().getTeam();
            if (team != null && team.getStatus() == TeamStatus.AUDIT_PASSED) {
                return team;
            }
        }
        return null;
    }

}
