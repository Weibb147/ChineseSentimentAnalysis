package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 模型创建数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "模型创建数据传输对象")
public class ModelCreateDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

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
}
