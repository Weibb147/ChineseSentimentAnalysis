package com.wei.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型视图对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "模型视图对象")
public class ModelVO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "模型ID")
    private Long id;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型")
    private String modelType;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "模型描述")
    private String description;

    @Schema(description = "模型状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
