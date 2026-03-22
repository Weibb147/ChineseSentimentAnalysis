package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 反馈状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum FeedbackStatus {
    /**
     * 待处理
     */
    PENDING("PENDING", "待处理"),
    
    /**
     * 处理中
     */
    IN_PROGRESS("IN_PROGRESS", "处理中"),
    
    /**
     * 已解决
     */
    RESOLVED("RESOLVED", "已解决");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    FeedbackStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static FeedbackStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeedbackStatus status : FeedbackStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的反馈状态: " + code);
    }
}
