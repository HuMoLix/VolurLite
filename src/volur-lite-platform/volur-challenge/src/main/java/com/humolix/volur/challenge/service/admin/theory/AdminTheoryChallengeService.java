package com.humolix.volur.challenge.service.admin.theory;

import com.humolix.volur.challenge.domain.dto.theory.TheoryBankCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryBankUpdateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryBankListVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminTheoryChallengeService {

    PageResponse<TheoryBankListVO> listBanks(Integer page, Integer size, String keyword);

    TheoryBankDetailVO getBank(Long id);

    Long createBank(TheoryBankCreateRequestDTO request, Long authorId);

    void updateBank(Long id, TheoryBankUpdateRequestDTO request);

    void deleteBank(Long id);

    ChallengeImportResultVO importBanks(MultipartFile file, Long authorId);

    void exportBanks(List<Long> bankIds, HttpServletResponse response);

    PageResponse<TheoryChallengeListVO> listBankQuestions(Long bankId, Integer page, Integer size, TheoryType type, String category, String keyword);

    ChallengeImportResultVO importQuestionsToBank(Long bankId, MultipartFile file, Long authorId);

    void exportQuestionsByBank(Long bankId, List<Long> questionIds, HttpServletResponse response);

    PageResponse<TheoryChallengeListVO> listChallenges(Integer page, Integer size, TheoryType type, String category, String keyword);

    TheoryChallengeDetailVO getChallenge(Long id);

    Long createChallenge(TheoryChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, TheoryChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

    ChallengeImportResultVO importChallenges(MultipartFile file, Long authorId);

    void exportChallenges(List<Long> challengeIds, HttpServletResponse response);

}
