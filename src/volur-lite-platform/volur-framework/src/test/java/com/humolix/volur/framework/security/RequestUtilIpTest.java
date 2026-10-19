package com.humolix.volur.framework.security;

import com.humolix.volur.framework.common.util.RequestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtilIpTest {

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    public void testSingleProxyPublicIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "124.64.23.71");
        request.setRemoteAddr("172.18.0.1");
        setRequest(request);

        Assertions.assertEquals("124.64.23.71", RequestUtil.getRemoteIp());
    }

    @Test
    public void testMultiProxyFRPAndDockerNetwork() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 远程公网客户端 -> 远程 Nginx -> FRP -> 内网 Nginx -> Spring Boot
        request.addHeader("X-Forwarded-For", "124.64.23.71, 172.18.0.1");
        request.addHeader("X-Real-IP", "172.18.0.1"); // 即使被某级反代误写内网 IP
        request.setRemoteAddr("172.18.0.2");
        setRequest(request);

        // 必须准确回溯出公网真实客户端 124.64.23.71
        Assertions.assertEquals("124.64.23.71", RequestUtil.getRemoteIp());
    }

    @Test
    public void testAntiSpoofingRightToLeftTraversalBlocksFakeIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 攻击者伪造了 8.8.8.8，其真实公网 IP 为 203.0.113.88，经由多级反代追加
        request.addHeader("X-Forwarded-For", "8.8.8.8, 203.0.113.88, 172.18.0.1");
        request.setRemoteAddr("172.18.0.2");
        setRequest(request);

        // 自右向左标准回溯跳过 172.18.0.1 内部私网代理后，命中直连公网真实客户端 203.0.113.88，阻断伪造的 8.8.8.8
        Assertions.assertEquals("203.0.113.88", RequestUtil.getRemoteIp());
    }

    @Test
    public void testCloudflareConnectingIpPriority() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // CDN 拓扑下具备专用高信任度边缘客户端头
        request.addHeader("CF-Connecting-IP", "32.185.229.115");
        request.addHeader("X-Forwarded-For", "32.185.229.115, 104.28.1.1, 172.18.0.1");
        request.setRemoteAddr("172.18.0.1");
        setRequest(request);

        Assertions.assertEquals("32.185.229.115", RequestUtil.getRemoteIp());
    }

    @Test
    public void testTrueClientIpPriority() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("True-Client-IP", "114.119.153.91");
        request.addHeader("X-Forwarded-For", "114.119.153.91, 172.18.0.1");
        request.setRemoteAddr("172.18.0.1");
        setRequest(request);

        Assertions.assertEquals("114.119.153.91", RequestUtil.getRemoteIp());
    }

    @Test
    public void testPureInternalLANMultiPlayers() {
        // 线下 CTF 局域网对抗拓扑：选手 A (192.168.1.101) 和选手 B (192.168.1.102) 直连单机 docker-compose
        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.addHeader("X-Forwarded-For", "192.168.1.101, 172.18.0.1");
        requestA.setRemoteAddr("172.18.0.1");
        setRequest(requestA);
        Assertions.assertEquals("192.168.1.101", RequestUtil.getRemoteIp(), "选手 A 必须被独立识别为 192.168.1.101");

        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.addHeader("X-Forwarded-For", "192.168.1.102, 172.18.0.1");
        requestB.setRemoteAddr("172.18.0.1");
        setRequest(requestB);
        Assertions.assertEquals("192.168.1.102", RequestUtil.getRemoteIp(), "选手 B 必须被独立识别为 192.168.1.102");
    }

    @Test
    public void testLocalhostDirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        setRequest(request);

        Assertions.assertEquals("127.0.0.1", RequestUtil.getRemoteIp());
    }

}
