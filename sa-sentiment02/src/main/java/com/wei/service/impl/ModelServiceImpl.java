package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wei.mapper.ModelInfoMapper;
import com.wei.pojo.ModelInfo;
import com.wei.pojo.enums.ModelStatus;
import com.wei.service.ModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: admin
 * @date: 2025/9/1
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelInfoMapper, ModelInfo> implements ModelService {

    @Override
    public Page<ModelInfo> getModelList(int pageNum, int pageSize, String status) {
        Page<ModelInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ModelInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ModelInfo::getStatus, ModelStatus.fromCode(status));
        }
        wrapper.orderByDesc(ModelInfo::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public ModelInfo getActiveModel() {
        LambdaQueryWrapper<ModelInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelInfo::getStatus, ModelStatus.ACTIVE)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public ModelInfo createModel(String modelName, String modelType, String version, String description, String modelFilePath) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setModelName(modelName);
        modelInfo.setModelType(modelType);
        modelInfo.setVersion(version);
        modelInfo.setDescription(description);
        modelInfo.setModelFilePath(modelFilePath);
        modelInfo.setStatus(ModelStatus.INACTIVE);
        save(modelInfo);
        return modelInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateModel(Long id) {
        // 修改为：只激活指定模型，不禁用其他模型（允许同时存在多个可用模型）
        lambdaUpdate()
            .eq(ModelInfo::getId, id)
            .set(ModelInfo::getStatus, ModelStatus.ACTIVE)
            .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateModel(Long id) {
        // 停用指定模型
        lambdaUpdate()
            .eq(ModelInfo::getId, id)
            .set(ModelInfo::getStatus, ModelStatus.INACTIVE)
            .update();
    }
}
