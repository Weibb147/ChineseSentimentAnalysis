package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 模型状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum ModelStatus {
    /**
     * 激活
     */
    ACTIVE("ACTIVE", "激活"),
    
    /**
     * 未激活
     */
    INACTIVE("INACTIVE", "未激活"),
    
    /**
     * 训练中
     */
    TRAINING("TRAINING", "训练中"),
    
    /**
     * 已弃用
     */
    DEPRECATED("DEPRECATED", "已弃用");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    ModelStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static ModelStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ModelStatus status : ModelStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的模型状态: " + code);
    }
}
