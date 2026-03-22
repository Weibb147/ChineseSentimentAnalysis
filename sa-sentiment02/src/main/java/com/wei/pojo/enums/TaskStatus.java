package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 分析任务状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum TaskStatus {
    /**
     * 等待中
     */
    PENDING("PENDING", "等待中"),
    
    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),
    
    /**
     * 已完成
     */
    FINISHED("FINISHED", "已完成"),
    
    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus status : TaskStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任务状态: " + code);
    }
}
