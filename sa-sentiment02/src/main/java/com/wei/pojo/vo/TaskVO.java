package com.wei.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.TaskSource;
import com.wei.pojo.enums.TaskStatus;
import com.wei.pojo.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务视图对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务视图对象")
public class TaskVO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务来源")
    private String source;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "总数量")
    private Integer totalCount;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failCount;

    @Schema(description = "任务耗时（毫秒）")
    private Integer durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;
}
