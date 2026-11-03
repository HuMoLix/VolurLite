package com.humolix.volur.verification.mail.provider;

import com.humolix.volur.verification.mail.bo.MailMessageBO;

public interface MailDeliveryProvider {

    String getProviderName();

    boolean isAvailable();

    void send(MailMessageBO message) throws Exception;

    void testConnection() throws Exception;

}
