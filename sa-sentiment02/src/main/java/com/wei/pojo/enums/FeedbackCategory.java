package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 反馈分类枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum FeedbackCategory {
    /**
     * Bug反馈
     */
    BUG("BUG", "Bug反馈"),
    
    /**
     * 功能建议
     */
    SUGGESTION("SUGGESTION", "功能建议"),
    
    /**
     * 使用体验
     */
    EXPERIENCE("EXPERIENCE", "使用体验"),
    
    /**
     * 其他
     */
    OTHER("OTHER", "其他");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    FeedbackCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static FeedbackCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeedbackCategory category : FeedbackCategory.values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("未知的反馈分类: " + code);
    }
}
