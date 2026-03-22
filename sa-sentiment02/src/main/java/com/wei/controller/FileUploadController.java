package com.wei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.exception.BusinessException;
import com.wei.common.utils.AliOssUtil;
import com.wei.common.utils.AuthContext;
import com.wei.common.utils.ResultCode;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.FileUpload;
import com.wei.pojo.User;
import com.wei.pojo.vo.FileUploadVO;
import com.wei.service.FileUploadService;
import com.wei.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UserService userService;

    /**
     * 上传文件到阿里云OSS
     * @param file 文件
     * @return 文件上传信息
     */
    @PostMapping("/upload")
    public ResultUtils<FileUploadVO> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            // 验证文件
            validateFile(file, 10 * 1024 * 1024); // 10MB limit for general files

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String filename = generateUniqueFilename(originalFilename);
            
            // 上传到阿里云OSS
            String url = AliOssUtil.uploadFile(filename, file.getInputStream());
            
            // 保存文件记录到数据库
            FileUpload fileUpload = fileUploadService.uploadFile(
                    userId,
                    originalFilename,
                    url,
                    file.getContentType(),
                    (int) (file.getSize() / 1024)
            );

            FileUploadVO vo = new FileUploadVO();
            BeanUtils.copyProperties(fileUpload, vo);
            return ResultUtils.success(vo);
        } catch (BusinessException e) {
            log.error("文件上传失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResultUtils.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传用户头像
     * @param file 头像文件
     * @return 头像URL和消息
     */
    @PostMapping("/upload/avatar")
    public ResultUtils<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            // 验证文件
            validateImageFile(file, 2 * 1024 * 1024); // 2MB limit for avatar

            // 生成唯一文件名（添加avatar前缀）
            String originalFilename = file.getOriginalFilename();
            String filename = "avatar/" + generateUniqueFilename(originalFilename);
            
            // 上传到阿里云OSS
            String url = AliOssUtil.uploadFile(filename, file.getInputStream());
            
            // 更新用户头像URL - 使用updateProfile方法
            User partialUser = new User();
            partialUser.setId(userId);
            partialUser.setImageUrl(url);
            userService.updateProfile(partialUser);
            
            // 保存文件记录到数据库
            fileUploadService.uploadFile(
                    userId,
                    originalFilename,
                    url,
                    file.getContentType(),
                    (int) (file.getSize() / 1024)
            );

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("message", "头像上传成功");
            
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            log.error("头像上传失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResultUtils.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     * @param file 文件
     * @param maxSize 最大文件大小（字节）
     */
    private void validateFile(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FAIL, "文件不能为空");
        }

        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FAIL, 
                String.format("文件大小不能超过%dMB", maxSize / 1024 / 1024));
        }
    }

    /**
     * 验证图片文件
     * @param file 文件
     * @param maxSize 最大文件大小（字节）
     */
    private void validateImageFile(MultipartFile file, long maxSize) {
        validateFile(file, maxSize);

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.FAIL, "只能上传图片文件");
        }

        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(
                originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
                throw new BusinessException(ResultCode.FAIL, 
                    "只支持 JPG、PNG、GIF、BMP、WEBP 格式的图片");
            }
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 分页查询用户上传的文件
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 文件列表
     */
    @GetMapping("/list")
    public ResultUtils<Page<FileUploadVO>> getFileList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            Page<FileUpload> page = new Page<>(pageNum, pageSize);
            page = fileUploadService.lambdaQuery()
                    .eq(FileUpload::getUserId, userId)
                    .orderByDesc(FileUpload::getCreatedAt)
                    .page(page);

            Page<FileUploadVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            voPage.setRecords(page.getRecords().stream().map(file -> {
                FileUploadVO vo = new FileUploadVO();
                BeanUtils.copyProperties(file, vo);
                return vo;
            }).toList());

            return ResultUtils.success(voPage);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return ResultUtils.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResultUtils<Void> deleteFile(@PathVariable Long id) {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            FileUpload fileUpload = fileUploadService.lambdaQuery()
                    .eq(FileUpload::getId, id)
                    .eq(FileUpload::getUserId, userId)
                    .one();
                    
            if (fileUpload == null) {
                return ResultUtils.error("文件不存在或无权限删除");
            }
            
            // 注意：如果使用阿里云OSS，这里应该调用OSS的删除API
            // TODO: 实现OSS文件删除
            // AliOssUtil.deleteFile(fileUpload.getFilePath());
            
            // 删除数据库记录
            fileUploadService.removeById(id);
            
            return ResultUtils.success();
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return ResultUtils.error("删除文件失败: " + e.getMessage());
        }
    }
}
