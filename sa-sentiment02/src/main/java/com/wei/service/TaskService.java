package com.wei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.AnalysisTask;

/**
 * @author: admin
 * @date: 2025/9/1
 */
public interface TaskService extends IService<AnalysisTask> {
    /**
     * 创建单条分析任务
     */
    AnalysisTask createSingleTask(Long userId, Long modelId, String taskName);

    /**
     * 创建批量分析任务
     */
    AnalysisTask createBatchTask(Long userId, Long modelId, Long fileId, String taskName);

    /**
     * 分页查询用户任务
     */
    Page<AnalysisTask> getUserTasks(Long userId, int pageNum, int pageSize, String status, String taskName, String taskType, String startDate, String endDate);

    /**
     * 更新任务状态
     */
    void updateTaskStatus(Long taskId, String status, Integer successCount, Integer failCount, Integer durationMs);
}
