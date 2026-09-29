package com.humolix.volur.security.flow;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionLockManager {

    private static final String VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX = "vredis:security:flow:session:";
    private static final Duration FLOW_SESSION_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final DshGenerator dshGenerator;
    private final TransitionLockGenerator transitionLockGenerator;

    public String generateDsh() {
        return dshGenerator.generate();
    }

    public String generateTL(String dsh, int cid, String step, String identifier) {
        return transitionLockGenerator.generate(dsh, cid, step, identifier);
    }

    public AuthFlowSession createOrGetSession(String dsh, String flowName, String flowEntry, String clientIp, String userAgent) {
        if (StringUtils.hasText(dsh)) {
            AuthFlowSession existing = getSessionOrNull(dsh);
            if (existing != null) {
                boolean modified = false;
                if (StringUtils.hasText(flowName) && !Objects.equals(existing.getFlowName(), flowName)) {
                    existing.setFlowName(flowName);
                    modified = true;
                }
                if (StringUtils.hasText(flowEntry) && !Objects.equals(existing.getFlowEntry(), flowEntry)) {
                    existing.setFlowEntry(flowEntry);
                    modified = true;
                }
                if (StringUtils.hasText(clientIp) && !Objects.equals(existing.getClientIp(), clientIp)) {
                    existing.setClientIp(clientIp);
                    modified = true;
                }
                if (StringUtils.hasText(userAgent) && !Objects.equals(existing.getUserAgent(), userAgent)) {
                    existing.setUserAgent(userAgent);
                    modified = true;
                }
                if (modified) {
                    saveSession(existing);
                }
                return existing;
            }
        }

        String assignedDsh = StringUtils.hasText(dsh) ? dsh.trim() : generateDsh();
        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(assignedDsh)
                .flowName(StringUtils.hasText(flowName) ? flowName.trim() : "VlifWebLogin")
                .flowEntry(StringUtils.hasText(flowEntry) ? flowEntry.trim() : "WebServiceLogin")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .currentStep("init")
                .cid(0)
                .completedSteps(new ArrayList<>())
                .metadata(new HashMap<>())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        saveSession(session);
        return session;
    }

    public AuthFlowSession createOrGetSession(String dsh, String clientIp, String userAgent) {
        return createOrGetSession(dsh, "VlifWebLogin", "WebServiceLogin", clientIp, userAgent);
    }

    public AuthFlowSession getSession(String dsh) {
        AuthFlowSession session = getSessionOrNull(dsh);
        if (session == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录或业务流程会话句柄 (dsh) 已失效或不存在，请重新开始");
        }
        return session;
    }

    public AuthFlowSession getSessionOrNull(String dsh) {
        if (!StringUtils.hasText(dsh)) {
            return null;
        }
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, AuthFlowSession.class);
        } catch (Exception e) {
            log.error("[TransitionLockManager] 解析会话 JSON 失败, dsh={}", dsh, e);
            return null;
        }
    }

    public void saveSession(AuthFlowSession session) {
        if (session == null || !StringUtils.hasText(session.getDsh())) {
            return;
        }
        session.setUpdatedAt(System.currentTimeMillis());
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + session.getDsh().trim();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(session), FLOW_SESSION_TTL);
    }

    public void validateTransitionTicket(String dsh, String tl, int cid, String expectedStep) {
        AuthFlowSession session = getSession(dsh);

        if (!StringUtils.hasText(tl) || !tl.startsWith("ADG-")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证流转票据 (TL) 缺失或格式非法");
        }

        if (!Objects.equals(session.getActiveTL(), tl)) {
            log.warn("[TransitionLockManager] TL 票据不匹配或已被核销: dsh={}, reqTL={}, activeTL={}", dsh, tl, session.getActiveTL());
            throw new BusinessException(HttpStatus.FORBIDDEN, "流转票据 (TL) 无效或已被核销，防止重放攻击");
        }

        if (!Objects.equals(session.getCurrentStep(), expectedStep)) {
            log.warn("[TransitionLockManager] 检测到跃迁跳步: dsh={}, expectedStep={}, sessionStep={}", dsh, expectedStep, session.getCurrentStep());
            throw new BusinessException(HttpStatus.FORBIDDEN, "检测到非法越级跳转 (Step Skipping)，请按顺序完成验证");
        }

        if (!Objects.equals(session.getCid(), cid)) {
            log.warn("[TransitionLockManager] cid 序号不匹配: dsh={}, reqCid={}, sessionCid={}", dsh, cid, session.getCid());
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证步骤序号 (cid) 不一致，流程已被重置");
        }
    }

    public void validateStepAndTl(String dsh, String tl, String expectedStep) {
        AuthFlowSession session = getSession(dsh);

        if (!StringUtils.hasText(tl) || !tl.startsWith("ADG-")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证流转票据 (TL) 缺失或格式非法");
        }

        if (!Objects.equals(session.getActiveTL(), tl)) {
            log.warn("[TransitionLockManager] TL 票据不匹配或已被核销: dsh={}, reqTL={}, activeTL={}", dsh, tl, session.getActiveTL());
            throw new BusinessException(HttpStatus.FORBIDDEN, "流转票据 (TL) 无效或已被核销，防止重放攻击");
        }

        if (StringUtils.hasText(expectedStep) && "VlifWebLogin".equals(session.getFlowName())) {
            if (!Objects.equals(session.getCurrentStep(), expectedStep)) {
                log.warn("[TransitionLockManager] 步骤不匹配: dsh={}, expectedStep={}, sessionStep={}", dsh, expectedStep, session.getCurrentStep());
                throw new BusinessException(HttpStatus.FORBIDDEN, "检测到非法越级跳转，请按顺序完成验证");
            }
        }
    }

    public String advanceTransitionTicket(String dsh, String currentTl, int currentCid, String completedStep, int nextCid, String nextStep, String identifier) {
        AuthFlowSession session = getSession(dsh);

        if (StringUtils.hasText(currentTl)) {
            validateTransitionTicket(dsh, currentTl, currentCid, completedStep);
            if (session.getCompletedSteps() == null) {
                session.setCompletedSteps(new ArrayList<>());
            }
            session.getCompletedSteps().add(completedStep);
        }

        String nextTL = generateTL(dsh, nextCid, nextStep, identifier);
        session.setActiveTL(nextTL);
        session.setCid(nextCid);
        session.setCurrentStep(nextStep);
        if (StringUtils.hasText(identifier)) {
            session.setIdentifier(identifier);
        }
        saveSession(session);

        log.info("[TransitionLockManager] 推进状态机步骤: flow={}, dsh={}, step: {} -> {}, cid: {} -> {}",
                session.getFlowName(), dsh, completedStep, nextStep, currentCid, nextCid);
        return nextTL;
    }

    public void setMetadata(String dsh, String key, Object value) {
        AuthFlowSession session = getSession(dsh);
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put(key, value);
        saveSession(session);
    }

    public Object getMetadata(String dsh, String key) {
        AuthFlowSession session = getSessionOrNull(dsh);
        if (session == null || session.getMetadata() == null) {
            return null;
        }
        return session.getMetadata().get(key);
    }

    public void burnAndCompleteSession(String dsh, String currentTl, int currentCid, String expectedStep) {
        validateTransitionTicket(dsh, currentTl, currentCid, expectedStep);
        destroySession(dsh);
        log.info("[TransitionLockManager] 流程认证完成，已销毁会话与事务锁: dsh={}", dsh);
    }

    public void destroySession(String dsh) {
        if (StringUtils.hasText(dsh)) {
            String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
            redisTemplate.delete(key);
        }
    }

}
