package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.InitSessionRequestDTO;
import com.humolix.volur.security.session.domain.vo.SessionVO;

public interface SessionService {

    SessionVO initSess(InitSessionRequestDTO requestDTO);

}
