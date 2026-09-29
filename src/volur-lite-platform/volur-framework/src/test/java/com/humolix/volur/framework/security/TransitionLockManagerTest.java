package com.humolix.volur.framework.security;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.flow.AuthFlowSession;
import com.humolix.volur.framework.security.flow.TransitionLockManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class TransitionLockManagerTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TransitionLockManager transitionLockManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        transitionLockManager = new TransitionLockManager(redisTemplate);
    }

    @Test
    void generateDsh_shouldMatchGoogleAccountsFormat() {
        String dsh = transitionLockManager.generateDsh();
        Assertions.assertNotNull(dsh);
        Assertions.assertTrue(dsh.startsWith("S-"), "dsh 必须以 S- 前缀开头");
        Assertions.assertTrue(dsh.contains(":"), "dsh 必须包含冒号时间戳隔离符");
    }

    @Test
    void generateTL_shouldGenerateHmacSignatureToken() {
        String dsh = "S-969965009:1790527266429740";
        String tl = transitionLockManager.generateTL(dsh, 0, "identifier", "admin@ctf.local");
        Assertions.assertNotNull(tl);
        Assertions.assertTrue(tl.startsWith("ADG-"), "TL 流转票据必须以 ADG- 前缀开头");
        Assertions.assertTrue(tl.length() >= 64, "TL 票据应具备足够的抗暴力破解熵长度");
    }

    @Test
    void createOrGetSession_shouldCreateNewSessionAndSaveRedis() {
        String clientIp = "127.0.0.1";
        String ua = "Mozilla/5.0";

        AuthFlowSession session = transitionLockManager.createOrGetSession(null, clientIp, ua);

        Assertions.assertNotNull(session);
        Assertions.assertNotNull(session.getDsh());
        Assertions.assertEquals("init", session.getCurrentStep());
        Assertions.assertEquals(0, session.getCid());
        Assertions.assertEquals(clientIp, session.getClientIp());

        verify(valueOperations).set(eq("vredis:security:flow:session:" + session.getDsh()), any(), any(Duration.class));
    }

    @Test
    void advanceTransitionTicket_shouldIncrementCidAndRotateTL() {
        String dsh = "S-999999999:1700000000000000";
        String initialTL = transitionLockManager.generateTL(dsh, 0, "init", null);

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("identifier")
                .cid(0)
                .activeTL(initialTL)
                .build();

        String sessionJson = com.alibaba.fastjson2.JSON.toJSONString(session);
        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(sessionJson);

        String nextTL = transitionLockManager.advanceTransitionTicket(
                dsh, initialTL, 0, "identifier", 1, "recaptcha", "hacker@ctf.local"
        );

        Assertions.assertNotNull(nextTL);
        Assertions.assertNotEquals(initialTL, nextTL, "流转推进必须轮转票据 TL");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("vredis:security:flow:session:" + dsh), captor.capture(), any(Duration.class));

        AuthFlowSession saved = com.alibaba.fastjson2.JSON.parseObject(captor.getValue(), AuthFlowSession.class);
        Assertions.assertEquals(1, saved.getCid());
        Assertions.assertEquals("recaptcha", saved.getCurrentStep());
        Assertions.assertEquals("hacker@ctf.local", saved.getIdentifier());
        Assertions.assertEquals(nextTL, saved.getActiveTL());
    }

    @Test
    void validateTransitionTicket_shouldRejectExpiredOrSkippingSteps() {
        String dsh = "S-999999999:1700000000000000";
        String activeTL = transitionLockManager.generateTL(dsh, 1, "recaptcha", null);

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .currentStep("recaptcha")
                .cid(1)
                .activeTL(activeTL)
                .build();

        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(com.alibaba.fastjson2.JSON.toJSONString(session));

        // 1. 传递已作废或伪造的 TL -> 必须拦截
        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransitionTicket(dsh, "ADG-OLD_EXPIRED_TOKEN_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 1, "recaptcha")
        );

        // 2. 尝试跃迁跳步（例如在 recaptcha 阶段直接请求 pwd） -> 必须拦截
        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransitionTicket(dsh, activeTL, 1, "pwd")
        );
    }
}
