package com.humolix.volur.competition.service.admin.impl;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionSolved;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionAnswer;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.entity.theory.TheoryUserPaper;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.export.*;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionSolvedRepository;
import com.humolix.volur.competition.repository.theory.TheoryCompetitionAnswerRepository;
import com.humolix.volur.competition.repository.theory.TheoryUserPaperRepository;
import com.humolix.volur.competition.service.admin.AdminCompetitionScoreExportService;
import com.humolix.volur.competition.util.CsvExportUtil;
import com.humolix.volur.competition.util.ExcelExportUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.Gender;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCompetitionScoreExportServiceImpl implements AdminCompetitionScoreExportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CtfCompetitionSolvedRepository ctfSolvedRepository;
    private final TheoryUserPaperRepository userPaperRepository;
    private final TheoryCompetitionAnswerRepository theoryAnswerRepository;
    private final ObjectMapper objectMapper;

    private record ColumnDefinition<T>(String key, String header, String group, boolean defaultSelected, Function<T, String> extractor) {}

    @Override
    @Transactional(readOnly = true)
    public List<ExportFieldOptionVO> getExportFieldOptions(Long competitionId, String scope) {
        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        String scp = normalizeScope(scope);
        CompetitionType type = comp.getType();
        List<ExportFieldOptionVO> result = new ArrayList<>();

        if (type == CompetitionType.CTF) {
            if ("members".equals(scp) || "registration".equals(scp)) {
                for (ColumnDefinition<CtfTeamMemberExportVO> col : getCtfMemberColumns()) {
                    result.add(ExportFieldOptionVO.builder()
                            .key(col.key())
                            .label(col.header())
                            .group(col.group())
                            .defaultSelected(col.defaultSelected())
                            .build());
                }
            } else if ("detail".equals(scp)) {
                for (ColumnDefinition<CtfSolveDetailExportVO> col : getCtfSolveColumns()) {
                    result.add(ExportFieldOptionVO.builder()
                            .key(col.key())
                            .label(col.header())
                            .group(col.group())
                            .defaultSelected(col.defaultSelected())
                            .build());
                }
            } else {
                for (ColumnDefinition<CtfTeamRankExportVO> col : getCtfRankColumns()) {
                    result.add(ExportFieldOptionVO.builder()
                            .key(col.key())
                            .label(col.header())
                            .group(col.group())
                            .defaultSelected(col.defaultSelected())
                            .build());
                }
            }
        } else if (type == CompetitionType.THEORY) {
            if ("detail".equals(scp)) {
                for (ColumnDefinition<TheoryAnswerDetailExportVO> col : getTheoryAnswerColumns()) {
                    result.add(ExportFieldOptionVO.builder()
                            .key(col.key())
                            .label(col.header())
                            .group(col.group())
                            .defaultSelected(col.defaultSelected())
                            .build());
                }
            } else {
                for (ColumnDefinition<TheoryCandidateScoreExportVO> col : getTheoryCandidateColumns()) {
                    result.add(ExportFieldOptionVO.builder()
                            .key(col.key())
                            .label(col.header())
                            .group(col.group())
                            .defaultSelected(col.defaultSelected())
                            .build());
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void exportScores(Long competitionId, String format, String scope, List<String> fields, HttpServletResponse response) {
        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        String fmt = format != null ? format.trim().toLowerCase() : "xlsx";
        String scp = normalizeScope(scope);

        if (!fmt.equals("xlsx") && !fmt.equals("csv") && !fmt.equals("json")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的导出格式，仅支持 xlsx、csv、json");
        }

        List<String> normalizedFields = fields == null ? List.of() : fields.stream()
                .flatMap(f -> Arrays.stream(f.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CompetitionType type = comp.getType();
        if (type == CompetitionType.CTF) {
            exportCtf(comp, fmt, scp, normalizedFields, response);
        } else if (type == CompetitionType.THEORY) {
            exportTheory(comp, fmt, scp, normalizedFields, response);
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前赛制暂不支持导出功能");
        }
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "summary";
        }
        String s = scope.trim().toLowerCase();
        if (s.equals("members") || s.equals("registration") || s.equals("team_members")) {
            return "members";
        }
        if (s.equals("detail") || s.equals("details")) {
            return "detail";
        }
        if (s.equals("all") || s.equals("full")) {
            return "all";
        }
        return "summary";
    }

    private void exportCtf(Competition comp, String fmt, String scp, List<String> fields, HttpServletResponse response) {
        Long compId = comp.getId();
        List<CompetitionTeam> teams = teamRepository.findByCompetitionIdAndStatusIn(compId, List.of(TeamStatus.AUDIT_PASSED, TeamStatus.ENDED));
        List<CompetitionMember> allMembers = memberRepository.findByCompetitionIdWithUserAndTeam(compId);
        Map<Long, List<CompetitionMember>> teamMembersMap = allMembers.stream()
                .filter(m -> m.getTeam() != null)
                .collect(Collectors.groupingBy(m -> m.getTeam().getId()));

        List<CtfCompetitionSolved> allSolves = ctfSolvedRepository.findAllSolvesWithDetails(compId);

        Map<Long, Integer> teamScoreMap = new HashMap<>();
        Map<Long, Integer> teamSolvesMap = new HashMap<>();
        Map<Long, Instant> teamLastSolveMap = new HashMap<>();

        for (CtfCompetitionSolved solve : allSolves) {
            if (solve.getTeam() == null) continue;
            Long tid = solve.getTeam().getId();
            teamScoreMap.merge(tid, solve.getPointsAwarded() != null ? solve.getPointsAwarded() : 0, Integer::sum);
            teamSolvesMap.merge(tid, 1, Integer::sum);

            Instant solvedAt = solve.getSolvedAt();
            if (solvedAt != null) {
                teamLastSolveMap.compute(tid, (k, current) -> current == null || solvedAt.isAfter(current) ? solvedAt : current);
            }
        }

        List<CtfTeamRankExportVO> rankings = new ArrayList<>();
        for (CompetitionTeam team : teams) {
            User captain = team.getCaptain();
            List<CompetitionMember> tMembers = teamMembersMap.getOrDefault(team.getId(), List.of());
            CompetitionMember captainMember = tMembers.stream()
                    .filter(m -> m.getRole() == MemberRole.CAPTAIN)
                    .findFirst()
                    .orElse(null);

            String cRealName = captainMember != null && captainMember.getRealName() != null ? captainMember.getRealName() : (captain != null ? captain.getRealName() : "");
            Gender cGender = captainMember != null && captainMember.getGender() != null ? captainMember.getGender() : (captain != null ? captain.getGender() : null);
            String cPhone = captainMember != null && captainMember.getPhone() != null ? captainMember.getPhone() : (captain != null && captain.getPhone() != null ? captain.getPhone() : "");
            String cEmail = captainMember != null && captainMember.getEmail() != null ? captainMember.getEmail() : (captain != null && captain.getEmail() != null ? captain.getEmail() : "");
            String cIdCard = captainMember != null && captainMember.getIdCard() != null ? captainMember.getIdCard() : "";
            String cEmpNo = captainMember != null && captainMember.getEmployeeNumber() != null ? captainMember.getEmployeeNumber() : "";
            String cDept = captainMember != null && captainMember.getDepartment() != null ? captainMember.getDepartment() : "";
            String cPos = captainMember != null && captainMember.getPosition() != null ? captainMember.getPosition() : "";

            rankings.add(CtfTeamRankExportVO.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .organization(team.getOrganization())
                    .leaderUsername(captain != null ? captain.getUsername() : "")
                    .leaderRealName(cRealName)
                    .leaderGender(cGender)
                    .leaderPhone(cPhone)
                    .leaderEmail(cEmail)
                    .leaderIdCard(cIdCard)
                    .leaderEmployeeNumber(cEmpNo)
                    .leaderDepartment(cDept)
                    .leaderPosition(cPos)
                    .score(teamScoreMap.getOrDefault(team.getId(), 0))
                    .solvesCount(teamSolvesMap.getOrDefault(team.getId(), 0))
                    .lastSolveTime(teamLastSolveMap.get(team.getId()))
                    .build());
        }

        rankings.sort((a, b) -> {
            int cmp = Integer.compare(b.getScore(), a.getScore());
            if (cmp != 0) return cmp;
            if (a.getLastSolveTime() == null && b.getLastSolveTime() == null) return 0;
            if (a.getLastSolveTime() == null) return 1;
            if (b.getLastSolveTime() == null) return -1;
            return a.getLastSolveTime().compareTo(b.getLastSolveTime());
        });

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        List<CtfTeamMemberExportVO> memberList = new ArrayList<>();
        if ("members".equals(scp) || "all".equals(scp)) {
            for (CompetitionTeam team : teams) {
                List<CompetitionMember> tMembers = teamMembersMap.getOrDefault(team.getId(), List.of());
                List<CompetitionMember> sortedMembers = new ArrayList<>(tMembers);
                sortedMembers.sort(Comparator.comparingInt((CompetitionMember m) -> {
                    if (m.getRole() == MemberRole.CAPTAIN) return 0;
                    if (m.getRole() == MemberRole.MEMBER) return 1;
                    return 2;
                }).thenComparing(CompetitionMember::getId));

                for (CompetitionMember m : sortedMembers) {
                    User u = m.getUser();
                    String rName = m.getRealName() != null ? m.getRealName() : (u != null ? u.getRealName() : "");
                    Gender g = m.getGender() != null ? m.getGender() : (u != null ? u.getGender() : null);
                    String ph = m.getPhone() != null ? m.getPhone() : (u != null && u.getPhone() != null ? u.getPhone() : "");
                    String em = m.getEmail() != null ? m.getEmail() : (u != null && u.getEmail() != null ? u.getEmail() : "");

                    memberList.add(CtfTeamMemberExportVO.builder()
                            .teamId(team.getId())
                            .teamName(team.getTeamName())
                            .organization(team.getOrganization())
                            .role(m.getRole())
                            .userId(u != null ? u.getId() : null)
                            .username(u != null ? u.getUsername() : "")
                            .realName(rName)
                            .gender(g)
                            .phone(ph)
                            .email(em)
                            .idCard(m.getIdCard())
                            .employeeNumber(m.getEmployeeNumber())
                            .department(m.getDepartment())
                            .position(m.getPosition())
                            .status(m.getStatus())
                            .joinedAt(m.getJoinedAt())
                            .build());
                }
            }
        }

        List<CtfSolveDetailExportVO> details = new ArrayList<>();
        if ("detail".equals(scp) || "all".equals(scp)) {
            List<CtfCompetitionSolved> sortedSolves = new ArrayList<>(allSolves);
            sortedSolves.sort(Comparator.comparing(CtfCompetitionSolved::getSolvedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            for (CtfCompetitionSolved solve : sortedSolves) {
                User u = solve.getUser();
                CtfCompetitionChallenge compChal = solve.getCompetitionChallenge();
                com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge chal = compChal != null ? compChal.getChallenge() : null;
                details.add(CtfSolveDetailExportVO.builder()
                        .solveId(solve.getId())
                        .teamId(solve.getTeam() != null ? solve.getTeam().getId() : null)
                        .teamName(solve.getTeam() != null ? solve.getTeam().getTeamName() : "")
                        .challengeName(chal != null ? chal.getName() : "")
                        .category(chal != null && chal.getCategory() != null ? chal.getCategory().getName() : "Misc")
                        .username(u != null ? u.getUsername() : "")
                        .realName(u != null ? u.getRealName() : "")
                        .pointsAwarded(solve.getPointsAwarded())
                        .solvedAt(solve.getSolvedAt())
                        .isFirstBlood(Boolean.TRUE.equals(solve.getIsFirstBlood()))
                        .build());
            }
        }

        String baseName = sanitizeFileName(comp.getTitle()) + "_CTF数据报表";

        List<ColumnDefinition<CtfTeamRankExportVO>> rankCols = resolveColumns(getCtfRankColumns(), fields);
        List<ColumnDefinition<CtfTeamMemberExportVO>> memberCols = resolveColumns(getCtfMemberColumns(), fields);
        List<ColumnDefinition<CtfSolveDetailExportVO>> solveCols = resolveColumns(getCtfSolveColumns(), fields);

        if ("json".equals(fmt)) {
            Map<String, Object> exportMap = new LinkedHashMap<>();
            exportMap.put("competitionId", compId);
            exportMap.put("competitionTitle", comp.getTitle());
            exportMap.put("exportScope", scp);
            exportMap.put("exportTime", Instant.now());

            if (!"members".equals(scp)) {
                exportMap.put("rankings", toMappedRows(rankings, rankCols));
            }
            if ("members".equals(scp) || "all".equals(scp)) {
                exportMap.put("members", toMappedRows(memberList, memberCols));
            }
            if ("detail".equals(scp) || "all".equals(scp)) {
                exportMap.put("details", toMappedRows(details, solveCols));
            }
            writeJson(response, baseName + ".json", exportMap);
        } else if ("csv".equals(fmt)) {
            List<CsvExportUtil.CsvSection> sections = new ArrayList<>();
            if ("members".equals(scp)) {
                sections.add(new CsvExportUtil.CsvSection("【参赛队员报名名单】", extractHeaders(memberCols), extractRows(memberList, memberCols)));
            } else if ("detail".equals(scp)) {
                sections.add(new CsvExportUtil.CsvSection("【解题明细流水】", extractHeaders(solveCols), extractRows(details, solveCols)));
            } else if ("all".equals(scp)) {
                sections.add(new CsvExportUtil.CsvSection("【CTF 战队榜单】", extractHeaders(rankCols), extractRows(rankings, rankCols)));
                sections.add(new CsvExportUtil.CsvSection("【参赛队员报名名单】", extractHeaders(memberCols), extractRows(memberList, memberCols)));
                sections.add(new CsvExportUtil.CsvSection("【解题明细流水】", extractHeaders(solveCols), extractRows(details, solveCols)));
            } else {
                sections.add(new CsvExportUtil.CsvSection("【CTF 战队榜单】", extractHeaders(rankCols), extractRows(rankings, rankCols)));
            }
            writeCsvSections(response, baseName + ".csv", sections);
        } else {
            List<ExcelExportUtil.SheetData> sheets = new ArrayList<>();
            if ("members".equals(scp)) {
                sheets.add(new ExcelExportUtil.SheetData("队员报名名单", extractHeaders(memberCols), extractRows(memberList, memberCols)));
            } else if ("detail".equals(scp)) {
                sheets.add(new ExcelExportUtil.SheetData("解题明细流水", extractHeaders(solveCols), extractRows(details, solveCols)));
            } else if ("all".equals(scp)) {
                sheets.add(new ExcelExportUtil.SheetData("战队排名榜单", extractHeaders(rankCols), extractRows(rankings, rankCols)));
                sheets.add(new ExcelExportUtil.SheetData("队员报名名单", extractHeaders(memberCols), extractRows(memberList, memberCols)));
                sheets.add(new ExcelExportUtil.SheetData("解题明细流水", extractHeaders(solveCols), extractRows(details, solveCols)));
            } else {
                sheets.add(new ExcelExportUtil.SheetData("战队排名榜单", extractHeaders(rankCols), extractRows(rankings, rankCols)));
            }
            writeExcelSheets(response, baseName + ".xlsx", sheets);
        }
    }

    private void exportTheory(Competition comp, String fmt, String scp, List<String> fields, HttpServletResponse response) {
        Long compId = comp.getId();
        List<TheoryUserPaper> userPapers = userPaperRepository.findByCompetitionId(compId);
        List<TheoryCompetitionAnswer> allAnswers = theoryAnswerRepository.findByCompetitionId(compId);
        List<CompetitionMember> allMembers = memberRepository.findByCompetitionIdWithUserAndTeam(compId);

        Map<Long, CompetitionMember> userMemberMap = allMembers.stream()
                .filter(m -> m.getUser() != null)
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, b) -> a));

        Map<Long, List<TheoryCompetitionAnswer>> userAnswersMap = new HashMap<>();
        for (TheoryCompetitionAnswer ans : allAnswers) {
            if (ans.getUser() != null) {
                userAnswersMap.computeIfAbsent(ans.getUser().getId(), k -> new ArrayList<>()).add(ans);
            }
        }

        List<TheoryCandidateScoreExportVO> candidates = new ArrayList<>();
        for (TheoryUserPaper up : userPapers) {
            User u = up.getUser();
            Long uid = u != null ? u.getId() : null;
            CompetitionMember member = uid != null ? userMemberMap.get(uid) : null;
            List<TheoryCompetitionAnswer> ansList = uid != null ? userAnswersMap.getOrDefault(uid, List.of()) : List.of();

            int correctCount = 0;
            for (TheoryCompetitionAnswer a : ansList) {
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    correctCount++;
                }
            }

            int totalQuestions = up.getTotalQuestions() != null ? up.getTotalQuestions() : ansList.size();
            double accRate = totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
            double roundedAcc = Math.round(accRate * 100.0) / 100.0;

            String empNo = member != null && member.getEmployeeNumber() != null ? member.getEmployeeNumber() : "";
            String idCard = member != null && member.getIdCard() != null ? member.getIdCard() : "";
            String phone = member != null && member.getPhone() != null ? member.getPhone() : (u != null && u.getPhone() != null ? u.getPhone() : "");
            String email = member != null && member.getEmail() != null ? member.getEmail() : (u != null && u.getEmail() != null ? u.getEmail() : "");
            String realName = member != null && member.getRealName() != null ? member.getRealName() : (u != null && u.getRealName() != null ? u.getRealName() : "");
            Gender gender = member != null && member.getGender() != null ? member.getGender() : (u != null ? u.getGender() : null);
            String dept = member != null && member.getDepartment() != null ? member.getDepartment() : "";
            String pos = member != null && member.getPosition() != null ? member.getPosition() : "";

            candidates.add(TheoryCandidateScoreExportVO.builder()
                    .teamId(up.getTeam() != null ? up.getTeam().getId() : null)
                    .teamName(up.getTeam() != null ? up.getTeam().getTeamName() : "")
                    .userId(uid)
                    .username(u != null ? u.getUsername() : "")
                    .realName(realName)
                    .gender(gender)
                    .phone(phone)
                    .email(email)
                    .idCard(idCard)
                    .employeeNumber(empNo)
                    .department(dept)
                    .position(pos)
                    .isSubmitted(Boolean.TRUE.equals(up.getIsSubmitted()))
                    .correctCount(correctCount)
                    .totalQuestions(totalQuestions)
                    .totalScore(up.getTotalScore() != null ? up.getTotalScore() : 0)
                    .scoreObtained(up.getMyTotalScore() != null ? up.getMyTotalScore() : 0)
                    .accuracyRate(roundedAcc)
                    .violationCount(0)
                    .submittedAt(up.getUpdatedAt())
                    .build());
        }

        candidates.sort((a, b) -> {
            int cmp = Integer.compare(b.getScoreObtained(), a.getScoreObtained());
            if (cmp != 0) return cmp;
            if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) return 0;
            if (a.getSubmittedAt() == null) return 1;
            if (b.getSubmittedAt() == null) return -1;
            return a.getSubmittedAt().compareTo(b.getSubmittedAt());
        });

        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setRank(i + 1);
        }

        List<TheoryAnswerDetailExportVO> details = new ArrayList<>();
        if ("detail".equals(scp) || "all".equals(scp)) {
            List<TheoryCompetitionAnswer> sortedAnswers = new ArrayList<>(allAnswers);
            sortedAnswers.sort((a, b) -> {
                Long uidA = a.getUser() != null ? a.getUser().getId() : 0L;
                Long uidB = b.getUser() != null ? b.getUser().getId() : 0L;
                int uCmp = uidA.compareTo(uidB);
                if (uCmp != 0) return uCmp;
                Integer oA = a.getPaper() != null && a.getPaper().getSortOrder() != null ? a.getPaper().getSortOrder() : 0;
                Integer oB = b.getPaper() != null && b.getPaper().getSortOrder() != null ? b.getPaper().getSortOrder() : 0;
                return oA.compareTo(oB);
            });

            for (TheoryCompetitionAnswer ans : sortedAnswers) {
                User u = ans.getUser();
                TheoryCompetitionPaper paper = ans.getPaper();
                TheoryChallenge chal = paper != null ? paper.getChallenge() : null;

                String stdAns = paper != null && paper.getShuffledStandardAnswer() != null && !paper.getShuffledStandardAnswer().isBlank()
                        ? paper.getShuffledStandardAnswer()
                        : (chal != null ? chal.getStandardAnswer() : "");

                details.add(TheoryAnswerDetailExportVO.builder()
                        .userId(u != null ? u.getId() : null)
                        .username(u != null ? u.getUsername() : "")
                        .realName(u != null ? u.getRealName() : "")
                        .teamName(ans.getTeam() != null ? ans.getTeam().getTeamName() : "")
                        .paperId(paper != null ? paper.getId() : null)
                        .challengeName(chal != null ? chal.getContent() : "")
                        .type(chal != null && chal.getType() != null ? chal.getType().name() : "")
                        .score(paper != null ? paper.getScore() : 0)
                        .userAnswer(ans.getUserAnswer())
                        .standardAnswer(stdAns)
                        .isCorrect(ans.getIsCorrect())
                        .scoreObtained(ans.getScoreObtained())
                        .build());
            }
        }

        String baseName = sanitizeFileName(comp.getTitle()) + "_理论知识赛数据报表";

        List<ColumnDefinition<TheoryCandidateScoreExportVO>> candCols = resolveColumns(getTheoryCandidateColumns(), fields);
        List<ColumnDefinition<TheoryAnswerDetailExportVO>> ansCols = resolveColumns(getTheoryAnswerColumns(), fields);

        if ("json".equals(fmt)) {
            Map<String, Object> exportMap = new LinkedHashMap<>();
            exportMap.put("competitionId", compId);
            exportMap.put("competitionTitle", comp.getTitle());
            exportMap.put("exportScope", scp);
            exportMap.put("exportTime", Instant.now());
            exportMap.put("candidates", toMappedRows(candidates, candCols));
            if ("detail".equals(scp) || "all".equals(scp)) {
                exportMap.put("details", toMappedRows(details, ansCols));
            }
            writeJson(response, baseName + ".json", exportMap);
        } else if ("csv".equals(fmt)) {
            List<CsvExportUtil.CsvSection> sections = new ArrayList<>();
            if ("detail".equals(scp)) {
                sections.add(new CsvExportUtil.CsvSection("【答题明细流水】", extractHeaders(ansCols), extractRows(details, ansCols)));
            } else if ("all".equals(scp)) {
                sections.add(new CsvExportUtil.CsvSection("【考生总榜及报名信息】", extractHeaders(candCols), extractRows(candidates, candCols)));
                sections.add(new CsvExportUtil.CsvSection("【答题明细流水】", extractHeaders(ansCols), extractRows(details, ansCols)));
            } else {
                sections.add(new CsvExportUtil.CsvSection("【考生总榜及报名信息】", extractHeaders(candCols), extractRows(candidates, candCols)));
            }
            writeCsvSections(response, baseName + ".csv", sections);
        } else {
            List<ExcelExportUtil.SheetData> sheets = new ArrayList<>();
            if ("detail".equals(scp)) {
                sheets.add(new ExcelExportUtil.SheetData("答题明细流水", extractHeaders(ansCols), extractRows(details, ansCols)));
            } else if ("all".equals(scp)) {
                sheets.add(new ExcelExportUtil.SheetData("考生总榜与报名信息", extractHeaders(candCols), extractRows(candidates, candCols)));
                sheets.add(new ExcelExportUtil.SheetData("答题明细流水", extractHeaders(ansCols), extractRows(details, ansCols)));
            } else {
                sheets.add(new ExcelExportUtil.SheetData("考生总榜与报名信息", extractHeaders(candCols), extractRows(candidates, candCols)));
            }
            writeExcelSheets(response, baseName + ".xlsx", sheets);
        }
    }

    private <T> List<ColumnDefinition<T>> resolveColumns(List<ColumnDefinition<T>> allColumns, List<String> requestedFieldKeys) {
        if (requestedFieldKeys == null || requestedFieldKeys.isEmpty()) {
            return allColumns.stream().filter(ColumnDefinition::defaultSelected).toList();
        }

        Map<String, ColumnDefinition<T>> keyMap = new HashMap<>();
        for (ColumnDefinition<T> col : allColumns) {
            keyMap.put(col.key().toLowerCase(), col);
        }

        List<ColumnDefinition<T>> resolved = new ArrayList<>();
        for (String reqKey : requestedFieldKeys) {
            String lower = reqKey.trim().toLowerCase();
            ColumnDefinition<T> match = keyMap.get(lower);
            if (match == null) {
                if (lower.equals("score") && keyMap.containsKey("scoreobtained")) {
                    match = keyMap.get("scoreobtained");
                } else if (lower.equals("username") && keyMap.containsKey("leaderusername")) {
                    match = keyMap.get("leaderusername");
                } else if (lower.equals("realname") && keyMap.containsKey("leaderrealname")) {
                    match = keyMap.get("leaderrealname");
                } else if (lower.equals("employeenumber") && keyMap.containsKey("leaderemployeenumber")) {
                    match = keyMap.get("leaderemployeenumber");
                } else if (lower.equals("idcard") && keyMap.containsKey("leaderidcard")) {
                    match = keyMap.get("leaderidcard");
                } else if (lower.equals("phone") && keyMap.containsKey("leaderphone")) {
                    match = keyMap.get("leaderphone");
                } else if (lower.equals("email") && keyMap.containsKey("leaderemail")) {
                    match = keyMap.get("leaderemail");
                } else if (lower.equals("department") && keyMap.containsKey("leaderdepartment")) {
                    match = keyMap.get("leaderdepartment");
                } else if (lower.equals("position") && keyMap.containsKey("leaderposition")) {
                    match = keyMap.get("leaderposition");
                } else if (lower.equals("gender") && keyMap.containsKey("leadergender")) {
                    match = keyMap.get("leadergender");
                }
            }
            if (match != null && !resolved.contains(match)) {
                resolved.add(match);
            }
        }

        return resolved.isEmpty() ? allColumns.stream().filter(ColumnDefinition::defaultSelected).toList() : resolved;
    }

    private <T> List<String> extractHeaders(List<ColumnDefinition<T>> columns) {
        return columns.stream().map(ColumnDefinition::header).toList();
    }

    private <T> List<List<String>> extractRows(List<T> items, List<ColumnDefinition<T>> columns) {
        List<List<String>> rows = new ArrayList<>(items.size());
        for (T item : items) {
            List<String> row = new ArrayList<>(columns.size());
            for (ColumnDefinition<T> col : columns) {
                row.add(nullToEmpty(col.extractor().apply(item)));
            }
            rows.add(row);
        }
        return rows;
    }

    private <T> List<Map<String, String>> toMappedRows(List<T> items, List<ColumnDefinition<T>> columns) {
        List<Map<String, String>> result = new ArrayList<>(items.size());
        for (T item : items) {
            Map<String, String> row = new LinkedHashMap<>();
            for (ColumnDefinition<T> col : columns) {
                row.put(col.key(), nullToEmpty(col.extractor().apply(item)));
            }
            result.add(row);
        }
        return result;
    }

    private List<ColumnDefinition<CtfTeamRankExportVO>> getCtfRankColumns() {
        return List.of(
                new ColumnDefinition<>("rank", "排名", "score", true, r -> String.valueOf(r.getRank())),
                new ColumnDefinition<>("teamId", "战队ID", "basic", true, r -> String.valueOf(r.getTeamId())),
                new ColumnDefinition<>("teamName", "战队名称", "basic", true, CtfTeamRankExportVO::getTeamName),
                new ColumnDefinition<>("organization", "所属单位/机构", "basic", true, CtfTeamRankExportVO::getOrganization),
                new ColumnDefinition<>("leaderRealName", "队长姓名", "registration", true, CtfTeamRankExportVO::getLeaderRealName),
                new ColumnDefinition<>("leaderEmployeeNumber", "队长学号/工号", "registration", true, CtfTeamRankExportVO::getLeaderEmployeeNumber),
                new ColumnDefinition<>("leaderIdCard", "队长身份证号", "registration", true, CtfTeamRankExportVO::getLeaderIdCard),
                new ColumnDefinition<>("leaderPhone", "队长手机号", "registration", true, CtfTeamRankExportVO::getLeaderPhone),
                new ColumnDefinition<>("leaderEmail", "队长邮箱", "registration", true, CtfTeamRankExportVO::getLeaderEmail),
                new ColumnDefinition<>("leaderDepartment", "队长学院/部门", "registration", false, CtfTeamRankExportVO::getLeaderDepartment),
                new ColumnDefinition<>("leaderPosition", "队长专业/职务", "registration", false, CtfTeamRankExportVO::getLeaderPosition),
                new ColumnDefinition<>("leaderGender", "队长性别", "registration", false, r -> formatGender(r.getLeaderGender())),
                new ColumnDefinition<>("leaderUsername", "队长账号", "basic", false, CtfTeamRankExportVO::getLeaderUsername),
                new ColumnDefinition<>("score", "总积分", "score", true, r -> String.valueOf(r.getScore())),
                new ColumnDefinition<>("solvesCount", "解题数", "score", true, r -> String.valueOf(r.getSolvesCount())),
                new ColumnDefinition<>("lastSolveTime", "最后解题时间", "score", true, r -> formatInstant(r.getLastSolveTime()))
        );
    }

    private List<ColumnDefinition<CtfTeamMemberExportVO>> getCtfMemberColumns() {
        return List.of(
                new ColumnDefinition<>("teamName", "战队名称", "basic", true, CtfTeamMemberExportVO::getTeamName),
                new ColumnDefinition<>("organization", "队伍所属单位", "basic", true, CtfTeamMemberExportVO::getOrganization),
                new ColumnDefinition<>("role", "队伍角色", "basic", true, m -> formatRole(m.getRole())),
                new ColumnDefinition<>("realName", "真实姓名", "registration", true, CtfTeamMemberExportVO::getRealName),
                new ColumnDefinition<>("gender", "性别", "registration", true, m -> formatGender(m.getGender())),
                new ColumnDefinition<>("employeeNumber", "学号/工号", "registration", true, CtfTeamMemberExportVO::getEmployeeNumber),
                new ColumnDefinition<>("idCard", "身份证号", "registration", true, CtfTeamMemberExportVO::getIdCard),
                new ColumnDefinition<>("phone", "手机号", "registration", true, CtfTeamMemberExportVO::getPhone),
                new ColumnDefinition<>("email", "电子邮箱", "registration", true, CtfTeamMemberExportVO::getEmail),
                new ColumnDefinition<>("department", "学院/部门", "registration", true, CtfTeamMemberExportVO::getDepartment),
                new ColumnDefinition<>("position", "专业/班级/职务", "registration", true, CtfTeamMemberExportVO::getPosition),
                new ColumnDefinition<>("username", "用户账号", "basic", false, CtfTeamMemberExportVO::getUsername),
                new ColumnDefinition<>("status", "成员状态", "basic", false, m -> m.getStatus() != null ? (m.getStatus() == MemberStatus.INVITED_ACCEPTED ? "已加入" : m.getStatus().name()) : ""),
                new ColumnDefinition<>("joinedAt", "加入时间", "basic", false, m -> formatInstant(m.getJoinedAt()))
        );
    }

    private List<ColumnDefinition<CtfSolveDetailExportVO>> getCtfSolveColumns() {
        return List.of(
                new ColumnDefinition<>("solveId", "解题记录ID", "detail", true, d -> String.valueOf(d.getSolveId())),
                new ColumnDefinition<>("teamName", "战队名称", "detail", true, CtfSolveDetailExportVO::getTeamName),
                new ColumnDefinition<>("challengeName", "赛题名称", "detail", true, CtfSolveDetailExportVO::getChallengeName),
                new ColumnDefinition<>("category", "赛题分类", "detail", true, CtfSolveDetailExportVO::getCategory),
                new ColumnDefinition<>("username", "解题队员账号", "detail", true, CtfSolveDetailExportVO::getUsername),
                new ColumnDefinition<>("realName", "解题队员姓名", "detail", true, CtfSolveDetailExportVO::getRealName),
                new ColumnDefinition<>("pointsAwarded", "获得积分", "detail", true, d -> String.valueOf(d.getPointsAwarded())),
                new ColumnDefinition<>("solvedAt", "解题时间", "detail", true, d -> formatInstant(d.getSolvedAt())),
                new ColumnDefinition<>("isFirstBlood", "是否一血", "detail", true, d -> Boolean.TRUE.equals(d.getIsFirstBlood()) ? "是" : "否")
        );
    }

    private List<ColumnDefinition<TheoryCandidateScoreExportVO>> getTheoryCandidateColumns() {
        return List.of(
                new ColumnDefinition<>("rank", "排名", "score", true, c -> String.valueOf(c.getRank())),
                new ColumnDefinition<>("teamName", "战队名称", "basic", true, TheoryCandidateScoreExportVO::getTeamName),
                new ColumnDefinition<>("username", "考生账号", "basic", true, TheoryCandidateScoreExportVO::getUsername),
                new ColumnDefinition<>("realName", "考生姓名", "registration", true, TheoryCandidateScoreExportVO::getRealName),
                new ColumnDefinition<>("gender", "性别", "registration", false, c -> formatGender(c.getGender())),
                new ColumnDefinition<>("employeeNumber", "学号/工号", "registration", true, TheoryCandidateScoreExportVO::getEmployeeNumber),
                new ColumnDefinition<>("idCard", "身份证号", "registration", true, TheoryCandidateScoreExportVO::getIdCard),
                new ColumnDefinition<>("phone", "手机号", "registration", true, TheoryCandidateScoreExportVO::getPhone),
                new ColumnDefinition<>("email", "电子邮箱", "registration", true, TheoryCandidateScoreExportVO::getEmail),
                new ColumnDefinition<>("department", "学院/部门", "registration", false, TheoryCandidateScoreExportVO::getDepartment),
                new ColumnDefinition<>("position", "专业/职务", "registration", false, TheoryCandidateScoreExportVO::getPosition),
                new ColumnDefinition<>("isSubmitted", "是否交卷", "score", true, c -> Boolean.TRUE.equals(c.getIsSubmitted()) ? "已交卷" : "未交卷"),
                new ColumnDefinition<>("correctCount", "正确题数", "score", true, c -> String.valueOf(c.getCorrectCount())),
                new ColumnDefinition<>("totalQuestions", "总题数", "score", true, c -> String.valueOf(c.getTotalQuestions())),
                new ColumnDefinition<>("totalScore", "试卷满分", "score", true, c -> String.valueOf(c.getTotalScore())),
                new ColumnDefinition<>("scoreObtained", "最终得分", "score", true, c -> String.valueOf(c.getScoreObtained())),
                new ColumnDefinition<>("accuracyRate", "正确率(%)", "score", true, c -> String.format(Locale.ROOT, "%.2f", c.getAccuracyRate())),
                new ColumnDefinition<>("submittedAt", "交卷时间", "score", true, c -> formatInstant(c.getSubmittedAt()))
        );
    }

    private List<ColumnDefinition<TheoryAnswerDetailExportVO>> getTheoryAnswerColumns() {
        return List.of(
                new ColumnDefinition<>("username", "考生账号", "detail", true, TheoryAnswerDetailExportVO::getUsername),
                new ColumnDefinition<>("realName", "考生姓名", "detail", true, TheoryAnswerDetailExportVO::getRealName),
                new ColumnDefinition<>("teamName", "战队名称", "detail", true, TheoryAnswerDetailExportVO::getTeamName),
                new ColumnDefinition<>("paperId", "考题序号", "detail", true, d -> String.valueOf(d.getPaperId())),
                new ColumnDefinition<>("type", "考题题型", "detail", true, TheoryAnswerDetailExportVO::getType),
                new ColumnDefinition<>("challengeName", "题干简述", "detail", true, d -> truncate(d.getChallengeName(), 60)),
                new ColumnDefinition<>("score", "单题满分", "detail", true, d -> String.valueOf(d.getScore())),
                new ColumnDefinition<>("userAnswer", "考生作答", "detail", true, TheoryAnswerDetailExportVO::getUserAnswer),
                new ColumnDefinition<>("standardAnswer", "标准答案", "detail", true, TheoryAnswerDetailExportVO::getStandardAnswer),
                new ColumnDefinition<>("isCorrect", "判定结果", "detail", true, d -> Boolean.TRUE.equals(d.getIsCorrect()) ? "正确" : "错误"),
                new ColumnDefinition<>("scoreObtained", "获得分值", "detail", true, d -> String.valueOf(d.getScoreObtained()))
        );
    }

    private String formatGender(Gender gender) {
        if (gender == null) return "";
        return switch (gender) {
            case MALE -> "男";
            case FEMALE -> "女";
            default -> "其他";
        };
    }

    private String formatRole(MemberRole role) {
        if (role == null) return "";
        return switch (role) {
            case CAPTAIN -> "队长";
            case LEADER -> "领队";
            case MEMBER -> "队员";
        };
    }

    private void writeJson(HttpServletResponse response, String fileName, Object data) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            setDownloadHeaders(response, fileName);
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("写出 JSON 导出文件失败: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "生成 JSON 导出文件失败");
        }
    }

    private void writeCsvSections(HttpServletResponse response, String fileName, List<CsvExportUtil.CsvSection> sections) {
        try {
            response.setContentType("text/csv;charset=UTF-8");
            setDownloadHeaders(response, fileName);
            CsvExportUtil.writeSections(response.getOutputStream(), sections);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("写出 CSV 导出文件失败: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "生成 CSV 导出文件失败");
        }
    }

    private void writeExcelSheets(HttpServletResponse response, String fileName, List<ExcelExportUtil.SheetData> sheets) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            setDownloadHeaders(response, fileName);
            ExcelExportUtil.writeSheets(response.getOutputStream(), sheets);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("写出 Excel 导出文件失败: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "生成 Excel 导出文件失败");
        }
    }

    private void setDownloadHeaders(HttpServletResponse response, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    private String sanitizeFileName(String title) {
        if (title == null || title.isBlank()) {
            return "competition";
        }
        return title.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "";
        return TIME_FORMATTER.format(instant);
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

}
