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
     * 结果对象 - 用于判断请求状态 { message, num }
     */
    @Nullable
    private ResultInfo result;

    /**
     * 业务数据 - 可能是直接包裹的data字段，也可能是根级别的其他字段
     */
    @Nullable
    private T data;

    /**
     * 结果信息内部类
     */
    public static class ResultInfo {
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
    public ResultInfo getResult() {
        return result;
    }

    public int getCode() {
        return result != null ? result.getNum() : -1;
    }

    public String getMessage() {
        return result != null && result.getMessage() != null ? result.getMessage() : "未知错误";
    }

    @Nullable
    public T getData() {
        return data;
    }

    public void setData(@Nullable T data) {
        this.data = data;
    }

    public void setResult(@Nullable ResultInfo result) {
        this.result = result;
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