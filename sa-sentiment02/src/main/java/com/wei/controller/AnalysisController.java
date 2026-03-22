package com.wei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.AuthContext;
import com.wei.common.utils.BeanConverter;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.AnalysisResult;
import com.wei.pojo.AnalysisTask;
import com.wei.pojo.ModelInfo;
import com.wei.pojo.dto.AnalysisRequestDTO;
import com.wei.pojo.dto.BatchAnalysisDTO;
import com.wei.pojo.vo.ResultVO;
import com.wei.pojo.vo.TaskVO;
import com.wei.pojo.vo.VisualizationDataVO;
import com.wei.service.FastApiService;
import com.wei.service.ModelService;
import com.wei.service.ResultService;
import com.wei.service.TaskService;
import com.wei.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.wei.common.utils.JsonUtils;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ModelService modelService;
    
    @Autowired
    private FastApiService fastApiService;
    @Autowired
    private com.wei.service.FileUploadService fileUploadService;
    @Autowired
    private UserService userService;

    /**
     * 单条文本分析
     */
    @PostMapping("/single")
    public ResultUtils<TaskVO> analyzeSingle(@Valid @RequestBody AnalysisRequestDTO dto) {
        Long userId = AuthContext.getCurrentUserId();

        // 获取模型名称
        String modelName = dto.getModelName();
        Long modelId = dto.getModelId();

        // 如果没有指定模型名称和ID，尝试使用激活的模型
        if ((modelName == null || modelName.trim().isEmpty()) && modelId == null) {
            ModelInfo activeModel = modelService.getActiveModel();
            if (activeModel != null) {
                modelId = activeModel.getId();
                // 优先使用 modelType (对应后端模型标识)
                if (activeModel.getModelType() != null && !activeModel.getModelType().isEmpty()) {
                    modelName = activeModel.getModelType();
                } else {
                    modelName = activeModel.getModelName();
                }
            } else {
                modelName = "roberta_base";
            }
        } else if (modelName == null || modelName.trim().isEmpty()) {
            // 如果只有ID
            if (modelId != null) {
                 ModelInfo modelInfo = modelService.getById(modelId);
                 if (modelInfo != null) {
                     modelName = modelInfo.getModelType();
                 } else {
                     modelName = "roberta_base";
                 }
            } else {
                modelName = "roberta_base";
            }
        }
        

        modelName = mapModelIdToName(modelName);

        // 如果还没有modelId（比如直接传了名称），尝试查找或使用默认
        if (modelId == null) {
            // 简单的映射逻辑，实际应该查询数据库
            modelId = mapModelNameToId(modelName);
        }

        // 创建任务
        AnalysisTask task = taskService.createSingleTask(userId, modelId, dto.getTaskName());

        try {
            // 调用FastAPI模型服务进行分析
            Mono<Map<String, Object>> resultMono = fastApiService.analyzeSingle(dto.getContent(), modelName);
            Map<String, Object> result = resultMono.block(); // 同步等待结果

            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                // 提取预测结果
                String predictedLabel = (String) result.get("predictedLabel");
                String probabilityJson = (String) result.get("probabilityJson");
                String keywordsJson = (String) result.get("keywordsJson");

                // 保存结果
                resultService.saveResult(task.getId(), dto.getContent(), predictedLabel, probabilityJson, keywordsJson);

                // 更新任务状态为成功
                taskService.updateTaskStatus(task.getId(), "FINISHED", 1, 0, 100);
            } else {
                // 处理模型服务返回的错误
                String errorMessage = result != null ? (String) result.get("errorMessage") : "模型服务调用失败";
                taskService.updateTaskStatus(task.getId(), "FAILED", 0, 1, 0);
                return ResultUtils.error("情感分析失败: " + errorMessage);
            }
        } catch (Exception e) {
            // 处理调用异常
            taskService.updateTaskStatus(task.getId(), "FAILED", 0, 1, 0);
            return ResultUtils.error("调用模型服务失败: " + e.getMessage());
        }

        return ResultUtils.success(BeanConverter.toTaskVO(taskService.getById(task.getId())));
    }

    @PostMapping("/batch")
    public ResultUtils<TaskVO> analyzeBatch(@Valid @RequestBody BatchAnalysisDTO dto) {
        Long userId = AuthContext.getCurrentUserId();

        // 获取模型
        Long modelId = dto.getModelId();
        if (modelId == null) {
            ModelInfo activeModel = modelService.getActiveModel();
            if (activeModel == null) {
                return ResultUtils.error("没有可用的模型");
            }
            modelId = activeModel.getId();
        }

        // 创建批量任务
        AnalysisTask task = taskService.createBatchTask(userId, modelId, dto.getFileId(), dto.getTaskName());
        try {
            com.wei.pojo.FileUpload file = fileUploadService.getById(dto.getFileId());
            if (file == null || file.getFilePath() == null) {
                taskService.updateTaskStatus(task.getId(), "FAILED", 0, 0, 0);
                return ResultUtils.error("文件不存在");
            }

            java.util.List<String> contents = new java.util.ArrayList<>();
            try {
                java.net.URL url = new java.net.URL(file.getFilePath());
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String t = line.trim();
                        if (!t.isEmpty()) {
                            contents.add(t);
                        }
                    }
                }
            } catch (Exception e) {
                taskService.updateTaskStatus(task.getId(), "FAILED", 0, 0, 0);
                return ResultUtils.error("读取文件失败: " + e.getMessage());
            }

            task.setTotalCount(contents.size());
            taskService.updateById(task);

            String modelName;
            ModelInfo modelInfo = modelService.getById(modelId);
            if (modelInfo != null && modelInfo.getModelType() != null) {
                modelName = mapModelIdToName(modelInfo.getModelType());
            } else {
                modelName = "roberta_base";
            }

            Mono<Map<String, Object>> resMono = fastApiService.analyzeBatch(contents, modelName);
            Map<String, Object> res = resMono.block();
            java.util.List<AnalysisResult> resultsToSave = new java.util.ArrayList<>();
            int success = 0;
            int fail = 0;

            if (res != null && Boolean.TRUE.equals(res.get("success"))) {
                Object r = res.get("results");
                if (r instanceof java.util.List<?> list) {
                    for (Object o : list) {
                        if (o instanceof java.util.Map<?, ?> m) {
                            String content = String.valueOf(m.get("content"));
                            String predictedLabel = String.valueOf(m.get("predictedLabel"));
                            String probabilityJson = String.valueOf(m.get("probabilityJson"));
                            String keywordsJson = String.valueOf(m.get("keywordsJson"));
                            if (predictedLabel != null && !predictedLabel.isEmpty()) {
                                AnalysisResult ar = new AnalysisResult();
                                ar.setTaskId(task.getId());
                                ar.setContent(content);
                                ar.setPredictedLabel(predictedLabel);
                                ar.setProbabilityJson(probabilityJson);
                                ar.setKeywordsJson(keywordsJson);
                                resultsToSave.add(ar);
                                success++;
                            } else {
                                fail++;
                            }
                        } else {
                            fail++;
                        }
                    }
                }
            } else {
                taskService.updateTaskStatus(task.getId(), "FAILED", 0, contents.size(), 0);
                TaskVO vo = new TaskVO();
                BeanUtils.copyProperties(taskService.getById(task.getId()), vo);
                return ResultUtils.success(vo);
            }

            if (!resultsToSave.isEmpty()) {
                resultService.batchSaveResults(resultsToSave);
            }

            taskService.updateTaskStatus(task.getId(), "FINISHED", success, fail, 100);
        } catch (Exception e) {
            taskService.updateTaskStatus(task.getId(), "FAILED", 0, 0, 0);
            return ResultUtils.error("批量分析失败: " + e.getMessage());
        }

        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(taskService.getById(task.getId()), vo);
        return ResultUtils.success(vo);
    }

    /**
     * 查询任务列表
     */
    @GetMapping("/tasks")
    public ResultUtils<Page<TaskVO>> getTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long currentUserId = AuthContext.getCurrentUserId();
        String role = AuthContext.getCurrentUserRole();
        
        // If admin, use provided userId (filter) or null (all)
        // If regular user, always query their own tasks
        Long queryUserId = "ADMIN".equals(role) ? userId : currentUserId;
        
        Page<AnalysisTask> page = taskService.getUserTasks(queryUserId, pageNum, pageSize, status, taskName, taskType, startDate, endDate);

        // Pre-fetch models to avoid N+1
        java.util.Map<Long, String> modelNameMap = new java.util.HashMap<>();
        try {
            modelService.list().forEach(m -> modelNameMap.put(m.getId(), m.getModelName()));
        } catch (Exception ignored) {}

        Page<TaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(task -> {
            TaskVO vo = BeanConverter.toTaskVO(task);
            
            // 设置模型名称
            if (task.getModelId() != null) {
                vo.setModelName(modelNameMap.getOrDefault(task.getModelId(), "未知模型"));
            }

            try {
                com.wei.pojo.vo.UserVO userVO = userService.getProfile(task.getUserId());
                if (userVO != null) {
                    vo.setUsername(userVO.getUsername());
                }
            } catch (Exception ignored) {}
            return vo;
        }).toList());

        //打印任务列表

        System.out.println("voPage："+voPage);
        return ResultUtils.success(voPage);
    }

    /**
     * 删除任务
     */
    @RequestMapping(value = "/tasks/{taskId}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResultUtils<String> deleteTask(@PathVariable Long taskId) {
        Long currentUserId = AuthContext.getCurrentUserId();
        String role = AuthContext.getCurrentUserRole();
        
        AnalysisTask task = taskService.getById(taskId);
        if (task == null) {
            return ResultUtils.error("任务不存在");
        }
        
        if (!"ADMIN".equals(role) && !task.getUserId().equals(currentUserId)) {
            return ResultUtils.error("无权删除该任务");
        }
        
        taskService.removeById(taskId);
        resultService.remove(new QueryWrapper<AnalysisResult>().eq("task_id", taskId));
        
        return ResultUtils.success("删除成功");
    }

    /**
     * 删除单条分析结果
     */
    @RequestMapping(value = "/results/{resultId}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResultUtils<String> deleteResult(@PathVariable Long resultId) {
        Long currentUserId = AuthContext.getCurrentUserId();
        String role = AuthContext.getCurrentUserRole();

        AnalysisResult result = resultService.getById(resultId);
        if (result == null) {
            return ResultUtils.error("记录不存在");
        }

        AnalysisTask task = taskService.getById(result.getTaskId());
        
        // Check permission
        if (task != null) {
            if (!"ADMIN".equalsIgnoreCase(role) && !task.getUserId().equals(currentUserId)) {
                return ResultUtils.error("无权删除该记录");
            }
        } else {
             // If task is missing (orphan result), only admin can delete
             if (!"ADMIN".equalsIgnoreCase(role)) {
                return ResultUtils.error("无权删除该记录");
            }
        }

        resultService.removeById(resultId);
        return ResultUtils.success("删除成功");
    }

    /**
     * 查询任务结果
     */
    @GetMapping("/results/{taskId}")
    public ResultUtils<Page<ResultVO>> getResults(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<AnalysisResult> page = resultService.getTaskResults(taskId, pageNum, pageSize);

        Page<ResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(BeanConverter::toResultVO).toList());

        return ResultUtils.success(voPage);
    }

    /**
     * 获取可视化数据
     */
    @GetMapping("/visualization-data")
    public ResultUtils<java.util.List<VisualizationDataVO>> getVisualizationData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = AuthContext.getCurrentUserId();
        return ResultUtils.success(resultService.getUserVisualizationData(userId, startDate, endDate));
    }
    
    /**
     * 验证模型名称是否有效
     */
    private boolean isValidModelName(String modelName) {
        if (modelName == null) {
            return false;
        }
        
        String normalized = modelName.toLowerCase().trim();
        return normalized.equals("roberta_base") ||
               normalized.equals("roberta_bilstm_attention") ||
               normalized.equals("roberta_gru_attention");
    }

    /**
     * 将模型名称映射到数据库ID
     */
    private Long mapModelNameToId(String modelName) {
        if (modelName == null) {
            return 1L; // 默认模型ID
        }
        
        switch (modelName.toLowerCase()) {
            case "roberta_base":
                return 1L; // RoBERTa Base
            case "roberta_bilstm_attention":
                return 2L; // RoBERTa + BiLSTM + Attention
            case "roberta_gru_attention":
                return 3L; // RoBERTa + GRU + Attention
            default:
                return 1L; // 默认使用第一个模型
        }
    }

    /**
     * 将模型ID或类型映射到FastAPI模型名称
     */
    private String mapModelIdToName(String modelType) {
        if (modelType == null) {
            return "roberta_base";
        }
        
        switch (modelType.toLowerCase()) {
            case "roberta_base":
            case "roberta":
            case "base":
                return "roberta_base";
            case "roberta_bilstm":
            case "bilstm":
            case "lstm":
            case "roberta_bilstm_attention":
                return "roberta_bilstm_attention";
            case "roberta_gru":
            case "gru":
            case "roberta_gru_attention":
                return "roberta_gru_attention";
            default:
                // 如果是其他类型，直接返回原值，允许扩展新模型
                return modelType;
        }
    }

    @PostMapping("/keywords/extract")
    public ResultUtils<Object> extractKeywords(@RequestBody Map<String, Object> body) {
        try {
            String content = (String) body.getOrDefault("content", "");
            Integer topK = body.get("topK") instanceof Number ? ((Number) body.get("topK")).intValue() : 20;
            Mono<Map<String, Object>> resMono = fastApiService.extractKeywords(content, topK);
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("关键词提取失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization/wordcloud")
    public ResultUtils<Object> wordCloud() {
        try {
            Mono<Map<String, Object>> resMono = fastApiService.wordCloud();
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("词云生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/visualization/sentiment_pie")
    public ResultUtils<Object> sentimentPie(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> contents = (java.util.List<String>) body.getOrDefault("contents", java.util.List.of());
            String modelName = (String) body.getOrDefault("modelName", "roberta_base");
            Mono<Map<String, Object>> resMono = fastApiService.sentimentPie(contents, modelName);
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("情绪分布计算失败: " + e.getMessage());
        }
    }

    @PostMapping("/visualization/line_trend")
    public ResultUtils<Object> lineTrend(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> dates = (java.util.List<String>) body.getOrDefault("dates", java.util.List.of());
            Mono<Map<String, Object>> resMono = fastApiService.lineTrend(dates);
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("趋势计算失败: " + e.getMessage());
        }
    }

    @PostMapping("/visualization/overview")
    public ResultUtils<Object> overview(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> contents = (java.util.List<String>) body.getOrDefault("contents", java.util.List.of());
            @SuppressWarnings("unchecked")
            java.util.List<String> dates = (java.util.List<String>) body.getOrDefault("dates", java.util.List.of());
            String modelName = (String) body.getOrDefault("modelName", "roberta_base");
            Mono<Map<String, Object>> resMono = fastApiService.overview(contents, dates, modelName);
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("概览计算失败: " + e.getMessage());
        }
    }
}
