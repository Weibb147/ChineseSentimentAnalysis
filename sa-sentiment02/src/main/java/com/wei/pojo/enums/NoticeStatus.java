package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 公告状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum NoticeStatus {
    /**
     * 可见
     */
    VISIBLE("VISIBLE", "可见"),
    
    /**
     * 隐藏
     */
    HIDDEN("HIDDEN", "隐藏");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    NoticeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static NoticeStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NoticeStatus status : NoticeStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的公告状态: " + code);
    }
}
