package com.humolix.volur.framework.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {

    private static MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    public static String getMessage(String codeOrMessage, Object... args) {
        if (codeOrMessage == null || codeOrMessage.isBlank()) {
            return codeOrMessage;
        }
        if (messageSource == null) {
            return codeOrMessage;
        }
        try {
            return messageSource.getMessage(codeOrMessage, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return codeOrMessage;
        }
    }

}
