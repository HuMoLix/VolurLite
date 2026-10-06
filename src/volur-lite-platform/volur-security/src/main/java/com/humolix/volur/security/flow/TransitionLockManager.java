package com.humolix.volur.security.flow;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.generator.TransitionLockGenerator;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionLockManager {

    private final TransitionLockGenerator transitionLockGenerator;
    private final SessionManager sessionManager;

    public String createTransLock(String dsh, int cid, String step, String identifier) {
        return transitionLockGenerator.generate(dsh, cid, step, identifier);
    }

    public void validateTransLock(String dsh, String tl, int cid, String expectedStep) {
        Session session = sessionManager.getSession(dsh);
        Flow flow = session.getFlow();
        if (flow == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "会话流程状态机不存在");
        }

        if (!transitionLockGenerator.isValid(tl)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证流转票据 (TL) 缺失或格式非法");
        }

        if (!Objects.equals(flow.getActiveTL(), tl)) {
            log.warn("[TransitionLockManager] TL 票据不匹配或已被核销: dsh={}, reqTL={}, activeTL={}", dsh, tl, flow.getActiveTL());
            throw new BusinessException(HttpStatus.FORBIDDEN, "流转票据 (TL) 无效或已被核销，防止重放攻击");
        }

        if (!Objects.equals(flow.getCurrentStep(), expectedStep)) {
            log.warn("[TransitionLockManager] 检测到跃迁跳步: dsh={}, expectedStep={}, sessionStep={}", dsh, expectedStep, flow.getCurrentStep());
            throw new BusinessException(HttpStatus.FORBIDDEN, "检测到非法越级跳转 (Step Skipping)，请按顺序完成验证");
        }

        if (!Objects.equals(flow.getCid(), cid)) {
            log.warn("[TransitionLockManager] cid 序号不匹配: dsh={}, reqCid={}, sessionCid={}", dsh, cid, flow.getCid());
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证步骤序号 (cid) 不一致，流程已被重置");
        }
    }

    public void validateTransLock(String dsh, String tl, String expectedStep) {
        Session session = sessionManager.getSession(dsh);
        Flow flow = session.getFlow();
        if (flow == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "会话流程状态机不存在");
        }

        if (!transitionLockGenerator.isValid(tl)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证流转票据 (TL) 缺失或格式非法");
        }

        if (!Objects.equals(flow.getActiveTL(), tl)) {
            log.warn("[TransitionLockManager] TL 票据不匹配或已被核销: dsh={}, reqTL={}, activeTL={}", dsh, tl, flow.getActiveTL());
            throw new BusinessException(HttpStatus.FORBIDDEN, "流转票据 (TL) 无效或已被核销，防止重放攻击");
        }

        if (StringUtils.hasText(expectedStep) && FlowNames.VLIF_WEB_LOGIN.getFlowName().equals(flow.getFlowName())) {
            if (!Objects.equals(flow.getCurrentStep(), expectedStep)) {
                log.warn("[TransitionLockManager] 步骤不匹配: dsh={}, expectedStep={}, sessionStep={}", dsh, expectedStep, flow.getCurrentStep());
                throw new BusinessException(HttpStatus.FORBIDDEN, "检测到非法越级跳转，请按顺序完成验证");
            }
        }
    }

    public String nextTransLock(String dsh, String currentTl, int currentCid, String completedStep, int nextCid, String nextStep, String identifier) {
        Session session = sessionManager.getSession(dsh);
        Flow flow = session.getFlow();
        if (flow == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "会话流程状态机不存在");
        }

        if (StringUtils.hasText(currentTl)) {
            validateTransLock(dsh, currentTl, currentCid, completedStep);
            flow.completeStep(completedStep);
        }

        String nextTL = createTransLock(dsh, nextCid, nextStep, identifier);
        flow.setActiveTL(nextTL);
        flow.setCid(nextCid);
        flow.setCurrentStep(nextStep);
        if (StringUtils.hasText(identifier)) {
            flow.setIdentifier(identifier);
        }
        sessionManager.saveSession(session);

        log.info("[TransitionLockManager] 推进状态机步骤: flow={}, dsh={}, step: {} -> {}, cid: {} -> {}",
                flow.getFlowName(), dsh, completedStep, nextStep, currentCid, nextCid);
        return nextTL;
    }

    public void burnAndCompleteSession(String dsh, String currentTl, int currentCid, String expectedStep) {
        validateTransLock(dsh, currentTl, currentCid, expectedStep);
        sessionManager.destroySession(dsh);
        log.info("[TransitionLockManager] 流程认证完成，已销毁会话与事务锁: dsh={}", dsh);
    }

}
