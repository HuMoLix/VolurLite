package com.humolix.volur.framework.common.exception.authException.csrfException;

public final class CsrfOriginDeniedException extends CsrfValidationException {

    public CsrfOriginDeniedException() {
        super(131401, "CSRF 来源域名校验未通过");
    }

    public CsrfOriginDeniedException(String message) {
        super(131401, message);
    }

}
