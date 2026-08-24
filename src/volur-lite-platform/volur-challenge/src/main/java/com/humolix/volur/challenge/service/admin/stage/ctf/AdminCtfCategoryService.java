package com.humolix.volur.challenge.service.admin.stage.ctf;

import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminCtfCategoryService {

    List<CtfChallengeCategoryResponseVO> listCategories();

    Page<CtfChallengeCategoryResponseVO> pageCategories(Integer page, Integer size, String keyword);

    CtfChallengeCategoryResponseVO createCategory(CtfChallengeCategoryCreateRequestDTO request);

    CtfChallengeCategoryResponseVO updateCategory(Long id, CtfChallengeCategoryUpdateRequestDTO request);

    void deleteCategory(Long id);

}
