package com.humolix.volur.competition.mapper;

import com.humolix.volur.competition.domain.entity.*;
import com.humolix.volur.competition.domain.entity.stage.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.stage.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.vo.*;
import com.humolix.volur.competition.domain.vo.stage.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.stage.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperQuestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompetitionMapper {

    CompetitionSummaryVO toSummaryVO(Competition competition);

    List<CompetitionSummaryVO> toSummaryVOList(List<Competition> competitions);

    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.config.allowWriteupSubmission", target = "allowWriteupSubmission")
    @Mapping(source = "competition.stageConfigs", target = "stages")
    @Mapping(source = "competition.fieldSchemas", target = "registrationFields")
    CompetitionDetailVO toDetailVO(Competition competition);

    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.maxTeams", target = "maxTeams")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.config.freezeScoreboardMinutes", target = "freezeScoreboardMinutes")
    @Mapping(source = "competition.config.isScoreboardFrozen", target = "isScoreboardFrozen")
    @Mapping(source = "competition.config.allowWriteupSubmission", target = "allowWriteupSubmission")
    @Mapping(source = "competition.stageConfigs", target = "stages")
    @Mapping(source = "competition.fieldSchemas", target = "fieldSchemas")
    @Mapping(source = "competition.createdBy.id", target = "createdById")
    @Mapping(source = "competition.createdBy.username", target = "createdByName")
    AdminCompetitionDetailVO toAdminDetailVO(Competition competition);

    AdminCompetitionListVO toAdminListVO(Competition competition);

    List<AdminCompetitionListVO> toAdminListVOList(List<Competition> competitions);

    CompetitionStageVO toStageVO(CompetitionStageConfig stageConfig);

    List<CompetitionStageVO> toStageVOList(List<CompetitionStageConfig> stageConfigs);

    CompetitionFieldSchemaVO toFieldSchemaVO(CompetitionFieldSchema fieldSchema);

    List<CompetitionFieldSchemaVO> toFieldSchemaVOList(List<CompetitionFieldSchema> fieldSchemas);

    @Mapping(source = "competition.config.maxLeaders", target = "maxLeaders")
    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.maxTeams", target = "maxTeams")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.stageConfigs", target = "stages")
    @Mapping(source = "competition.fieldSchemas", target = "registrationFields")
    CompetitionAboutVO toAboutVO(Competition competition);

    @Mapping(source = "member.user.id", target = "userId")
    @Mapping(source = "member.user.username", target = "username")
    @Mapping(source = "member.user.userProfile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "member.user.userProfile.org", target = "org")
    @Mapping(source = "member.user.phone", target = "phone")
    @Mapping(source = "member.user.email", target = "email")
    CompetitionMemberVO toMemberVO(CompetitionMember member);

    List<CompetitionMemberVO> toMemberVOList(List<CompetitionMember> members);

    @Mapping(source = "team.competition.id", target = "competitionId")
    @Mapping(source = "team.captain.id", target = "captainId")
    @Mapping(source = "team.captain.username", target = "captainUsername")
    CompetitionTeamVO toTeamVO(CompetitionTeam team);

    List<CompetitionTeamVO> toTeamVOList(List<CompetitionTeam> teams);

    @Mapping(source = "team.competition.id", target = "competitionId")
    @Mapping(source = "team.captain.id", target = "captainId")
    @Mapping(source = "team.captain.username", target = "captainUsername")
    AdminTeamListVO toAdminTeamListVO(CompetitionTeam team);

    List<AdminTeamListVO> toAdminTeamListVOList(List<CompetitionTeam> teams);

    @Mapping(source = "c.id", target = "id")
    @Mapping(source = "c.challenge.id", target = "challengeId")
    @Mapping(source = "c.challenge.name", target = "name")
    @Mapping(source = "c.challenge.category.id", target = "categoryId")
    @Mapping(source = "c.challenge.category.name", target = "categoryName")
    @Mapping(source = "c.challenge.type", target = "type")
    @Mapping(source = "c.challenge.difficulty", target = "difficulty")
    @Mapping(source = "c.stage.id", target = "stageId")
    @Mapping(source = "c.stage.stageName", target = "stageName")
    CtfCompetitionChallengeVO toCtfChallengeVO(CtfCompetitionChallenge c);

    List<CtfCompetitionChallengeVO> toCtfChallengeVOList(List<CtfCompetitionChallenge> challenges);

    @Mapping(source = "c.id", target = "id")
    @Mapping(source = "c.challenge.id", target = "challengeId")
    @Mapping(source = "c.challenge.name", target = "name")
    @Mapping(source = "c.challenge.description", target = "description")
    @Mapping(source = "c.challenge.category.id", target = "categoryId")
    @Mapping(source = "c.challenge.category.name", target = "categoryName")
    @Mapping(source = "c.challenge.type", target = "type")
    @Mapping(source = "c.challenge.difficulty", target = "difficulty")
    @Mapping(source = "c.challenge.attachmentUrl", target = "attachmentUrl")
    @Mapping(source = "c.challenge.attachmentName", target = "attachmentName")
    @Mapping(source = "c.stage.id", target = "stageId")
    @Mapping(source = "c.stage.stageName", target = "stageName")
    CtfCompetitionChallengeDetailVO toCtfChallengeDetailVO(CtfCompetitionChallenge c);

    @Mapping(source = "p.id", target = "paperId")
    @Mapping(source = "p.challenge.id", target = "challengeId")
    @Mapping(source = "p.challenge.name", target = "name")
    @Mapping(source = "p.challenge.category", target = "category")
    @Mapping(source = "p.challenge.type", target = "type")
    @Mapping(source = "p.challenge.content", target = "content")
    @Mapping(source = "p.challenge.options", target = "options")
    TheoryPaperQuestionVO toTheoryPaperQuestionVO(TheoryCompetitionPaper p);

    List<TheoryPaperQuestionVO> toTheoryPaperQuestionVOList(List<TheoryCompetitionPaper> papers);

    @Mapping(source = "invitation.competition.id", target = "competitionId")
    @Mapping(source = "invitation.competition.title", target = "competitionTitle")
    @Mapping(source = "invitation.team.id", target = "teamId")
    @Mapping(source = "invitation.team.teamName", target = "teamName")
    @Mapping(source = "invitation.inviter.id", target = "inviterId")
    @Mapping(source = "invitation.inviter.username", target = "inviterUsername")
    @Mapping(source = "invitation.invitee.id", target = "inviteeId")
    @Mapping(source = "invitation.invitee.username", target = "inviteeUsername")
    CompetitionInvitationVO toInvitationVO(CompetitionTeamInvitation invitation);

    List<CompetitionInvitationVO> toInvitationVOList(List<CompetitionTeamInvitation> invitations);

}
