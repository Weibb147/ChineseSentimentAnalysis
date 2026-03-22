package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 模型运行类型枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum ModelRunType {
    /**
     * 训练
     */
    TRAIN("TRAIN", "训练"),
    
    /**
     * 评估
     */
    EVAL("EVAL", "评估"),
    
    /**
     * 部署
     */
    DEPLOY("DEPLOY", "部署");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    ModelRunType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static ModelRunType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ModelRunType type : ModelRunType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的模型运行类型: " + code);
    }
}
