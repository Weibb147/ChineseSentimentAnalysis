package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分析结果实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("analysis_result")
@Schema(description = "分析结果实体")
public class AnalysisResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "结果ID")
    private Long id;

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", required = true)
    private Long taskId;

    @NotBlank(message = "分析内容不能为空")
    @Schema(description = "分析内容", required = true)
    private String content;

    @Schema(description = "预测情感标签")
    private String predictedLabel;

    @Schema(description = "概率分布JSON")
    private String probabilityJson;

    @Schema(description = "关键词JSON")
    private String keywordsJson;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 获取情感标签中文描述
     */
    public String getPredictedLabelDescription() {
        if (predictedLabel == null) {
            return null;
        }
        try {
            SentimentLabel label = SentimentLabel.fromCode(predictedLabel);
            return label.getDescription();
        } catch (Exception e) {
            return predictedLabel;
        }
    }
}