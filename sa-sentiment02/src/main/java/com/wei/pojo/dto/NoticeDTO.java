package com.wei.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 公告数据传输对象
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "公告数据传输对象")
public class NoticeDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "公告标题不能为空")
    @Schema(description = "公告标题", required = true)
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容", required = true)
    private String content;

    @Schema(description = "公告类型")
    private String type;

    @Schema(description = "公告状态")
    private String status;
}
