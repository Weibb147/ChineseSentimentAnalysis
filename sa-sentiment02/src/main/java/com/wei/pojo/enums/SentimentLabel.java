package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 情感分析标签枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum SentimentLabel {
    /**
     * 愤怒
     */
    ANGRY("angry", "愤怒"),
    
    /**
     * 开心
     */
    HAPPY("happy", "开心"),
    
    /**
     * 悲伤
     */
    SAD("sad", "悲伤"),
    
    /**
     * 中性
     */
    NEUTRAL("neutral", "中性"),
    
    /**
     * 恐惧
     */
    FEAR("fear", "恐惧"),
    
    /**
     * 惊讶
     */
    SURPRISE("surprise", "惊讶");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    SentimentLabel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static SentimentLabel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SentimentLabel label : SentimentLabel.values()) {
            if (label.code.equals(code)) {
                return label;
            }
        }
        throw new IllegalArgumentException("未知的情感标签: " + code);
    }
}
