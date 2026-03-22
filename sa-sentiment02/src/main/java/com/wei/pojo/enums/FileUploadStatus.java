package com.wei.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 文件上传状态枚举
 * 
 * @author wei
 * @since 2025-11-10
 */
@Getter
public enum FileUploadStatus {
    /**
     * 已上传
     */
    UPLOADED("UPLOADED", "已上传"),
    
    /**
     * 已解析
     */
    PARSED("PARSED", "已解析"),
    
    /**
     * 已删除
     */
    DELETED("DELETED", "已删除"),
    
    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    @EnumValue
    @JsonValue
    private final String code;
    
    private final String description;

    FileUploadStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static FileUploadStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FileUploadStatus status : FileUploadStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的文件状态: " + code);
    }
}
