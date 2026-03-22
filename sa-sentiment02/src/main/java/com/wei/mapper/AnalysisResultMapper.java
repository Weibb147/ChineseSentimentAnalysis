package com.wei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wei.pojo.AnalysisResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisResultMapper extends BaseMapper<AnalysisResult> {
    
    @Select("SELECT predicted_label as label, COUNT(*) as count FROM analysis_result GROUP BY predicted_label")
    List<Map<String, Object>> countByLabel();
    
    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM analysis_result GROUP BY DATE(created_at) ORDER BY date DESC LIMIT 30")
    List<Map<String, Object>> countByDate();

    @Select("SELECT predicted_label as label, COUNT(*) as count FROM analysis_result WHERE (#{startDate} IS NULL OR created_at >= #{startDate}) AND (#{endDate} IS NULL OR created_at <= #{endDate}) GROUP BY predicted_label")
    List<Map<String, Object>> countByLabelRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM analysis_result WHERE (#{startDate} IS NULL OR created_at >= #{startDate}) AND (#{endDate} IS NULL OR created_at <= #{endDate}) GROUP BY DATE(created_at) ORDER BY date DESC")
    List<Map<String, Object>> countByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT r.id, r.content, r.predicted_label as predictedLabel, r.created_at as createdAt, r.keywords_json as keywordsJson, r.probability_json as probabilityJson, u.username, t.task_name as taskName, " +
            "CASE WHEN t.task_type = 'BATCH' THEN 'BATCH' ELSE 'SINGLE' END as taskType, " +
            "t.id as taskId " +
            "FROM analysis_result r " +
            "LEFT JOIN analysis_task t ON r.task_id = t.id " +
            "LEFT JOIN user u ON t.user_id = u.id " +
            "WHERE (#{startDate} IS NULL OR r.created_at >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR r.created_at <= #{endDate}) " +
            "ORDER BY r.created_at DESC")
    List<Map<String, Object>> selectVisualizationDataMap(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT r.id, r.content, r.predicted_label as predictedLabel, r.created_at as createdAt, r.keywords_json as keywordsJson, r.probability_json as probabilityJson, u.username, t.task_name as taskName, " +
            "CASE WHEN t.task_type = 'BATCH' THEN 'BATCH' ELSE 'SINGLE' END as taskType, " +
            "t.id as taskId " +
            "FROM analysis_result r " +
            "LEFT JOIN analysis_task t ON r.task_id = t.id " +
            "LEFT JOIN user u ON t.user_id = u.id " +
            "WHERE t.user_id = #{userId} " +
            "AND (#{startDate} IS NULL OR r.created_at &gt;= #{startDate}) " +
            "AND (#{endDate} IS NULL OR r.created_at &lt;= #{endDate}) " +
            "ORDER BY r.created_at DESC" +
            "</script>")
    List<Map<String, Object>> selectUserVisualizationDataMap(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
