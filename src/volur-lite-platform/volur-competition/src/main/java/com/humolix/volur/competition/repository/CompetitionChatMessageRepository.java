package com.humolix.volur.competition.repository;

import com.humolix.volur.competition.domain.entity.CompetitionChatMessage;
import com.humolix.volur.competition.domain.enums.ChatChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionChatMessageRepository extends JpaRepository<CompetitionChatMessage, Long> {

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = :channelType " +
            "AND m.team.id = :teamId " +
            "ORDER BY m.id DESC")
    Page<CompetitionChatMessage> findChannelHistory(
            @Param("competitionId") Long competitionId,
            @Param("channelType") ChatChannelType channelType,
            @Param("teamId") Long teamId,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.PRIVATE " +
            "AND ((m.sender.id = :userId1 AND m.receiverId = :userId2) OR (m.sender.id = :userId2 AND m.receiverId = :userId1)) " +
            "ORDER BY m.id DESC")
    Page<CompetitionChatMessage> findDirectMessageHistory(
            @Param("competitionId") Long competitionId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = :channelType " +
            "AND m.team.id = :teamId " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findLatestChannelMessages(
            @Param("competitionId") Long competitionId,
            @Param("channelType") ChatChannelType channelType,
            @Param("teamId") Long teamId,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = :channelType " +
            "AND m.team.id = :teamId " +
            "AND m.id < :beforeId " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findEarlierChannelMessages(
            @Param("competitionId") Long competitionId,
            @Param("channelType") ChatChannelType channelType,
            @Param("teamId") Long teamId,
            @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.PRIVATE " +
            "AND ((m.sender.id = :userId1 AND m.receiverId = :userId2) OR (m.sender.id = :userId2 AND m.receiverId = :userId1)) " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findLatestDirectMessages(
            @Param("competitionId") Long competitionId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.PRIVATE " +
            "AND ((m.sender.id = :userId1 AND m.receiverId = :userId2) OR (m.sender.id = :userId2 AND m.receiverId = :userId1)) " +
            "AND m.id < :beforeId " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findEarlierDirectMessages(
            @Param("competitionId") Long competitionId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = :channelType " +
            "AND m.team.id = :teamId " +
            "AND m.id < :beforeId " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findEarlierMessages(
            @Param("competitionId") Long competitionId,
            @Param("channelType") ChatChannelType channelType,
            @Param("teamId") Long teamId,
            @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("SELECT m FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE " +
            "AND m.id IN (" +
            "  SELECT MAX(m2.id) FROM CompetitionChatMessage m2 " +
            "  WHERE m2.competition.id = :competitionId " +
            "  AND m2.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE " +
            "  GROUP BY m2.team.id" +
            ") " +
            "ORDER BY m.id DESC")
    List<CompetitionChatMessage> findLatestRefereeMessagesByCompetition(@Param("competitionId") Long competitionId);

    long countByCompetitionIdAndChannelTypeAndTeamId(
            Long competitionId,
            ChatChannelType channelType,
            Long teamId);

    @Query("SELECT COUNT(m) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.TEAM " +
            "AND m.team.id = :teamId " +
            "AND m.sender.id != :userId " +
            "AND m.id > :lastReadId")
    long countTeamUnread(
            @Param("competitionId") Long competitionId,
            @Param("teamId") Long teamId,
            @Param("userId") Long userId,
            @Param("lastReadId") Long lastReadId);

    @Query("SELECT COUNT(m) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE " +
            "AND m.team.id = :teamId " +
            "AND m.senderRole = 'REFEREE' " +
            "AND m.id > :lastReadId")
    long countRefereeUnreadForTeam(
            @Param("competitionId") Long competitionId,
            @Param("teamId") Long teamId,
            @Param("lastReadId") Long lastReadId);

    @Query("SELECT COUNT(m) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE " +
            "AND m.senderRole != 'REFEREE' " +
            "AND m.id > :lastReadId")
    long countRefereeUnreadForAdmin(
            @Param("competitionId") Long competitionId,
            @Param("lastReadId") Long lastReadId);

    @Query("SELECT COUNT(m) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.PRIVATE " +
            "AND m.receiverId = :userId " +
            "AND m.sender.id = :senderId " +
            "AND m.id > :lastReadId")
    long countPrivateUnreadBySender(
            @Param("competitionId") Long competitionId,
            @Param("userId") Long userId,
            @Param("senderId") Long senderId,
            @Param("lastReadId") Long lastReadId);

    @Query("SELECT MAX(m.id) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.TEAM " +
            "AND m.team.id = :teamId")
    Long findMaxTeamMessageId(
            @Param("competitionId") Long competitionId,
            @Param("teamId") Long teamId);

    @Query("SELECT MAX(m.id) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE " +
            "AND m.team.id = :teamId")
    Long findMaxRefereeMessageIdForTeam(
            @Param("competitionId") Long competitionId,
            @Param("teamId") Long teamId);

    @Query("SELECT MAX(m.id) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.REFEREE")
    Long findMaxRefereeMessageIdForAdmin(
            @Param("competitionId") Long competitionId);

    @Query("SELECT MAX(m.id) FROM CompetitionChatMessage m " +
            "WHERE m.competition.id = :competitionId " +
            "AND m.channelType = com.humolix.volur.competition.domain.enums.ChatChannelType.PRIVATE " +
            "AND ((m.sender.id = :userId AND m.receiverId = :otherUserId) OR (m.sender.id = :otherUserId AND m.receiverId = :userId))")
    Long findMaxPrivateMessageId(
            @Param("competitionId") Long competitionId,
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM CompetitionChatMessage m WHERE m.team.id = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);

}

