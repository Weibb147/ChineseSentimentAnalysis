package com.wei.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.FileUploadMapper;
import com.wei.pojo.FileUpload;
import com.wei.pojo.enums.FileUploadStatus;
import com.wei.service.FileUploadService;
import org.springframework.stereotype.Service;

@Service
public class FileUploadServiceImpl extends ServiceImpl<FileUploadMapper, FileUpload> implements FileUploadService {

    @Override
    public FileUpload uploadFile(Long userId, String fileName, String filePath, String fileType, Integer fileSizeKb) {
        FileUpload fileUpload = new FileUpload();
        fileUpload.setUserId(userId);
        fileUpload.setFileName(fileName);
        fileUpload.setFilePath(filePath);
        fileUpload.setFileType(fileType);
        fileUpload.setFileSizeKb(fileSizeKb);
        fileUpload.setStatus(FileUploadStatus.UPLOADED);
        save(fileUpload);
        return fileUpload;
    }
}
