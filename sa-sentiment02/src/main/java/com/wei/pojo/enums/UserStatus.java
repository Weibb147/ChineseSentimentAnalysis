package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum UserStatus {
    /**
     * 禁用
     */
    DISABLED(0, "禁用"),
    
    /**
     * 启用
     */
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final Integer code;
    
    private final String description;

    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : UserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态: " + code);
    }
}
