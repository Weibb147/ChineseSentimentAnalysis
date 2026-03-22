package com.wei.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一返回结果封装
 */
@Data
@AllArgsConstructor
public class ResultUtils<T> {
    private Integer code;     // 业务状态码
    private String message;   // 提示信息
    private T data;           // 响应数据

    /**
     * 成功（带数据）
     */
    public static <T> ResultUtils<T> success(T data) {
        return new ResultUtils<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功（无数据）
     */
    public static <T> ResultUtils<T> success() {
        return new ResultUtils<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 失败（自定义提示）
     */
    public static <T> ResultUtils<T> error(String message) {
        return new ResultUtils<>(ResultCode.FAIL.getCode(), message, null);
    }

    /**
     * 失败（使用状态码枚举）
     */
    public static <T> ResultUtils<T> error(ResultCode code) {
        return new ResultUtils<>(code.getCode(), code.getMessage(), null);
    }

    /**
     * 失败（自定义状态码和提示）
     */
    public static <T> ResultUtils<T> error(int code, String message) {
        return new ResultUtils<>(code, message, null);
    }
}
