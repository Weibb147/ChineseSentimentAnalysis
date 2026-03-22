package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务创建数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务创建数据传输对象")
public class TaskCreateDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID", required = true)
    private Long modelId;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "单条文本内容")
    private String content;

    @Schema(description = "任务名称")
    private String taskName;
}