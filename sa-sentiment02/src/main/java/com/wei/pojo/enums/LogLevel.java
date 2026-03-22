package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 日志级别枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum LogLevel {
    /**
     * 信息
     */
    INFO("INFO", "信息"),
    
    /**
     * 警告
     */
    WARN("WARN", "警告"),
    
    /**
     * 错误
     */
    ERROR("ERROR", "错误"),
    
    /**
     * 严重错误
     */
    CRITICAL("CRITICAL", "严重错误");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    LogLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static LogLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LogLevel level : LogLevel.values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的日志级别: " + code);
    }
}
