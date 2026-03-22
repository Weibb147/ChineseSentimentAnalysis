package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户角色枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum UserRole {
    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员"),
    
    /**
     * 普通用户
     */
    USER("USER", "普通用户");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserRole role : UserRole.values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的用户角色: " + code);
    }
}
