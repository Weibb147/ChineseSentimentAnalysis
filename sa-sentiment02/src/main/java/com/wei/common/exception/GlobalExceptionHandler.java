package com.wei.common.exception;

import com.wei.common.utils.ResultUtils;
import com.wei.common.utils.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResultUtils<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultUtils<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResultUtils.error(message);
    }

    ///**
    // * 处理参数校验异常（@Validated）
    // */
    //@ExceptionHandler(ConstraintViolationException.class)
    //public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
    //    return Result.error(e.getMessage());
    //}

    /**
     * 处理其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResultUtils<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResultUtils.error(ResultCode.SERVER_ERROR);
    }
}
