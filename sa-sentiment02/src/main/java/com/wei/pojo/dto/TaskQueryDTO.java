package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务查询数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务查询数据传输对象")
public class TaskQueryDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "开始日期（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）")
    private String endDate;
}