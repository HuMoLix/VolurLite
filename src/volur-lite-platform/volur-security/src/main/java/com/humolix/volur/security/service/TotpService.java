package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.dto.TOTPChallengeRequestDTO;
import com.humolix.volur.security.domain.dto.TotpEnableRequestDTO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.domain.vo.TotpEnableResponseVO;
import com.humolix.volur.security.domain.vo.TotpSetupVO;
import com.humolix.volur.security.domain.vo.TotpStatusVO;

public interface TotpService {

    TotpSetupVO setupTotp(String dsh, String tl);

    TotpEnableResponseVO enableTotp(TotpEnableRequestDTO request);

    FlowResponseVO verifyTotp(TOTPChallengeRequestDTO request);

    TotpStatusVO getTotpStatus(Long userId);

    void disableTotp(Long userId);

}
