package com.wei.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.BeanConverter;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.AnalysisResult;
import com.wei.pojo.vo.ResultVO;
import com.wei.service.FastApiService;
import com.wei.service.ResultService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.wei.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analysis")
public class AdminAnalysisController {

    @Autowired
    private ResultService resultService;

    @Autowired
    private FastApiService fastApiService;

    @GetMapping("/results")
    public ResultUtils<Page<ResultVO>> getAllResults(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate) {
        Page<AnalysisResult> page = (startDate != null || endDate != null)
                ? resultService.getResultsByDateRange(startDate, endDate, pageNum, pageSize)
                : resultService.getAllResults(pageNum, pageSize);

        Page<ResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(BeanConverter::toResultVO).toList());

        return ResultUtils.success(voPage);
    }

    @GetMapping("/stats/sentiment")
    public ResultUtils<Object> sentimentStats(@RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        java.util.List<java.util.Map<String, Object>> data = resultService.getSentimentStats(startDate, endDate);
        return ResultUtils.success(data);
    }

    @GetMapping("/stats/trend")
    public ResultUtils<Object> trendStats(@RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate) {
        java.util.List<java.util.Map<String, Object>> data = resultService.getTrendStats(startDate, endDate);
        return ResultUtils.success(data);
    }

    @GetMapping("/stats/all")
    public ResultUtils<Object> allStats(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false, defaultValue = "50") Integer topK) {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("sentiment", resultService.getSentimentStats(startDate, endDate));
        res.put("trend", resultService.getTrendStats(startDate, endDate));
        res.put("keywords", resultService.getTopKeywords(startDate, endDate, topK != null ? topK : 50));
        return ResultUtils.success(res);
    }

    @PostMapping("/overview")
    public ResultUtils<Object> overview(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> contents = (List<String>) body.getOrDefault("contents", java.util.List.of());
            @SuppressWarnings("unchecked")
            List<String> dates = (List<String>) body.getOrDefault("dates", java.util.List.of());
            String modelName = (String) body.getOrDefault("modelName", "roberta_base");

            Mono<Map<String, Object>> resMono = fastApiService.overview(contents, dates, modelName);
            Map<String, Object> res = resMono.block();
            return ResultUtils.success(res);
        } catch (Exception e) {
            return ResultUtils.error("概览计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization-data")
    public ResultUtils<List<com.wei.pojo.vo.VisualizationDataVO>> getVisualizationData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResultUtils.success(resultService.getVisualizationData(startDate, endDate));
    }

}
