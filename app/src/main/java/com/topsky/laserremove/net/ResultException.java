package com.topsky.laserremove.net;

import com.hjq.http.exception.HttpException;

import androidx.annotation.NonNull;

public final class ResultException extends HttpException {

    private final HttpData<?> mData;

    public ResultException(String message, HttpData<?> data) {
        super(message);
        mData = data;
    }

    public ResultException(String message, Throwable cause, HttpData<?> data) {
        super(message, cause);
        mData = data;
    }

    @NonNull
    public HttpData<?> getHttpData() {
        return mData;
    }
}
