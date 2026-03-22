package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.UserFeedbackMapper;
import com.wei.pojo.UserFeedback;
import com.wei.pojo.enums.FeedbackCategory;
import com.wei.pojo.enums.FeedbackStatus;
import com.wei.service.UserFeedbackService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserFeedbackServiceImpl extends ServiceImpl<UserFeedbackMapper, UserFeedback> implements UserFeedbackService {

    @Override
    public UserFeedback submitFeedback(Long userId, String category, String content) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setCategory(FeedbackCategory.fromCode(category));
        feedback.setContent(content);
        feedback.setStatus(FeedbackStatus.PENDING);
        save(feedback);
        return feedback;
    }

    @Override
    public Page<UserFeedback> getFeedbacks(int pageNum, int pageSize, String status, String category) {
        Page<UserFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        
        // 状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(UserFeedback::getStatus, FeedbackStatus.fromCode(status));
        }
        
        // 分类筛选
        if (category != null && !category.isEmpty()) {
            wrapper.eq(UserFeedback::getCategory, FeedbackCategory.fromCode(category));
        }
        
        wrapper.orderByDesc(UserFeedback::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void replyFeedback(Long feedbackId, String adminReply) {
        UserFeedback feedback = getById(feedbackId);
        if (feedback != null) {
            feedback.setAdminReply(adminReply);
            feedback.setStatus(FeedbackStatus.RESOLVED);
            feedback.setRepliedAt(LocalDateTime.now());
            updateById(feedback);
        }
    }
}
