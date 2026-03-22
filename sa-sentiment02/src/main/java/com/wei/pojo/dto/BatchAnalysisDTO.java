package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 批量分析数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "批量分析数据传输对象")
public class BatchAnalysisDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotNull(message = "文件ID不能为空")
    @Schema(description = "文件ID", required = true)
    private Long fileId;

    @Schema(description = "模型ID（可选，不传则使用默认激活模型）")
    private Long modelId;

    @Schema(description = "任务名称")
    private String taskName;
}
