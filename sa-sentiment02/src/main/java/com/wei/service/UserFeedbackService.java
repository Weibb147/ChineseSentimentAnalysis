package com.wei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.UserFeedback;

public interface UserFeedbackService extends IService<UserFeedback> {
    /**
     * 提交反馈
     */
    UserFeedback submitFeedback(Long userId, String category, String content);

    /**
     * 分页查询反馈
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param status 状态筛选（可选）
     * @param category 分类筛选（可选）
     * @return 反馈分页列表
     */
    Page<UserFeedback> getFeedbacks(int pageNum, int pageSize, String status, String category);

    /**
     * 回复反馈
     */
    void replyFeedback(Long feedbackId, String adminReply);


}
