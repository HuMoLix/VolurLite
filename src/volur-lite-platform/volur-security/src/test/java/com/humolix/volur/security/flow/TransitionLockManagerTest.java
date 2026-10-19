package com.humolix.volur.security.flow;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.flow.generator.TransitionLockGenerator;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class TransitionLockManagerTest {

    private SessionManager sessionManager;
    private TransitionLockManager transitionLockManager;

    @BeforeEach
    void setUp() {
        sessionManager = Mockito.mock(SessionManager.class);
        transitionLockManager = new TransitionLockManager(new TransitionLockGenerator(), sessionManager);
    }

    @Test
    void createTransLock_shouldGenerateHmacSignatureToken() {
        String dsh = "S-969965009:1790527266429740";
        String tl = transitionLockManager.createTransLock(dsh, 0, "identifier", "admin@ctf.local");
        Assertions.assertNotNull(tl);
        Assertions.assertTrue(tl.startsWith("ADG-"));
        Assertions.assertTrue(tl.length() >= 64);
    }

    @Test
    void nextTransLock_shouldValidateAndRotateToken() {
        String dsh = "S-100200300:1790000000";
        String initialTL = transitionLockManager.createTransLock(dsh, 0, "init", null);

        Flow flow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .activeTL(initialTL)
                .completedSteps(new ArrayList<>())
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);

        String nextTL = transitionLockManager.nextTransLock(
                dsh, initialTL, 0, "init", 1, "recaptcha", "user@ctf.local"
        );

        Assertions.assertNotNull(nextTL);
        Assertions.assertNotEquals(initialTL, nextTL);
        Assertions.assertTrue(nextTL.startsWith("ADG-"));
        Assertions.assertEquals(1, flow.getCid());
        Assertions.assertEquals("recaptcha", flow.getCurrentStep());
        Assertions.assertEquals("user@ctf.local", flow.getIdentifier());
        Assertions.assertEquals(nextTL, flow.getActiveTL());
        Assertions.assertTrue(flow.getCompletedSteps().contains("init"));

        verify(sessionManager).saveSession(session);
    }

    @Test
    void validateTransLock_shouldThrowWhenTamperedOrSkipped() {
        String dsh = "S-100200300:1790000000";
        String activeTL = transitionLockManager.createTransLock(dsh, 1, "recaptcha", null);

        Flow flow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("recaptcha")
                .cid(1)
                .activeTL(activeTL)
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransLock(dsh, "ADG-OLD_EXPIRED_TOKEN_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 1, "recaptcha")
        );

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransLock(dsh, activeTL, 1, "pwd")
        );

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransLock(dsh, activeTL, 2, "recaptcha")
        );

        Assertions.assertDoesNotThrow(() ->
                transitionLockManager.validateTransLock(dsh, activeTL, "recaptcha")
        );

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransLock(dsh, activeTL, "pwd")
        );
    }

    @Test
    void burnAndCompleteSession_shouldValidateAndDestroy() {
        String dsh = "S-100200300:1790000000";
        String activeTL = transitionLockManager.createTransLock(dsh, 2, "pwd", null);

        Flow flow = Flow.builder()
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("pwd")
                .cid(2)
                .activeTL(activeTL)
                .build();

        Session session = Session.builder()
                .dsh(dsh)
                .flow(flow)
                .build();

        given(sessionManager.getSession(dsh)).willReturn(session);

        transitionLockManager.burnAndCompleteSession(dsh, activeTL, 2, "pwd");

        verify(sessionManager).destroySession(dsh);
    }

}
