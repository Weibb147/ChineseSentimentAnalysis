package com.wei.common.exception;

import lombok.Getter;

/**
 * 参数校验异常
 */
@Getter
public class ValidationException extends RuntimeException {

    private final int code;

    public ValidationException(String message) {
        super(message);
        this.code = 400; // 默认 HTTP 400
    }

    public ValidationException(int code, String message) {
        super(message);
        this.code = code;
    }
}