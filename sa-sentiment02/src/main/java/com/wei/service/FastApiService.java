package com.wei.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * FastAPI模型服务客户端
 * 负责与FastAPI模型服务进行HTTP通信
 */
@Service
public class FastApiService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;
    
    @Value("${fastapi.timeout:30}")
    private int timeoutSeconds;
    
    public FastApiService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 单条文本情感分析
     * @param content 文本内容
     * @param modelName 模型名称
     * @return 分析结果
     */
    public Mono<Map<String, Object>> analyzeSingle(String content, String modelName) {
        Map<String, Object> requestBody = Map.of(
            "content", content,
            "modelName", modelName != null ? modelName : "roberta_base"
        );
        
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/analysis/single")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }
    
    /**
     * 批量文本情感分析
     * @param contents 文本内容列表
     * @param modelName 模型名称
     * @return 批量分析结果
     */
    public Mono<Map<String, Object>> analyzeBatch(List<String> contents, String modelName) {
        Map<String, Object> requestBody = Map.of(
            "contents", contents,
            "modelName", modelName != null ? modelName : "roberta_base"
        );
        
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/analysis/batch")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds * 2)); // 批量分析超时时间更长
    }

    public Mono<Map<String, Object>> extractKeywords(String content, Integer topK) {
        Map<String, Object> requestBody = Map.of(
            "content", content,
            "topK", topK != null ? topK : 20
        );
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/keywords/extract")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<Map<String, Object>> wordCloud() {
        return webClient.get()
                .uri(fastApiBaseUrl + "/api/visualization/wordcloud")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<Map<String, Object>> wordCloud(Integer topK, String startDate, String endDate) {
        StringBuilder uri = new StringBuilder(fastApiBaseUrl + "/api/visualization/wordcloud");
        java.util.List<String> params = new java.util.ArrayList<>();
        if (topK != null) params.add("top_k=" + topK);
        if (startDate != null && !startDate.isBlank()) params.add("start_date=" + startDate);
        if (endDate != null && !endDate.isBlank()) params.add("end_date=" + endDate);
        if (!params.isEmpty()) {
            uri.append("?").append(String.join("&", params));
        }
        return webClient.get()
                .uri(uri.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<Map<String, Object>> sentimentPie(List<String> contents, String modelName) {
        Map<String, Object> requestBody = Map.of(
            "contents", contents,
            "modelName", modelName != null ? modelName : "roberta_base"
        );
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/visualization/sentiment_pie")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<Map<String, Object>> lineTrend(List<String> dates) {
        Map<String, Object> requestBody = Map.of(
            "dates", dates
        );
        return webClient.post()
                .uri(fastApiBaseUrl + "/api/visualization/line_trend")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<Map<String, Object>> overview(List<String> contents, List<String> dates, String modelName) {
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("contents", contents);
        if (dates != null) {
            requestBody.put("dates", dates);
        }
        requestBody.put("modelName", modelName != null ? modelName : "roberta_base");
        
        String url = fastApiBaseUrl + "/api/visualization/overview";
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(e -> {
                    // Log the detailed error including URL
                    System.err.println("Error calling FastAPI overview endpoint: " + url + ". Error: " + e.getMessage());
                    return Mono.error(new RuntimeException("调用模型概览接口失败: " + e.getMessage()));
                });
    }
    
    /**
     * 获取可用模型列表
     * @return 模型列表
     */
    public Mono<List<Map<String, Object>>> getModels() {
        return webClient.get()
                .uri(fastApiBaseUrl + "/api/models")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseModelList)
                .timeout(java.time.Duration.ofSeconds(10));
    }
    
    /**
     * 健康检查
     * @return 健康状态
     */
    public Mono<Map<String, Object>> healthCheck() {
        return webClient.get()
                .uri(fastApiBaseUrl + "/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .timeout(java.time.Duration.ofSeconds(5));
    }
    
    /**
     * 解析FastAPI响应
     * @param response 响应字符串
     * @return 解析后的Map
     */
    private Map<String, Object> parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("解析FastAPI响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析模型列表响应
     * @param response 响应字符串
     * @return 模型列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseModelList(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, List.class);
        } catch (Exception e) {
            throw new RuntimeException("解析模型列表响应失败: " + e.getMessage(), e);
        }
    }
}
