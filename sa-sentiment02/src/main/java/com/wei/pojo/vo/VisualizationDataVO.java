package com.wei.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Schema(description = "可视化数据VO")
public class VisualizationDataVO {

    @Schema(description = "结果ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "预测情感标签")
    private String predictedLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "分析内容")
    private String content;

    @Schema(description = "概率分布")
    private java.util.Map<String, Double> probability;

    @Schema(description = "关键词")
    private java.util.Map<String, Integer> keywords;
}
