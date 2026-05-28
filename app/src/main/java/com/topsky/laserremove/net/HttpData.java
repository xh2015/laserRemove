package com.topsky.laserremove.net;

import java.util.Map;

import androidx.annotation.Nullable;

public class HttpData<T> {

    /**
     * 响应头
     */
    @Nullable
    private Map<String, String> responseHeaders;

    /**
     * 结果对象
     */
    @Nullable
    private Result result;

    /**
     * 数据
     */
    @Nullable
    private T data;

    public static class Result {
        /**
         * 消息
         */
        private String message;

        /**
         * 状态码
         */
        private int num;

        public String getMessage() {
            return message;
        }

        public int getNum() {
            return num;
        }
    }

    public void setResponseHeaders(@Nullable Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Nullable
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    @Nullable
    public Result getResult() {
        return result;
    }

    public int getCode() {
        return result != null ? result.getNum() : -1;
    }

    public String getMessage() {
        return result != null ? result.getMessage() : "未知错误";
    }

    @Nullable
    public T getData() {
        return data;
    }

    /**
     * 是否请求成功
     */
    public boolean isRequestSuccess() {
        return getCode() == 200;
    }

    /**
     * 是否 Token 失效
     */
    public boolean isTokenInvalidation() {
        return getCode() == 1001 || getCode() == 401;
    }
}