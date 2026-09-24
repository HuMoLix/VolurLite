package com.humolix.volur.verification.risk.event;

import org.springframework.context.ApplicationEvent;

public class RiskAutoBanEvent extends ApplicationEvent {

    private final String ip;
    private final String reason;

    public RiskAutoBanEvent(Object source, String ip, String reason) {
        super(source);
        this.ip = ip;
        this.reason = reason;
    }

    public String getIp() {
        return ip;
    }

    public String getReason() {
        return reason;
    }

}
