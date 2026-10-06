package com.humolix.volur.security.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import com.humolix.volur.security.domain.dto.InitSessionRequestDTO;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.domain.vo.SessionVO;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.FlowCryptoService;
import com.humolix.volur.security.service.SessionService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.risk.DeviceEnvironmentMatrixEvaluator;
import com.humolix.volur.verification.risk.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final FlowCryptoService flowCryptoService;
    private final DeviceEnvironmentMatrixEvaluator deviceEnvironmentMatrixEvaluator;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public SessionVO initSess(InitSessionRequestDTO requestDTO) {
        FlowNames flow = FlowNames.validate(requestDTO.getFlowName(), requestDTO.getFlowEntry());
        String flowName = flow.getFlowName();
        String flowEntry = flow.findEntry(requestDTO.getFlowEntry()).getEntry();
        String fp = requestDTO.getFp();
        Long t = requestDTO.getT();
        String encryptedData = requestDTO.getData();

        String clientIp = RequestUtil.getClientIp();
        RequestHeader requestHeader = RequestHeader.fromCurrentRequest();

        RequestDeviceParams deviceParams;
        try {
            String decryptedJson = flowCryptoService.decrypt(encryptedData, flowName, flowEntry, null);
            deviceParams = objectMapper.readValue(decryptedJson, RequestDeviceParams.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Session] 设备参数解密或反序列化失败: flowName={}, flowEntry={}", flowName, flowEntry, e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "设备参数解密或解析失败: " + e.getMessage());
        }

        if (deviceParams == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "设备参数载荷不能为空");
        }

        Set<ConstraintViolation<RequestDeviceParams>> violations = validator.validate(deviceParams);
        if (!violations.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage());
        }

        deviceParams.setIp(clientIp);

        Session session = sessionManager.createSession(
                flowName, flowEntry, requestHeader, deviceParams
        );

        RiskLevel envRisk = deviceEnvironmentMatrixEvaluator.evaluate(deviceParams, requestHeader);
        session.setRiskLevel(envRisk.name());

        sessionManager.setMetadata(session.getDsh(), "fp", fp);
        sessionManager.setMetadata(session.getDsh(), "clientTimestamp", t);

        String initialTL = transitionLockManager.createTransLock(
                session.getDsh(),
                0,
                "init",
                null
        );
        session.getFlow().setActiveTL(initialTL);
        sessionManager.saveSession(session);

        log.info("[Session] 会话初始化完成: dsh={}, flowName={}, flowEntry={}, fp={}, ip={}",
                session.getDsh(), session.getFlow().getFlowName(), session.getFlow().getFlowEntry(), fp, clientIp);

        return SessionVO.builder()
                .dsh(session.getDsh())
                .tl(session.getFlow().getActiveTL())
                .flowName(session.getFlow().getFlowName())
                .flowEntry(session.getFlow().getFlowEntry())
                .cid(session.getFlow().getCid() != null ? session.getFlow().getCid() : 0)
                .fp(fp)
                .serverTime(System.currentTimeMillis())
                .build();
    }

}
