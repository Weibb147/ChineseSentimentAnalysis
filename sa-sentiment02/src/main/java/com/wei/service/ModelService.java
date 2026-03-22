package com.wei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wei.pojo.ModelInfo;

/**
 * @author: admin
 * @date: 2025/9/1
 */
public interface ModelService extends IService<ModelInfo> {
    /**
     * 分页查询模型列表
     */
    Page<ModelInfo> getModelList(int pageNum, int pageSize, String status);

    /**
     * 获取激活的模型
     */
    ModelInfo getActiveModel();

    /**
     * 创建模型
     */
    ModelInfo createModel(String modelName, String modelType, String version, String description, String modelFilePath);

    /**
     * 激活模型
     */
    void activateModel(Long id);

    /**
     * 停用模型
     */
    void deactivateModel(Long id);
}
