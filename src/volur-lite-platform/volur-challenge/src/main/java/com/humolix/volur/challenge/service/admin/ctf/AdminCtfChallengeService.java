package com.humolix.volur.challenge.service.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.framework.common.api.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminCtfChallengeService {

    PageResponse<CtfChallengeListVO> listChallenges(Integer page, Integer size, Long categoryId, String name);

    CtfChallengeDetailVO getChallenge(Long id);

    Long createChallenge(CtfChallengeCreateRequestDTO request, Long authorId);

    void updateChallenge(Long id, CtfChallengeUpdateRequestDTO request);

    void deleteChallenge(Long id);

    ChallengeImportResultVO importChallenge(MultipartFile file, Long authorId);

    default ChallengeImportResultVO importChallenges(MultipartFile file, Long authorId) {
        return importChallenge(file, authorId);
    }

    void downloadTemplate(HttpServletResponse response);

    void exportChallenges(List<Long> challengeIds, HttpServletResponse response);

    com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO uploadAttachment(MultipartFile file);

}
