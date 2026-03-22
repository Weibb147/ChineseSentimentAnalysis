package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 性别枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum Gender {
    /**
     * 男
     */
    MALE("男", "男"),
    
    /**
     * 女
     */
    FEMALE("女", "女"),
    
    /**
     * 保密
     */
    UNKNOWN("保密", "保密");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    Gender(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static Gender fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (Gender gender : Gender.values()) {
            if (gender.code.equals(code) || gender.name().equalsIgnoreCase(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("未知的性别: " + code);
    }
}
