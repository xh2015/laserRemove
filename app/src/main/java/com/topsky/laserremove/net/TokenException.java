package com.topsky.laserremove.net;

import com.hjq.http.exception.HttpException;

public final class TokenException extends HttpException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
