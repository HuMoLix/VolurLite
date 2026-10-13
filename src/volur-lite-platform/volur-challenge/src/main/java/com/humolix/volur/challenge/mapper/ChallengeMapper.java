package com.humolix.volur.challenge.mapper;

import com.humolix.volur.framework.common.util.JsonUtils;
import com.humolix.volur.challenge.domain.entity.awd.AwdChallenge;
import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.entity.isw.IswChallenge;
import com.humolix.volur.challenge.domain.entity.theory.TheoryBank;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeListVO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeListVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankListVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import org.mapstruct.*;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChallengeMapper {

    // --- CTF ---
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.icon", target = "categoryIcon")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    @Mapping(target = "attachments", ignore = true)
    CtfChallengeDetailVO toCtfDetailVO(CtfChallenge challenge);

    @AfterMapping
    default void mapCtfAttachments(CtfChallenge challenge, @MappingTarget CtfChallengeDetailVO vo) {
        if (challenge == null || vo == null) return;
        List<CtfAttachmentVO> list = null;
        if (StringUtils.hasText(challenge.getAttachments())) {
            try {
                list = JsonUtils.parseList(challenge.getAttachments(), CtfAttachmentVO.class);
            } catch (Exception ignored) {}
        }
        if ((list == null || list.isEmpty()) && StringUtils.hasText(challenge.getAttachmentUrl())) {
            list = List.of(CtfAttachmentVO.builder()
                    .name(challenge.getAttachmentName() != null ? challenge.getAttachmentName() : "附件文件")
                    .url(challenge.getAttachmentUrl())
                    .build());
        }
        vo.setAttachments(list != null ? list : Collections.emptyList());
    }

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.icon", target = "categoryIcon")
    @Mapping(source = "author.username", target = "authorName")
    CtfChallengeListVO toCtfListVO(CtfChallenge challenge);

    List<CtfChallengeListVO> toCtfListVOList(List<CtfChallenge> challenges);

    CtfCategoryVO toCtfCategoryVO(CtfChallengeCategory category);

    List<CtfCategoryVO> toCtfCategoryVOList(List<CtfChallengeCategory> categories);

    // --- THEORY ---
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    @Mapping(source = "bank.id", target = "bankId")
    @Mapping(source = "bank.name", target = "bankName")
    TheoryChallengeDetailVO toTheoryDetailVO(TheoryChallenge challenge);

    @Mapping(source = "author.username", target = "authorName")
    @Mapping(source = "bank.id", target = "bankId")
    @Mapping(source = "bank.name", target = "bankName")
    TheoryChallengeListVO toTheoryListVO(TheoryChallenge challenge);

    List<TheoryChallengeListVO> toTheoryListVOList(List<TheoryChallenge> challenges);

    @Mapping(source = "author.username", target = "authorName")
    @Mapping(target = "questionCount", ignore = true)
    TheoryBankListVO toTheoryBankListVO(TheoryBank bank);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    @Mapping(target = "questionCount", ignore = true)
    TheoryBankDetailVO toTheoryBankDetailVO(TheoryBank bank);

    // --- AWD ---
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    AwdChallengeDetailVO toAwdDetailVO(AwdChallenge challenge);

    @Mapping(source = "author.username", target = "authorName")
    AwdChallengeListVO toAwdListVO(AwdChallenge challenge);

    List<AwdChallengeListVO> toAwdListVOList(List<AwdChallenge> challenges);

    // --- AWDP ---
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    AwdPlusChallengeDetailVO toAwdPlusDetailVO(AwdPlusChallenge challenge);

    @Mapping(source = "author.username", target = "authorName")
    AwdPlusChallengeListVO toAwdPlusListVO(AwdPlusChallenge challenge);

    List<AwdPlusChallengeListVO> toAwdPlusListVOList(List<AwdPlusChallenge> challenges);

    // --- ISW ---
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    IswChallengeDetailVO toIswDetailVO(IswChallenge challenge);

    @Mapping(source = "author.username", target = "authorName")
    IswChallengeListVO toIswListVO(IswChallenge challenge);

    List<IswChallengeListVO> toIswListVOList(List<IswChallenge> challenges);

}
