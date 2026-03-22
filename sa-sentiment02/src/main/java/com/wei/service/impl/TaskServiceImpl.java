package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.AnalysisTaskMapper;
import com.wei.pojo.AnalysisTask;
import com.wei.pojo.enums.TaskSource;
import com.wei.pojo.enums.TaskStatus;
import com.wei.pojo.enums.TaskType;
import com.wei.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: admin
 * @date: 2025/9/1
 */
@Service
public class TaskServiceImpl extends ServiceImpl<AnalysisTaskMapper, AnalysisTask> implements TaskService {

    @Override
    public AnalysisTask createSingleTask(Long userId, Long modelId, String taskName) {
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setModelId(modelId);
        task.setTaskName(taskName);
        task.setTaskType(TaskType.SINGLE);
        task.setSource(TaskSource.WEB);
        task.setStatus(TaskStatus.PENDING);
        task.setTotalCount(1);
        task.setSuccessCount(0);
        task.setFailCount(0);
        save(task);
        return task;
    }

    @Override
    public AnalysisTask createBatchTask(Long userId, Long modelId, Long fileId, String taskName) {
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setModelId(modelId);
        task.setFileId(fileId);
        task.setTaskName(taskName);
        task.setTaskType(TaskType.BATCH);
        task.setSource(TaskSource.WEB);
        task.setStatus(TaskStatus.PENDING);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        save(task);
        return task;
    }

    @Override
    public Page<AnalysisTask> getUserTasks(Long userId, int pageNum, int pageSize, String status, String taskName, String taskType, String startDate, String endDate) {
        Page<AnalysisTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnalysisTask> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AnalysisTask::getUserId, userId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AnalysisTask::getStatus, TaskStatus.fromCode(status));
        }
        if (taskName != null && !taskName.isEmpty()) {
            wrapper.like(AnalysisTask::getTaskName, taskName);
        }
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.eq(AnalysisTask::getTaskType, TaskType.fromCode(taskType));
        }
        if (startDate != null && !startDate.isEmpty()) {
            try {
                // Ensure proper datetime format parsing or string comparison
                // Since frontend sends YYYY-MM-DD, we can append time or use string comparison if DB supports it.
                // Safest is to append start of day time
                String startDateTime = startDate.contains(" ") ? startDate : startDate + " 00:00:00";
                wrapper.ge(AnalysisTask::getCreatedAt, startDateTime);
            } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                // Append end of day time
                String endDateTime = endDate.contains(" ") ? endDate : endDate + " 23:59:59";
                wrapper.le(AnalysisTask::getCreatedAt, endDateTime);
            } catch (Exception ignored) {}
        }
        wrapper.orderByDesc(AnalysisTask::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void updateTaskStatus(Long taskId, String status, Integer successCount, Integer failCount, Integer durationMs) {
        AnalysisTask task = getById(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.fromCode(status));
            if (successCount != null) {
                task.setSuccessCount(successCount);
            }
            if (failCount != null) {
                task.setFailCount(failCount);
            }
            if (durationMs != null) {
                task.setDurationMs(durationMs);
            }
            if (TaskStatus.FINISHED.getCode().equals(status) || TaskStatus.FAILED.getCode().equals(status)) {
                task.setFinishedAt(LocalDateTime.now());
            }
            updateById(task);
        }
    }
}
