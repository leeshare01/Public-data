package com.eshop.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public Result() {}

    public Result(int code, String message, T data, long timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    // ---- 成功 ----
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, System.currentTimeMillis());
    }

    // ---- 失败 ----
    public static <T> Result<T> error(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(ResultCode code, String message) {
        return new Result<>(code.getCode(), message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    // ---- 快捷方法 ----
    public static <T> Result<T> unauthorized(String message) {
        return error(UNAUTHORIZED, message);
    }

    public static <T> Result<T> forbidden() {
        return error(FORBIDDEN);
    }

    public static <T> Result<T> notFound(String message) {
        return error(NOT_FOUND, message);
    }

    private static final ResultCode UNAUTHORIZED = ResultCode.UNAUTHORIZED;
    private static final ResultCode FORBIDDEN = ResultCode.FORBIDDEN;
    private static final ResultCode NOT_FOUND = ResultCode.NOT_FOUND;

    // ---- Getters / Setters ----
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
