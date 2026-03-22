package com.wei.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API统一返回状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 通用状态码
    SUCCESS(0, "操作成功"),
    FAIL(1, "操作失败"),
    SERVER_ERROR(500, "服务器内部错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),

    // 业务状态码
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    VERIFICATION_CODE_ERROR(1004, "验证码错误"),
    EMAIL_ALREADY_REGISTERED(1005, "邮箱已被注册"),
    USER_DISABLED(1006, "用户已被禁用");

    private final int code;
    private final String message;
}