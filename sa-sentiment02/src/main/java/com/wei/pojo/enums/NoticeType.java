package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 公告类型枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum NoticeType {
    /**
     * 系统公告
     */
    SYSTEM("SYSTEM", "系统公告"),
    
    /**
     * 更新公告
     */
    UPDATE("UPDATE", "更新公告"),
    
    /**
     * 活动公告
     */
    EVENT("EVENT", "活动公告"),
    
    /**
     * 其他
     */
    OTHER("OTHER", "其他");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    NoticeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static NoticeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NoticeType type : NoticeType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的公告类型: " + code);
    }
}
