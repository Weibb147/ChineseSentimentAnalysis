package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 反馈数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "反馈数据传输对象")
public class FeedbackDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈分类")
    private String category;

    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", required = true)
    private String content;
}
