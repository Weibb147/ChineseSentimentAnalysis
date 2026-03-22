package com.wei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.ModelInfo;
import com.wei.pojo.dto.ModelCreateDTO;
import com.wei.service.FastApiService;
import com.wei.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 模型管理控制器
 */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private FastApiService fastApiService;

    /**
     * 获取模型列表 (分页, 数据库)
     */
    @GetMapping("/list")
    public ResultUtils<Page<ModelInfo>> getModelList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return ResultUtils.success(modelService.getModelList(pageNum, pageSize, status));
    }

    /**
     * 创建模型
     */
    @PostMapping
    public ResultUtils<ModelInfo> createModel(@Valid @RequestBody ModelCreateDTO dto) {
        ModelInfo model = modelService.createModel(
            dto.getModelName(), 
            dto.getModelType(), 
            dto.getVersion(), 
            dto.getDescription(), 
            dto.getModelFilePath()
        );
        return ResultUtils.success(model);
    }

    /**
     * 激活模型
     */
    @PutMapping("/{id}/activate")
    public ResultUtils<String> activateModel(@PathVariable Long id) {
        modelService.activateModel(id);
        return ResultUtils.success("激活成功");
    }

    /**
     * 停用模型
     */
    @PutMapping("/{id}/deactivate")
    public ResultUtils<String> deactivateModel(@PathVariable Long id) {
        modelService.deactivateModel(id);
        return ResultUtils.success("停用成功");
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public ResultUtils<String> deleteModel(@PathVariable Long id) {
        modelService.removeById(id);
        return ResultUtils.success("删除成功");
    }

    /**
     * 获取模型详细信息 (保留原有逻辑，用于获取模型类型的元数据)
     */
    @GetMapping("/info/{modelName}")
    public ResultUtils<Object> getModelInfo(@PathVariable String modelName) {
        try {
            return ResultUtils.success(getModelInfoByName(modelName));
        } catch (Exception e) {
            return ResultUtils.error("获取模型信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据模型名称获取模型信息（本地方法）
     */
    private Map<String, Object> getModelInfoByName(String modelName) {
        switch (modelName) {
            case "roberta_base":
                return Map.of(
                    "id", "roberta_base",
                    "name", "RoBERTa Base",
                    "description", "基础RoBERTa模型，准确可靠",
                    "speed", "fast",
                    "accuracy", "high",
                    "recommended", true
                );
            case "roberta_bilstm_attention":
                return Map.of(
                    "id", "roberta_bilstm_attention",
                    "name", "RoBERTa + BiLSTM + Attention",
                    "description", "结合BiLSTM和注意力机制，更准确",
                    "speed", "medium",
                    "accuracy", "very_high",
                    "recommended", false
                );
            case "roberta_gru_attention":
                return Map.of(
                    "id", "roberta_gru_attention",
                    "name", "RoBERTa + GRU + Attention",
                    "description", "结合GRU和注意力机制，快速准确",
                    "speed", "fast",
                    "accuracy", "high",
                    "recommended", false
                );
            default:
                return Map.of(
                    "id", modelName,
                    "name", modelName,
                    "description", "未知模型",
                    "speed", "unknown",
                    "accuracy", "unknown",
                    "recommended", false
                );
        }
    }
}
