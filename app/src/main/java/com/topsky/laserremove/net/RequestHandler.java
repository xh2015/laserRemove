package com.topsky.laserremove.net;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hjq.gson.factory.GsonFactory;
import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestHandler;
import com.hjq.http.exception.CancelException;
import com.hjq.http.exception.DataException;
import com.hjq.http.exception.FileMd5Exception;
import com.hjq.http.exception.HttpException;
import com.hjq.http.exception.NetworkException;
import com.hjq.http.exception.NullBodyException;
import com.hjq.http.exception.ResponseException;
import com.hjq.http.exception.ServerException;
import com.hjq.http.exception.TimeoutException;
import com.hjq.http.request.HttpRequest;
import com.topsky.laserremove.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class RequestHandler implements IRequestHandler {

    private final Application mApplication;

    public RequestHandler(Application application) {
        mApplication = application;
    }

    @NonNull
    @Override
    public Object requestSuccess(@NonNull HttpRequest<?> httpRequest, @NonNull Response response, @NonNull Type type) throws Throwable {
        if (Response.class.equals(type)) {
            return response;
        }

        if (!response.isSuccessful()) {
            throw new ResponseException(String.format(mApplication.getString(R.string.http_response_error),
                    response.code(), response.message()), response);
        }

        if (Headers.class.equals(type)) {
            return response.headers();
        }

        ResponseBody body = response.body();
        if (body == null) {
            throw new NullBodyException(mApplication.getString(R.string.http_response_null_body));
        }

        if (ResponseBody.class.equals(type)) {
            return body;
        }

        // 如果是用数组接收，判断一下是不是用 byte[] 类型进行接收的
        if (type instanceof GenericArrayType) {
            Type genericComponentType = ((GenericArrayType) type).getGenericComponentType();
            if (byte.class.equals(genericComponentType)) {
                return body.bytes();
            }
        }

        if (InputStream.class.equals(type)) {
            return body.byteStream();
        }

        if (Bitmap.class.equals(type)) {
            return BitmapFactory.decodeStream(body.byteStream());
        }

        String text;
        try {
            text = body.string();
        } catch (IOException e) {
            // 返回结果读取异常
            throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
        }

        // 打印这个 Json 或者文本
        EasyLog.printJson(httpRequest, text);

        if (String.class.equals(type)) {
            return text;
        }

        final Object result;

        try {
            // 先解析为 JsonObject 进行预处理
            JsonObject jsonObject = JsonParser.parseString(text).getAsJsonObject();

            // 检查是否是 HttpData 类型
            if (type instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType parameterizedType = (java.lang.reflect.ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();

                // 如果返回类型是 HttpData，需要特殊处理
                if (rawType instanceof Class && HttpData.class.isAssignableFrom((Class<?>) rawType)) {
                    // 提取 result 字段
                    HttpData.ResultInfo resultInfo = null;
                    if (jsonObject.has("result")) {
                        resultInfo = GsonFactory.getSingletonGson().fromJson(jsonObject.get("result"), HttpData.ResultInfo.class);
                        jsonObject.remove("result");
                    }

                    // 提取泛型类型 T
                    Type dataType = parameterizedType.getActualTypeArguments()[0];

                    // 根据泛型类型决定如何提取数据
                    Object data = null;
                    if (dataType instanceof Class && !((Class<?>) dataType).isAssignableFrom(Map.class)) {
                        // 如果泛型是具体的 Bean 类（如 CameraInfoApi.Bean）
                        // 尝试从根级别提取与 Bean 类匹配的字段
                        data = extractBeanData(jsonObject, (Class<?>) dataType);
                    } else {
                        // 如果泛型是 Map 或其他类型，将整个 jsonObject（除了result）作为 data
                        data = GsonFactory.getSingletonGson().fromJson(jsonObject, dataType);
                    }

                    // 创建 HttpData 实例并设置值
                    HttpData<Object> httpData = new HttpData<>();
                    httpData.setResult(resultInfo);
                    httpData.setData(data);
                    result = httpData;
                } else {
                    // 非 HttpData 类型，正常解析
                    result = GsonFactory.getSingletonGson().fromJson(text, type);
                }
            } else {
                // 非参数化类型，正常解析
                result = GsonFactory.getSingletonGson().fromJson(text, type);
            }
        } catch (JsonSyntaxException e) {
            // 返回结果读取异常
            throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
        }

        if (result instanceof HttpData) {
            HttpData<?> model = (HttpData<?>) result;
            Headers headers = response.headers();
            int headersSize = headers.size();
            Map<String, String> headersMap = new HashMap<>(headersSize);
            for (int i = 0; i < headersSize; i++) {
                headersMap.put(headers.name(i), headers.value(i));
            }
            // Github issue 地址：https://github.com/getActivity/EasyHttp/issues/233
            model.setResponseHeaders(headersMap);

            if (model.isRequestSuccess()) {
                // 代表执行成功
                return result;
            }

            if (model.isTokenInvalidation()) {
                // 代表登录失效，需要重新登录
                throw new TokenException(mApplication.getString(R.string.http_token_error));
            }

            // 代表执行失败
            throw new ResultException(model.getMessage(), model);
        }
        return result;
    }

    /**
     * 从 JsonObject 中提取与指定 Bean 类匹配的数据
     * 策略：遍历 JsonObject 的所有字段，找到与 Bean 类字段最匹配的顶级字段
     */
    private Object extractBeanData(JsonObject jsonObject, Class<?> beanClass) {
        Gson gson = GsonFactory.getSingletonGson();

        // 策略1：如果 Bean 类有内部类结构，尝试匹配字段名
        // 例如：CameraInfoApi.Bean 中有 Focus 字段，JSON 根级别也有 Focus 字段

        // 获取 Bean 类的所有字段名
        java.lang.reflect.Field[] fields = beanClass.getDeclaredFields();

        // 创建一个临时的 JsonObject 来收集匹配的字段
        JsonObject matchedData = new JsonObject();

        for (java.lang.reflect.Field field : fields) {
            String fieldName = field.getName();
            if (jsonObject.has(fieldName)) {
                // 找到匹配的字段，添加到结果中
                matchedData.add(fieldName, jsonObject.get(fieldName));
            }
        }

        // 如果有匹配的字段，解析为 Bean 类
        if (matchedData.size() > 0) {
            try {
                return gson.fromJson(matchedData, beanClass);
            } catch (Exception e) {
                // 解析失败，返回整个 jsonObject
                return gson.fromJson(jsonObject, beanClass);
            }
        }

        // 没有匹配的字段，尝试直接解析整个 jsonObject
        return gson.fromJson(jsonObject, beanClass);
    }

    @NonNull
    @Override
    public Throwable requestFail(@NonNull HttpRequest<?> httpRequest, @NonNull Throwable throwable) {
        if (throwable instanceof HttpException) {
            if (throwable instanceof TokenException) {
                // 登录信息失效，跳转到登录页
            }
            return throwable;
        }

        if (throwable instanceof SocketTimeoutException) {
            return new TimeoutException(mApplication.getString(R.string.http_server_out_time), throwable);
        }

        if (throwable instanceof UnknownHostException) {
            NetworkInfo info = ((ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            // 判断网络是否连接
            if (info != null && info.isConnected()) {
                // 有连接就是服务器的问题
                return new ServerException(mApplication.getString(R.string.http_server_error), throwable);
            }
            // 没有连接就是网络异常
            return new NetworkException(mApplication.getString(R.string.http_network_error), throwable);
        }

        if (throwable instanceof IOException) {
            // 出现该异常的两种情况
            // 1. 调用 EasyHttp 取消请求
            // 2. 网络请求被中断
            return new CancelException(mApplication.getString(R.string.http_request_cancel), throwable);
        }

        return new HttpException(throwable.getMessage(), throwable);
    }

    @NonNull
    @Override
    public Throwable downloadFail(@NonNull HttpRequest<?> httpRequest, @NonNull Throwable throwable) {
        if (throwable instanceof ResponseException) {
            ResponseException responseException = ((ResponseException) throwable);
            Response response = responseException.getResponse();
            responseException.setMessage(String.format(mApplication.getString(R.string.http_response_error),
                    response.code(), response.message()));
            return responseException;
        } else if (throwable instanceof NullBodyException) {
            NullBodyException nullBodyException = ((NullBodyException) throwable);
            nullBodyException.setMessage(mApplication.getString(R.string.http_response_null_body));
            return nullBodyException;
        } else if (throwable instanceof FileMd5Exception) {
            FileMd5Exception fileMd5Exception = ((FileMd5Exception) throwable);
            fileMd5Exception.setMessage(mApplication.getString(R.string.http_response_md5_error));
            return fileMd5Exception;
        }
        return requestFail(httpRequest, throwable);
    }
}
