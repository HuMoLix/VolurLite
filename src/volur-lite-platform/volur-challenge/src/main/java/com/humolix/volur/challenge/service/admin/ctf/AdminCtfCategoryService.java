package com.humolix.volur.challenge.service.admin.ctf;

import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;

import java.util.List;

public interface AdminCtfCategoryService {

    List<CtfCategoryVO> listCategories();

    CtfCategoryVO getCategory(Long id);

    Long createCategory(CtfCategoryCreateRequestDTO request);

    void updateCategory(Long id, CtfCategoryUpdateRequestDTO request);

    void deleteCategory(Long id);

}
