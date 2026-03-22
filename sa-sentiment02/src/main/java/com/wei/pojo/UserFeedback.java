package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.FeedbackCategory;
import com.wei.pojo.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_feedback")
@Schema(description = "用户反馈实体")
public class UserFeedback implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "用户ID（允许匿名）")
    private Long userId;

    @Builder.Default
    @Schema(description = "反馈分类")
    private FeedbackCategory category = FeedbackCategory.OTHER;

    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", required = true)
    private String content;

    @Builder.Default
    @Schema(description = "反馈状态")
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Schema(description = "管理员回复")
    private String adminReply;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "回复时间")
    private LocalDateTime repliedAt;

    /**
     * 判断反馈是否已解决
     */
    public boolean isResolved() {
        return FeedbackStatus.RESOLVED.equals(this.status);
    }

    /**
     * 判断反馈是否已回复
     */
    public boolean hasReply() {
        return adminReply != null && !adminReply.isEmpty();
    }
}
