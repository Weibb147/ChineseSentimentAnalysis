package com.wei.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 分析结果视图对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分析结果视图对象")
public class ResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "结果ID")
    private Long id;

    @Schema(description = "分析内容")
    private String content;

    @Schema(description = "预测情感标签")
    private String predictedLabel;

    @Schema(description = "概率分布")
    private Map<String, Double> probability;

    @Schema(description = "关键词")
    private Map<String, Integer> keywords;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}