package com.humolix.volur.challenge.service.admin.stage.ctf;

import com.humolix.volur.challenge.domain.dto.stage.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.stage.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.stage.ctf.CtfCategoryVO;

import java.util.List;

public interface AdminCtfCategoryService {

    List<CtfCategoryVO> listCategories();

    CtfCategoryVO getCategory(Long id);

    Long createCategory(CtfCategoryCreateRequestDTO request);

    void updateCategory(Long id, CtfCategoryUpdateRequestDTO request);

    void deleteCategory(Long id);

}
