package com.wei.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.FileUpload;

public interface FileUploadService extends IService<FileUpload> {
    /**
     * 上传文件
     */
    FileUpload uploadFile(Long userId, String fileName, String filePath, String fileType, Integer fileSizeKb);
}
