package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.TaskSource;
import com.wei.pojo.enums.TaskStatus;
import com.wei.pojo.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分析任务实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("analysis_task")
@Schema(description = "分析任务实体")
public class AnalysisTask implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "任务ID")
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private Long userId;

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID", required = true)
    private Long modelId;

    @Schema(description = "文件ID（批量任务）")
    private Long fileId;

    @Schema(description = "任务名称")
    private String taskName;

    @Builder.Default
    @Schema(description = "任务类型")
    private TaskType taskType = TaskType.SINGLE;

    @Builder.Default
    @Schema(description = "任务来源")
    private TaskSource source = TaskSource.WEB;

    @Builder.Default
    @Schema(description = "任务状态")
    private TaskStatus status = TaskStatus.PENDING;

    @Builder.Default
    @Schema(description = "总数量")
    private Integer totalCount = 0;

    @Builder.Default
    @Schema(description = "成功数量")
    private Integer successCount = 0;

    @Builder.Default
    @Schema(description = "失败数量")
    private Integer failCount = 0;

    @Schema(description = "任务耗时（毫秒）")
    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    /**
     * 判断任务是否完成
     */
    public boolean isFinished() {
        return TaskStatus.FINISHED.equals(this.status);
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed() {
        return TaskStatus.FAILED.equals(this.status);
    }

    /**
     * 判断任务是否运行中
     */
    public boolean isRunning() {
        return TaskStatus.RUNNING.equals(this.status);
    }

    /**
     * 判断是否为批量任务
     */
    public boolean isBatchTask() {
        return TaskType.BATCH.equals(this.taskType);
    }

    /**
     * 计算任务成功率
     */
    public Double getSuccessRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return (successCount * 100.0) / totalCount;
    }
}