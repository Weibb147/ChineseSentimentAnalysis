package com.wei.common.utils;

import com.wei.pojo.*;
import com.wei.pojo.vo.*;

/**
 * Bean转换工具类
 * 用于Entity和VO之间的转换
 * 
 * @author wei
 * @since 2025-11-10
 */
public class BeanConverter {

    /**
     * User转UserVO
     */
    public static UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getCode() : null)
                .sex(user.getSex() != null ? user.getSex().getCode() : null)
                .status(user.getStatus() != null ? user.getStatus().getCode() : null)
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * AnalysisTask转TaskVO
     */
    public static TaskVO toTaskVO(AnalysisTask task) {
        if (task == null) {
            return null;
        }
        return TaskVO.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .modelId(task.getModelId())
                .fileId(task.getFileId())
                .taskName(task.getTaskName())
                .taskType(task.getTaskType() != null ? task.getTaskType().getCode() : null)
                .source(task.getSource() != null ? task.getSource().getCode() : null)
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .durationMs(task.getDurationMs())
                .createdAt(task.getCreatedAt())
                .finishedAt(task.getFinishedAt())
                .build();
    }

    /**
     * AnalysisResult转ResultVO
     */
    public static ResultVO toResultVO(AnalysisResult result) {
        if (result == null) {
            return null;
        }
        ResultVO vo = ResultVO.builder()
                .id(result.getId())
                .content(result.getContent())
                .predictedLabel(result.getPredictedLabel())
                .createdAt(result.getCreatedAt())
                .build();
        
        // Parse JSON strings to Map
        if (result.getProbabilityJson() != null && !result.getProbabilityJson().equals("[]")) {
            vo.setProbability(JsonUtils.fromJson(result.getProbabilityJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Double>>() {}));
        }
        if (result.getKeywordsJson() != null && !result.getKeywordsJson().equals("[]")) {
            vo.setKeywords(JsonUtils.fromJson(result.getKeywordsJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Integer>>() {}));
        }
        
        return vo;
    }

    /**
     * ModelInfo转ModelVO
     */
    public static ModelVO toModelVO(ModelInfo model) {
        if (model == null) {
            return null;
        }
        return ModelVO.builder()
                .id(model.getId())
                .modelName(model.getModelName())
                .modelType(model.getModelType())
                .version(model.getVersion())
                .description(model.getDescription())
                .status(model.getStatus() != null ? model.getStatus().getCode() : null)
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    /**
     * FileUpload转FileUploadVO
     */
    public static FileUploadVO toFileUploadVO(FileUpload file) {
        if (file == null) {
            return null;
        }
        return FileUploadVO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .fileSizeKb(file.getFileSizeKb())
                .status(file.getStatus() != null ? file.getStatus().getCode() : null)
                .createdAt(file.getCreatedAt())
                .build();
    }

    /**
     * Notice转NoticeVO
     */
    public static NoticeVO toNoticeVO(Notice notice) {
        if (notice == null) {
            return null;
        }
        return NoticeVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .type(notice.getType() != null ? notice.getType().getCode() : null)
                .status(notice.getStatus() != null ? notice.getStatus().getCode() : null)
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }

    /**
     * UserFeedback转FeedbackVO
     */
    public static FeedbackVO toFeedbackVO(UserFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return FeedbackVO.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .category(feedback.getCategory() != null ? feedback.getCategory().getCode() : null)
                .content(feedback.getContent())
                .status(feedback.getStatus() != null ? feedback.getStatus().getCode() : null)
                .adminReply(feedback.getAdminReply())
                .createdAt(feedback.getCreatedAt())
                .repliedAt(feedback.getRepliedAt())
                .build();
    }
}
