package com.humolix.volur.competition.mapper;

import com.humolix.volur.competition.domain.entity.CompetitionChatMessage;
import com.humolix.volur.competition.domain.vo.chat.ChatMessageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompetitionChatMapper {

    @Mapping(source = "competition.id", target = "competitionId")
    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.teamName", target = "teamName")
    @Mapping(source = "sender.id", target = "senderId")
    ChatMessageVO toVO(CompetitionChatMessage message);

    List<ChatMessageVO> toVOList(List<CompetitionChatMessage> messages);

}
