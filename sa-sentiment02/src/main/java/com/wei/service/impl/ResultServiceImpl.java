package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.AnalysisResultMapper;
import com.wei.pojo.AnalysisResult;
import com.wei.pojo.enums.SentimentLabel;
import com.wei.service.ResultService;
import org.springframework.stereotype.Service;

import com.wei.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.List;

/**
 * @author: admin
 * @date: 2025/9/1
 */
@Service
public class ResultServiceImpl extends ServiceImpl<AnalysisResultMapper, AnalysisResult> implements ResultService {

    @Override
    public AnalysisResult saveResult(Long taskId, String content, String predictedLabel, String probabilityJson, String keywordsJson) {
        AnalysisResult result = new AnalysisResult();
        result.setTaskId(taskId);
        result.setContent(content);
        result.setPredictedLabel(predictedLabel);
        result.setProbabilityJson(probabilityJson);
        result.setKeywordsJson(keywordsJson);
        save(result);
        return result;
    }

    @Override
    public void batchSaveResults(List<AnalysisResult> results) {
        saveBatch(results);
    }

    @Override
    public Page<AnalysisResult> getTaskResults(Long taskId, int pageNum, int pageSize) {
        Page<AnalysisResult> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnalysisResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisResult::getTaskId, taskId)
                .orderByDesc(AnalysisResult::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public List<AnalysisResult> getResultsByLabel(Long taskId, String label) {
        LambdaQueryWrapper<AnalysisResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisResult::getTaskId, taskId)
                .eq(AnalysisResult::getPredictedLabel, label);
        return list(wrapper);
    }

    @Override
    public Page<AnalysisResult> getAllResults(int pageNum, int pageSize) {
        Page<AnalysisResult> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnalysisResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AnalysisResult::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public Page<AnalysisResult> getResultsByDateRange(String startDate, String endDate, int pageNum, int pageSize) {
        Page<AnalysisResult> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnalysisResult> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AnalysisResult::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AnalysisResult::getCreatedAt, endDate);
        }
        wrapper.orderByDesc(AnalysisResult::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public List<java.util.Map<String, Object>> getSentimentStats() {
        return baseMapper.countByLabel();
    }

    @Override
    public List<java.util.Map<String, Object>> getTrendStats() {
        return baseMapper.countByDate();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getSentimentStats(String startDate, String endDate) {
        boolean noRange = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty());
        if (noRange) {
            return baseMapper.countByLabel();
        }
        return baseMapper.countByLabelRange(startDate, endDate);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTrendStats(String startDate, String endDate) {
        boolean noRange = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty());
        if (noRange) {
            return baseMapper.countByDate();
        }
        return baseMapper.countByDateRange(startDate, endDate);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTopKeywords(String startDate, String endDate, int topK) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnalysisResult> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AnalysisResult::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AnalysisResult::getCreatedAt, endDate);
        }
        wrapper.select(AnalysisResult::getKeywordsJson);
        java.util.List<AnalysisResult> list = list(wrapper);

        java.util.Map<String, Integer> freq = new java.util.HashMap<>();
        for (AnalysisResult r : list) {
            String kj = r.getKeywordsJson();
            if (kj == null || kj.isEmpty() || kj.equals("[]")) {
                continue;
            }
            try {
                java.util.Map<String, Integer> m = JsonUtils.fromJson(kj, new TypeReference<java.util.Map<String, Integer>>() {});
                for (java.util.Map.Entry<String, Integer> e : m.entrySet()) {
                    String k = e.getKey();
                    if (k == null || k.isEmpty()) {
                        continue;
                    }
                    int v = e.getValue() != null ? e.getValue() : 0;
                    freq.put(k, freq.getOrDefault(k, 0) + v);
                }
            } catch (Exception ignored) {
            }
        }

        java.util.List<java.util.Map<String, Object>> entries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : freq.entrySet()) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            entries.add(item);
        }
        entries.sort((a, b) -> Integer.compare(((Number) b.get("value")).intValue(), ((Number) a.get("value")).intValue()));
        if (topK > 0 && entries.size() > topK) {
            return entries.subList(0, topK);
        }
        return entries;
    }

    @Override
    public java.util.List<com.wei.pojo.vo.VisualizationDataVO> getVisualizationData(String startDate, String endDate) {
        List<Map<String, Object>> list = baseMapper.selectVisualizationDataMap(startDate, endDate);
        return mapToVisualizationDataVO(list);
    }

    @Override
    public java.util.List<com.wei.pojo.vo.VisualizationDataVO> getUserVisualizationData(Long userId, String startDate, String endDate) {
        List<Map<String, Object>> list = baseMapper.selectUserVisualizationDataMap(userId, startDate, endDate);
        return mapToVisualizationDataVO(list);
    }

    private List<com.wei.pojo.vo.VisualizationDataVO> mapToVisualizationDataVO(List<Map<String, Object>> list) {
        return list.stream().map(map -> {
            com.wei.pojo.vo.VisualizationDataVO vo = new com.wei.pojo.vo.VisualizationDataVO();
            vo.setId((Long) map.get("id"));
            vo.setTaskId((Long) map.get("taskId"));
            
            // Handle taskName with fallback for map keys
            String taskName = (String) map.get("taskName");
            if (taskName == null) {
                taskName = (String) map.get("task_name");
            }
            vo.setTaskName(taskName);
            
            // Handle taskType with fallback for map keys and explicit logic
            Object taskTypeObj = map.get("taskType");
            if (taskTypeObj == null) {
                taskTypeObj = map.get("task_type");
            }
            
            String taskTypeStr = "SINGLE";
            if (taskTypeObj != null) {
                taskTypeStr = taskTypeObj.toString().trim();
            }
            
            // Double check based on file extension if taskName is present
            // This is a robust fallback for batch tasks that might be mislabeled
            if (taskName != null) {
                String lowerName = taskName.toLowerCase();
                if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv") || lowerName.endsWith(".txt")) {
                    taskTypeStr = "BATCH";
                }
            }
            
            vo.setTaskType(taskTypeStr);
            
            vo.setContent((String) map.get("content"));
            vo.setPredictedLabel((String) map.get("predictedLabel"));
            
            Object createdAtObj = map.get("createdAt");
            if (createdAtObj instanceof java.sql.Timestamp) {
                vo.setCreatedAt(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
            } else if (createdAtObj instanceof java.time.LocalDateTime) {
                vo.setCreatedAt((java.time.LocalDateTime) createdAtObj);
            }
            
            vo.setUsername((String) map.get("username"));

            String keywordsJson = (String) map.get("keywordsJson");
            if (keywordsJson != null && !keywordsJson.equals("[]")) {
                try {
                    vo.setKeywords(JsonUtils.fromJson(keywordsJson, new TypeReference<Map<String, Integer>>(){}));
                } catch (Exception ignored) {}
            }
            
            String probabilityJson = (String) map.get("probabilityJson");
            if (probabilityJson != null && !probabilityJson.equals("[]")) {
                try {
                    vo.setProbability(JsonUtils.fromJson(probabilityJson, new TypeReference<Map<String, Double>>(){}));
                } catch (Exception ignored) {}
            }

            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }
}
