package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分析请求数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分析请求数据传输对象")
public class AnalysisRequestDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分析文本不能为空")
    @Schema(description = "分析文本内容", required = true)
    private String content;

    @Schema(description = "模型ID（可选，不传则使用默认激活模型）")
    private Long modelId;

    @Schema(description = "模型名称（优先使用，如roberta_base、roberta_bilstm_attention等）")
    private String modelName;

    @Schema(description = "任务名称")
    private String taskName;
}
