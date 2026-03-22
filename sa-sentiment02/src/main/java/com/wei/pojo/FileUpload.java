package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wei.pojo.enums.FileUploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件上传实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("file_upload")
@Schema(description = "文件上传实体")
public class FileUpload implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "文件ID")
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private Long userId;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件原始名称", required = true)
    private String fileName;

    @NotBlank(message = "文件路径不能为空")
    @Schema(description = "服务器存储路径", required = true)
    private String filePath;

    @Schema(description = "文件MIME类型")
    private String fileType;

    @Schema(description = "文件大小（KB）")
    private Integer fileSizeKb;

    @Builder.Default
    @Schema(description = "文件状态")
    private FileUploadStatus status = FileUploadStatus.UPLOADED;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 判断文件是否已解析
     */
    public boolean isParsed() {
        return FileUploadStatus.PARSED.equals(this.status);
    }

    /**
     * 判断文件是否已删除
     */
    public boolean isDeleted() {
        return FileUploadStatus.DELETED.equals(this.status);
    }
}
