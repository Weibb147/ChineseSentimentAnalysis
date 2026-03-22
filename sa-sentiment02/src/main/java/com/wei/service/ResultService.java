package com.wei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.AnalysisResult;

import java.util.List;

/**
 * @author: admin
 * @date: 2025/9/1
 */
public interface ResultService extends IService<AnalysisResult> {
    /**
     * 保存分析结果
     */
    AnalysisResult saveResult(Long taskId, String content, String predictedLabel, String probabilityJson, String keywordsJson);

    /**
     * 批量保存分析结果
     */
    void batchSaveResults(List<AnalysisResult> results);

    /**
     * 分页查询任务的结果
     */
    Page<AnalysisResult> getTaskResults(Long taskId, int pageNum, int pageSize);

    /**
     * 按标签统计任务结果
     */
    List<AnalysisResult> getResultsByLabel(Long taskId, String label);

    /**
     * 管理员分页查询全部结果
     */
    Page<AnalysisResult> getAllResults(int pageNum, int pageSize);

    /**
     * 管理员按日期范围查询结果（可选）
     */
    Page<AnalysisResult> getResultsByDateRange(String startDate, String endDate, int pageNum, int pageSize);

    /**
     * 获取情感分布统计
     */
    java.util.List<java.util.Map<String, Object>> getSentimentStats();

    /**
     * 获取每日趋势统计
     */
    java.util.List<java.util.Map<String, Object>> getTrendStats();

    /**
     * 获取情感分布统计（按日期范围）
     */
    java.util.List<java.util.Map<String, Object>> getSentimentStats(String startDate, String endDate);

    /**
     * 获取每日趋势统计（按日期范围）
     */
    java.util.List<java.util.Map<String, Object>> getTrendStats(String startDate, String endDate);

    /**
     * 获取关键词TopK（按日期范围）
     */
    java.util.List<java.util.Map<String, Object>> getTopKeywords(String startDate, String endDate, int topK);

    /**
     * 获取可视化所需的基础数据（标签、时间、关键词）
     */
    java.util.List<com.wei.pojo.vo.VisualizationDataVO> getVisualizationData(String startDate, String endDate);

    /**
     * 获取指定用户的可视化数据
     */
    java.util.List<com.wei.pojo.vo.VisualizationDataVO> getUserVisualizationData(Long userId, String startDate, String endDate);
}
