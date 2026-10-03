package com.humolix.volur.framework.common.exception.authException.csrfException;

public final class CsrfTokenMismatchException extends CsrfValidationException {

    public CsrfTokenMismatchException() {
        super(131402, "CSRF 凭证校验失败");
    }

    public CsrfTokenMismatchException(String message) {
        super(131402, message);
    }

}
