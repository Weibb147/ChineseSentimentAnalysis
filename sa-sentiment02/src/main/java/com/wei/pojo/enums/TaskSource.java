package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务来源枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum TaskSource {
    /**
     * Web端
     */
    WEB("WEB", "Web端"),
    
    /**
     * API接口
     */
    API("API", "API接口");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    TaskSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskSource fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskSource source : TaskSource.values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的任务来源: " + code);
    }
}
