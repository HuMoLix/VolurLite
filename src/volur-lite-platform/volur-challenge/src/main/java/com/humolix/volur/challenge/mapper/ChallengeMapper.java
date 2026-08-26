package com.humolix.volur.challenge.mapper;

import com.humolix.volur.challenge.domain.entity.stage.awd.AwdChallenge;
import com.humolix.volur.challenge.domain.entity.stage.awdp.AwdPlusChallenge;
import com.humolix.volur.challenge.domain.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.entity.stage.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.entity.stage.isw.IswChallenge;
import com.humolix.volur.challenge.domain.entity.stage.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.vo.stage.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.awd.AwdChallengeListVO;
import com.humolix.volur.challenge.domain.vo.stage.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.isw.IswChallengeListVO;
import com.humolix.volur.challenge.domain.vo.stage.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.stage.theory.TheoryChallengeListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChallengeMapper {

    // --- CTF ---
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    CtfChallengeDetailVO toCtfDetailVO(CtfChallenge challenge);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "author.username", target = "authorName")
    CtfChallengeListVO toCtfListVO(CtfChallenge challenge);

    List<CtfChallengeListVO> toCtfListVOList(List<CtfChallenge> challenges);

    CtfCategoryVO toCtfCategoryVO(CtfChallengeCategory category);

    List<CtfCategoryVO> toCtfCategoryVOList(List<CtfChallengeCategory> categories);

    // --- THEORY ---
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.username", target = "authorName")
    TheoryChallengeDetailVO toTheoryDetailVO(TheoryChallenge challenge);

    @Mapping(source = "author.username", target = "authorName")
    TheoryChallengeListVO toTheoryListVO(TheoryChallenge challenge);

    List<TheoryChallengeListVO> toTheoryListVOList(List<TheoryChallenge> challenges);

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
