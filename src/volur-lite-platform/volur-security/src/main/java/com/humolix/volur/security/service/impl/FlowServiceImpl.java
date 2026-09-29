package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.domain.vo.FlowInitVO;
import com.humolix.volur.security.service.FlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final TransitionLockManager transitionLockManager;

    @Override
    public FlowInitVO initSess(String flowName, String flowEntry, String fp, Long t, String dsh) {
        String clientIp = RequestUtil.getClientIp();
        String userAgent = RequestUtil.getUserAgent();

        String resolvedFlowName = StringUtils.hasText(flowName) ? flowName.trim() : "VlifWebLogin";
        String resolvedFlowEntry = StringUtils.hasText(flowEntry) ? flowEntry.trim() : "WebServiceLogin";

        AuthFlowSession session = transitionLockManager.createOrGetSession(
                dsh, resolvedFlowName, resolvedFlowEntry, clientIp, userAgent
        );

        if (StringUtils.hasText(fp)) {
            transitionLockManager.setMetadata(session.getDsh(), "fp", fp.trim());
        }
        if (t != null) {
            transitionLockManager.setMetadata(session.getDsh(), "clientTimestamp", t);
        }

        if (!StringUtils.hasText(session.getActiveTL())) {
            String initialTL = transitionLockManager.generateTL(
                    session.getDsh(),
                    session.getCid() != null ? session.getCid() : 0,
                    session.getCurrentStep() != null ? session.getCurrentStep() : "init",
                    null
            );
            session.setActiveTL(initialTL);
            transitionLockManager.saveSession(session);
        }

        log.info("[VLIF-Flow] 会话初始化完成: dsh={}, flowName={}, flowEntry={}, fp={}, ip={}",
                session.getDsh(), session.getFlowName(), session.getFlowEntry(), fp, clientIp);

        return FlowInitVO.builder()
                .dsh(session.getDsh())
                .tl(session.getActiveTL())
                .flowName(session.getFlowName())
                .flowEntry(session.getFlowEntry())
                .cid(session.getCid() != null ? session.getCid() : 0)
                .fp(fp)
                .serverTime(System.currentTimeMillis())
                .build();
    }

}
