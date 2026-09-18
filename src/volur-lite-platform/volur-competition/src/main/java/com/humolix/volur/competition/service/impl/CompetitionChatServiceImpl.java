package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.domain.dto.chat.ChatMessagePageQueryDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatMessageSendDTO;
import com.humolix.volur.competition.domain.dto.chat.ChatReadDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionChatMessage;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionTeam;
import com.humolix.volur.competition.domain.enums.ChatChannelType;
import com.humolix.volur.competition.domain.enums.ChatMessageType;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import com.humolix.volur.competition.domain.vo.chat.ChatUnreadCountVO;
import com.humolix.volur.competition.domain.vo.chat.RefereeTeamSummaryVO;
import com.humolix.volur.competition.mapper.CompetitionChatMapper;
import com.humolix.volur.competition.repository.CompetitionChatMessageRepository;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionTeamRepository;
import com.humolix.volur.competition.service.CompetitionChatService;
import com.humolix.volur.competition.websocket.model.CompetitionChatBroadcastMessage;
import com.humolix.volur.competition.websocket.redis.CompetitionChatRedisPublisher;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionChatServiceImpl implements CompetitionChatService {

    public static final String VREDIS_COMPETITION_CHAT_READ_KEY_PREFIX = "vredis:competition:chat:read:";
    public static final Duration CHAT_READ_TTL = Duration.ofDays(30);

    private final CompetitionChatMessageRepository chatMessageRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final CompetitionMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CompetitionChatMapper chatMapper;
    private final CompetitionChatRedisPublisher redisPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long competitionId, Long userId, ChatMessageSendDTO dto) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean isReferee = (sender.getRole() == UserRole.SUPER_ADMIN || sender.getRole() == UserRole.ADMIN);

        CompetitionTeam targetTeam;
        String senderRole;
        Long receiverId = null;
        String receiverName = null;
        String mentionStr = null;

        if (dto.getChannelType() == ChatChannelType.TEAM) {
            // 战队内部公屏交流
            if (isReferee) {
                if (dto.getTeamId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "裁判向队伍频道发送消息时必须指定目标队伍 ID");
                }
                targetTeam = teamRepository.findById(dto.getTeamId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的队伍不存在"));
                senderRole = "REFEREE";
            } else {
                CompetitionMember member = getAcceptedMember(competitionId, userId);
                if (member == null) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "未加入竞赛队伍，无法发送战队内部消息");
                }
                targetTeam = member.getTeam();
                senderRole = member.getRole().name();

                if (dto.getMentionUserIds() != null && !dto.getMentionUserIds().isEmpty()) {
                    // 队伍不能在队伍频道中 @ 裁判，严格排除管理员与裁判
                    List<Long> validMentionIds = dto.getMentionUserIds().stream()
                            .filter(id -> {
                                if (id == null) return false;
                                User u = userRepository.findById(id).orElse(null);
                                return u != null && u.getRole() != UserRole.ADMIN && u.getRole() != UserRole.SUPER_ADMIN;
                            })
                            .collect(Collectors.toList());
                    if (!validMentionIds.isEmpty()) {
                        mentionStr = validMentionIds.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","));
                    }
                }
            }

        } else if (dto.getChannelType() == ChatChannelType.PRIVATE) {
            // 队员点对点私信 / 裁判与参赛成员私信
            if (dto.getReceiverId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "发送私信时必须指定接收队员");
            }
            if (dto.getReceiverId().equals(userId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "无法向自己发送私信");
            }

            User receiver = userRepository.findById(dto.getReceiverId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的私信目标用户不存在"));

            if (isReferee) {
                if (dto.getTeamId() != null) {
                    targetTeam = teamRepository.findById(dto.getTeamId()).orElse(null);
                } else {
                    List<CompetitionMember> receiverMemberships = memberRepository.findMembersWithTeam(competitionId, receiver.getId());
                    targetTeam = receiverMemberships.stream()
                            .filter(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED)
                            .map(CompetitionMember::getTeam)
                            .findFirst()
                            .orElse(null);
                }
                if (targetTeam == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "目标成员未加入任何战队");
                }
                senderRole = "REFEREE";
            } else {
                CompetitionMember member = getAcceptedMember(competitionId, userId);
                if (member == null) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "未加入竞赛队伍，无法发送私信");
                }
                targetTeam = member.getTeam();
                senderRole = member.getRole().name();
            }

            receiverId = receiver.getId();
            receiverName = receiver.getUsername();

        } else if (dto.getChannelType() == ChatChannelType.REFEREE) {
            // 裁判工单通道
            if (isReferee) {
                if (dto.getTeamId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "裁判回复工单时必须指定目标队伍 ID");
                }
                targetTeam = teamRepository.findById(dto.getTeamId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的队伍不存在"));
                senderRole = "REFEREE";
            } else {
                CompetitionMember member = getAcceptedMember(competitionId, userId);
                if (member == null) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "未加入竞赛队伍，无法向裁判组发起工单");
                }
                targetTeam = member.getTeam();
                senderRole = member.getRole().name();
            }
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的聊天频道类型");
        }

        String avatarUrl = sender.getUserProfile() != null ? sender.getUserProfile().getAvatarUrl() : null;

        CompetitionChatMessage message = CompetitionChatMessage.builder()
                .competition(competition)
                .channelType(dto.getChannelType())
                .team(targetTeam)
                .sender(sender)
                .senderName(sender.getUsername())
                .senderRole(senderRole)
                .senderAvatar(avatarUrl)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .mentionUserIds(mentionStr)
                .content(dto.getContent().trim())
                .msgType(dto.getMsgType() != null ? dto.getMsgType() : ChatMessageType.TEXT)
                .build();

        CompetitionChatMessage saved = chatMessageRepository.save(message);
        ChatMessageVO vo = chatMapper.toVO(saved);

        // Redis 跨节点分布式广播
        CompetitionChatBroadcastMessage broadcastMessage = CompetitionChatBroadcastMessage.builder()
                .competitionId(competitionId)
                .channelType(dto.getChannelType())
                .teamId(targetTeam.getId())
                .senderId(userId)
                .receiverId(receiverId)
                .payload(vo)
                .build();

        redisPublisher.publish(broadcastMessage);

        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageVO> getHistoryMessages(Long competitionId, ChatMessagePageQueryDTO query, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean isReferee = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        int pageNum = Math.max(0, query.getPage() != null ? query.getPage() : 0);
        int pageSize = query.getSize() != null && query.getSize() > 0 ? Math.min(query.getSize(), 100) : 50;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        if (query.getChannelType() == ChatChannelType.PRIVATE) {
            // 私信历史记录
            Long targetUserId = query.getTargetUserId();
            if (targetUserId == null) {
                return PageResponse.empty();
            }
            Page<CompetitionChatMessage> page = chatMessageRepository.findDirectMessageHistory(
                    competitionId, userId, targetUserId, pageable);
            List<ChatMessageVO> voList = chatMapper.toVOList(page.getContent());
            return PageResponse.of(voList, page.getTotalElements(), page.getTotalPages(), page.getNumber() + 1);
        }

        Long targetTeamId;
        if (query.getChannelType() == ChatChannelType.TEAM) {
            if (isReferee) {
                targetTeamId = query.getTeamId();
                if (targetTeamId == null) {
                    return PageResponse.empty();
                }
            } else {
                CompetitionMember member = getAcceptedMember(competitionId, userId);
                if (member == null) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "未加入竞赛队伍，无法查看战队消息");
                }
                targetTeamId = member.getTeam().getId();
            }
        } else {
            if (isReferee) {
                targetTeamId = query.getTeamId();
                if (targetTeamId == null) {
                    return PageResponse.empty();
                }
            } else {
                CompetitionMember member = getAcceptedMember(competitionId, userId);
                if (member == null) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "未加入竞赛队伍，无法查看裁判工单记录");
                }
                targetTeamId = member.getTeam().getId();
            }
        }

        Page<CompetitionChatMessage> page = chatMessageRepository.findChannelHistory(
                competitionId, query.getChannelType(), targetTeamId, pageable);

        List<ChatMessageVO> voList = chatMapper.toVOList(page.getContent());
        return PageResponse.of(voList, page.getTotalElements(), page.getTotalPages(), page.getNumber() + 1);
    }

    @Override
    public void markRead(Long competitionId, Long userId, ChatReadDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTarget())) {
            return;
        }
        String redisKey = VREDIS_COMPETITION_CHAT_READ_KEY_PREFIX + competitionId + ":" + userId;
        Long lastId = dto.getLastMessageId();
        if (lastId == null || lastId <= 0) {
            lastId = System.currentTimeMillis();
        }
        stringRedisTemplate.opsForHash().put(redisKey, dto.getTarget(), String.valueOf(lastId));
        stringRedisTemplate.expire(redisKey, CHAT_READ_TTL);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatUnreadCountVO getUnreadCount(Long competitionId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean isReferee = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        String redisKey = VREDIS_COMPETITION_CHAT_READ_KEY_PREFIX + competitionId + ":" + userId;
        Map<Object, Object> readEntries = stringRedisTemplate.opsForHash().entries(redisKey);

        long teamUnread = 0L;
        long refereeUnread = 0L;
        long privateTotalUnread = 0L;
        Map<String, Long> privateDetails = new HashMap<>();

        CompetitionMember myMember = getAcceptedMember(competitionId, userId);

        if (myMember != null) {
            Long teamId = myMember.getTeam().getId();
            // 1. 战队公屏未读
            Long lastTeamReadId = parseReadId(readEntries.get("TEAM"));
            teamUnread = chatMessageRepository.countTeamUnread(
                    competitionId, teamId, userId, lastTeamReadId != null ? lastTeamReadId : 0L);

            // 2. 裁判工单未读 (选手端)
            Long lastRefereeReadId = parseReadId(readEntries.get("REFEREE"));
            refereeUnread = chatMessageRepository.countRefereeUnreadForTeam(
                    competitionId, teamId, lastRefereeReadId != null ? lastRefereeReadId : 0L);

            // 3. 私信未读 (统计同队队员发给当前用户的未读消息)
            List<CompetitionMember> teamMembers = memberRepository.findByTeamIdAndStatus(
                    teamId, MemberStatus.INVITED_ACCEPTED);
            for (CompetitionMember m : teamMembers) {
                if (m.getUser() != null && !m.getUser().getId().equals(userId)) {
                    Long otherUserId = m.getUser().getId();
                    String userKey = "USER_" + otherUserId;
                    Long lastPrivateReadId = parseReadId(readEntries.get(userKey));
                    long unreadForOther = chatMessageRepository.countPrivateUnreadBySender(
                            competitionId, userId, otherUserId, lastPrivateReadId != null ? lastPrivateReadId : 0L);
                    if (unreadForOther > 0) {
                        privateDetails.put(userKey, unreadForOther);
                        privateTotalUnread += unreadForOther;
                    }
                }
            }
        } else if (isReferee) {
            // 裁判端查看所有队伍的裁判工单未读
            Long lastRefereeReadId = parseReadId(readEntries.get("REFEREE"));
            refereeUnread = chatMessageRepository.countRefereeUnreadForAdmin(
                    competitionId, lastRefereeReadId != null ? lastRefereeReadId : 0L);
        }

        long total = teamUnread + refereeUnread + privateTotalUnread;

        return ChatUnreadCountVO.builder()
                .teamUnreadCount(teamUnread)
                .refereeUnreadCount(refereeUnread)
                .privateTotalUnreadCount(privateTotalUnread)
                .totalUnreadCount(total)
                .privateDetails(privateDetails)
                .build();
    }

    private Long parseReadId(Object val) {
        if (val == null) return null;
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefereeTeamSummaryVO> getRefereeTeamSummaries(Long competitionId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean isReferee = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);
        if (!isReferee) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "非管理员或裁判无权查阅全队工单概览");
        }

        List<CompetitionChatMessage> latestMessages = chatMessageRepository.findLatestRefereeMessagesByCompetition(competitionId);
        List<RefereeTeamSummaryVO> result = new ArrayList<>();

        for (CompetitionChatMessage msg : latestMessages) {
            if (msg.getTeam() == null) continue;
            long totalCount = chatMessageRepository.countByCompetitionIdAndChannelTypeAndTeamId(
                    competitionId, ChatChannelType.REFEREE, msg.getTeam().getId());

            RefereeTeamSummaryVO summary = RefereeTeamSummaryVO.builder()
                    .teamId(msg.getTeam().getId())
                    .teamName(msg.getTeam().getTeamName())
                    .teamAvatar(msg.getTeam().getAvatarUrl())
                    .lastSenderName(msg.getSenderName())
                    .lastSenderRole(msg.getSenderRole())
                    .lastContent(msg.getContent())
                    .lastCreatedAt(msg.getCreatedAt())
                    .totalMessages(totalCount)
                    .build();
            result.add(summary);
        }

        return result;
    }

    private CompetitionMember getAcceptedMember(Long competitionId, Long userId) {
        List<CompetitionMember> list = memberRepository.findMembersWithTeam(competitionId, userId);
        return list.stream()
                .filter(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED)
                .findFirst()
                .orElse(null);
    }

}
