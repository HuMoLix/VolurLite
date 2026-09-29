package com.humolix.volur.security.service;

import com.humolix.volur.security.domain.vo.FlowInitVO;

/**
 * VLIF (Volur Login Interface Framework) 全域状态机会话服务
 */
public interface FlowService {

    /**
     * 初始化或复用流程会话 (initSess)
     *
     * @param flowName  状态机大模块流名称 (如 VlifWebLogin, VlifWebSignUp, VlifWebRecovery)
     * @param flowEntry 大模块下的具体功能入口标识 (如 WebServiceLogin, WebSignUp, WebPasswordReset)
     * @param fp        客户端浏览器指纹 md5 摘要
     * @param t         客户端请求时间戳
     * @param dsh       已持有的分布式会话句柄 (可选)
     * @return 会话初始化视图 VO
     */
    FlowInitVO initSess(String flowName, String flowEntry, String fp, Long t, String dsh);

}
