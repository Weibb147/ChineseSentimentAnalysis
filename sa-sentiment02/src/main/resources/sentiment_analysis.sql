/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50742 (5.7.42)
 Source Host           : localhost:3306
 Source Schema         : bishe003

 Target Server Type    : MySQL
 Target Server Version : 50742 (5.7.42)
 File Encoding         : 65001

 Date: 05/01/2026 01:05:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for analysis_result
-- ----------------------------
DROP TABLE IF EXISTS `analysis_result`;
CREATE TABLE `analysis_result`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `task_id` bigint(20) NOT NULL COMMENT '所属任务ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文本',
  `predicted_label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '预测情感标签 (VARCHAR以支持不同模型)',
  `probability_json` json NULL COMMENT '各类别概率分布 (e.g., {\"happy\": 0.9, ...})',
  `keywords_json` json NULL COMMENT '关键词（用于词云） (e.g., [\"开心\", \"哈哈\"])',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_result_task`(`task_id`) USING BTREE,
  INDEX `idx_result_label`(`predicted_label`) USING BTREE,
  CONSTRAINT `fk_result_task` FOREIGN KEY (`task_id`) REFERENCES `analysis_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 166 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '6. 分析结果表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for analysis_task
-- ----------------------------
DROP TABLE IF EXISTS `analysis_task`;
CREATE TABLE `analysis_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint(20) NOT NULL COMMENT '提交任务的用户ID',
  `model_id` bigint(20) NOT NULL COMMENT '使用的模型ID',
  `file_id` bigint(20) NULL DEFAULT NULL COMMENT '关联的文件ID (批量任务)',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '未命名任务' COMMENT '任务名称 (用户自定义)',
  `task_type` enum('SINGLE','BATCH') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务类型 (单条/批量)',
  `source` enum('WEB','API') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'WEB' COMMENT '任务来源 (网页/API调用)',
  `status` enum('PENDING','RUNNING','FINISHED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `total_count` int(11) NULL DEFAULT 0 COMMENT '总文本数',
  `success_count` int(11) NULL DEFAULT 0 COMMENT '成功分析数',
  `fail_count` int(11) NULL DEFAULT 0 COMMENT '失败数',
  `duration_ms` int(11) NULL DEFAULT NULL COMMENT '任务耗时(毫秒)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_user`(`user_id`) USING BTREE,
  INDEX `idx_task_model`(`model_id`) USING BTREE,
  INDEX `idx_task_file`(`file_id`) USING BTREE,
  INDEX `idx_task_status`(`status`) USING BTREE,
  CONSTRAINT `fk_task_file` FOREIGN KEY (`file_id`) REFERENCES `file_upload` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_task_model` FOREIGN KEY (`model_id`) REFERENCES `model_info` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 152 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '5. 分析任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for file_upload
-- ----------------------------
DROP TABLE IF EXISTS `file_upload`;
CREATE TABLE `file_upload`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `user_id` bigint(20) NOT NULL COMMENT '上传用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务器存储路径',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'text/csv' COMMENT '文件类型 (MIME Type)',
  `file_size_kb` int(11) NULL DEFAULT NULL COMMENT '文件大小(KB)',
  `status` enum('UPLOADED','PARSED','DELETED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'UPLOADED' COMMENT '文件状态',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id`) USING BTREE,
  CONSTRAINT `file_upload_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '4. 上传文件记录表 (批量任务)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for model_info
-- ----------------------------
DROP TABLE IF EXISTS `model_info`;
CREATE TABLE `model_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称 (如: RoBERTa-wwm-ext-SMP2020)',
  `model_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型基座 (如: RoBERTa, BERT)',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型版本号 (如: v1.0)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '模型描述 (如: 基于SMP2020数据集训练)',
  `model_file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型文件存储路径 (e.g., /opt/models/roberta_v1/)',
  `status` enum('ACTIVE','INACTIVE','TRAINING','DEPRECATED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'INACTIVE' COMMENT '模型状态 (ACTIVE=启用, INACTIVE=未启用)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '2. 模型信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容 (支持HTML或Markdown)',
  `type` enum('SYSTEM','UPDATE','EVENT','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'SYSTEM' COMMENT '公告类型',
  `is_top` tinyint(1) NULL DEFAULT 0 COMMENT '是否置顶 (1=是, 0=否)',
  `status` enum('VISIBLE','HIDDEN') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'VISIBLE' COMMENT '状态 (VISIBLE=可见, HIDDEN=隐藏)',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '发布人ID (管理员)',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `created_by`(`created_by`) USING BTREE,
  CONSTRAINT `notice_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '9. 系统公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名 (登录凭证)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加密后的密码 (建议使用bcrypt)',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱 (用于找回密码)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `sex` enum('男','女','保密') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '保密' COMMENT '性别',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像地址',
  `role` enum('ADMIN','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'USER' COMMENT '角色',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '1. 用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_feedback
-- ----------------------------
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '反馈用户ID (允许匿名)',
  `category` enum('BUG','SUGGESTION','EXPERIENCE','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'OTHER' COMMENT '反馈类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '反馈内容',
  `status` enum('PENDING','IN_PROGRESS','RESOLVED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `admin_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '管理员回复',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `replied_at` datetime NULL DEFAULT NULL COMMENT '回复时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id`) USING BTREE,
  CONSTRAINT `user_feedback_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '10. 用户反馈表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
