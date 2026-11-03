package com.humolix.volur.competition.mapper;

import com.humolix.volur.competition.domain.entity.*;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionPaper;
import com.humolix.volur.competition.domain.vo.*;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeDetailVO;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperQuestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompetitionMapper {

    CompetitionSummaryVO toSummaryVO(Competition competition);

    List<CompetitionSummaryVO> toSummaryVOList(List<Competition> competitions);

    @Mapping(source = "competition.config.maxLeaders", target = "maxLeaders")
    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.config.allowWriteupSubmission", target = "allowWriteupSubmission")
    @Mapping(source = "competition.config.regRequirements", target = "regRequirements")
    CompetitionDetailVO toDetailVO(Competition competition);

    @Mapping(source = "competition.config.maxLeaders", target = "maxLeaders")
    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.maxTeams", target = "maxTeams")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.config.freezeScoreboardMinutes", target = "freezeScoreboardMinutes")
    @Mapping(source = "competition.config.isScoreboardFrozen", target = "isScoreboardFrozen")
    @Mapping(source = "competition.config.allowWriteupSubmission", target = "allowWriteupSubmission")
    @Mapping(source = "competition.config.maxContainersPerTeam", target = "maxContainersPerTeam")
    @Mapping(source = "competition.config.writeupTemplateUrl", target = "writeupTemplateUrl")
    @Mapping(source = "competition.config.writeupTemplateName", target = "writeupTemplateName")
    @Mapping(source = "competition.config.writeupStartTime", target = "writeupStartTime")
    @Mapping(source = "competition.config.writeupEndTime", target = "writeupEndTime")
    @Mapping(source = "competition.config.regRequirements", target = "regRequirements")
    @Mapping(source = "competition.config.enableBloodBonus", target = "enableBloodBonus")
    @Mapping(source = "competition.config.firstBloodBonusPercentage", target = "firstBloodBonusPercentage")
    @Mapping(source = "competition.config.secondBloodBonusPercentage", target = "secondBloodBonusPercentage")
    @Mapping(source = "competition.config.thirdBloodBonusPercentage", target = "thirdBloodBonusPercentage")
    @Mapping(source = "competition.createdBy.id", target = "createdById")
    @Mapping(source = "competition.createdBy.username", target = "createdByName")
    AdminCompetitionDetailVO toAdminDetailVO(Competition competition);

    AdminCompetitionListVO toAdminListVO(Competition competition);

    List<AdminCompetitionListVO> toAdminListVOList(List<Competition> competitions);

    @Mapping(source = "competition.config.maxLeaders", target = "maxLeaders")
    @Mapping(source = "competition.config.maxTeamMembers", target = "maxTeamMembers")
    @Mapping(source = "competition.config.minTeamMembers", target = "minTeamMembers")
    @Mapping(source = "competition.config.maxTeams", target = "maxTeams")
    @Mapping(source = "competition.config.requireTeamAudit", target = "requireTeamAudit")
    @Mapping(source = "competition.config.regRequirements", target = "regRequirements")
    CompetitionAboutVO toAboutVO(Competition competition);

    @Mapping(source = "member.user.id", target = "userId")
    @Mapping(source = "member.user.username", target = "username")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(source = "member.realName", target = "realName")
    @Mapping(source = "member.gender", target = "gender")
    @Mapping(source = "member.phone", target = "phone")
    @Mapping(source = "member.email", target = "email")
    @Mapping(source = "member.idCard", target = "idCard")
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
    CtfCompetitionChallengeDetailVO toCtfChallengeDetailVO(CtfCompetitionChallenge c);

    @Mapping(source = "p.id", target = "paperId")
    @Mapping(source = "p.challenge.id", target = "challengeId")
    @Mapping(source = "p.challenge.name", target = "name")
    @Mapping(source = "p.challenge.category", target = "category")
    @Mapping(source = "p.challenge.type", target = "type")
    @Mapping(source = "p.challenge.content", target = "content")
    @Mapping(target = "options", expression = "java(p.getShuffledOptions() != null ? p.getShuffledOptions() : (p.getChallenge() != null ? p.getChallenge().getOptions() : null))")
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

    CompetitionAnnouncementVO toAnnouncementVO(CompetitionAnnouncement announcement);

    List<CompetitionAnnouncementVO> toAnnouncementVOList(List<CompetitionAnnouncement> announcements);

}
