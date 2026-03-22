package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.NoticeStatus;
import com.wei.pojo.enums.NoticeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("notice")
@Schema(description = "公告实体")
public class Notice implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "公告ID")
    private Long id;

    @NotBlank(message = "公告标题不能为空")
    @Schema(description = "公告标题", required = true)
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容", required = true)
    private String content;

    @Builder.Default
    @Schema(description = "公告类型")
    private NoticeType type = NoticeType.SYSTEM;

    @Builder.Default
    @Schema(description = "公告状态")
    private NoticeStatus status = NoticeStatus.VISIBLE;

    @Schema(description = "发布人ID（管理员）")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 判断公告是否可见
     */
    public boolean isVisible() {
        return NoticeStatus.VISIBLE.equals(this.status);
    }


}
