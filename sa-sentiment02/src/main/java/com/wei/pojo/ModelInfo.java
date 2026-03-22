package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.ModelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型信息实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("model_info")
@Schema(description = "模型信息实体")
public class ModelInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "模型ID")
    private Long id;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", required = true)
    private String modelName;

    @NotBlank(message = "模型类型不能为空")
    @Schema(description = "模型类型（RoBERTa, BERT）", required = true)
    private String modelType;

    @NotBlank(message = "版本号不能为空")
    @Schema(description = "版本号", required = true)
    private String version;

    @Schema(description = "模型描述")
    private String description;

    @NotBlank(message = "模型文件路径不能为空")
    @Schema(description = "模型文件路径", required = true)
    private String modelFilePath;

    @Builder.Default
    @Schema(description = "模型状态")
    private ModelStatus status = ModelStatus.INACTIVE;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 判断模型是否激活
     */
    public boolean isActive() {
        return ModelStatus.ACTIVE.equals(this.status);
    }

    /**
     * 判断模型是否在训练中
     */
    public boolean isTraining() {
        return ModelStatus.TRAINING.equals(this.status);
    }
}