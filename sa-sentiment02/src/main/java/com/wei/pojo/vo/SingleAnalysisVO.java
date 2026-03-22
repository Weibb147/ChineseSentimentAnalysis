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
 * 单文本分析结果视图对象
 * 包含任务信息和分析结果
 * 
 * @author wei
 * @since 2025-12-28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "单文本分析结果视图对象")
public class SingleAnalysisVO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "分析内容")
    private String content;

    @Schema(description = "预测情感标签")
    private String predictedLabel;

    @Schema(description = "概率分布")
    private Map<String, Double> probability;

    @Schema(description = "关键词")
    private Map<String, Integer> keywords;
}
