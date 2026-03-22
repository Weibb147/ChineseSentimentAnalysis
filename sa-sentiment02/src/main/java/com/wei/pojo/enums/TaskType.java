package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 分析任务类型枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum TaskType {
    /**
     * 单条分析
     */
    SINGLE("SINGLE", "单条分析"),
    
    /**
     * 批量分析
     */
    BATCH("BATCH", "批量分析");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    TaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskType type : TaskType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任务类型: " + code);
    }
}
