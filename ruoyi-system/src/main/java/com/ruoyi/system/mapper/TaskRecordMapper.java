package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.DeployConfig;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发布任务记录 数据层
 * 
 * @author ruoyi
 */
public interface TaskRecordMapper {

    /**
     * 查询列表数据
     */
    List<TaskRecord> selectList(TaskRecordQueryVo query);

    /**
     * 根据ID查询信息
     */
    TaskRecord selectById(@Param("taskId") Long taskId);

    /**
     * 新增任务
     */
    int insert(TaskRecord task);

    /**
     * 批量新增任务
     */
    int batchInsert(@Param("tasks") List<TaskRecord> tasks);

    /**
     * 更具Jira号修改任务信息
     */
    int updateByJiraNo(TaskRecord task);

    /**
     * 更具Id修改任务信息
     */
    int updateById(TaskRecord task);

    /** 批量更新任务 */
    int batchUpdate(@Param("tasks") List<TaskRecord> tasks, @Param("env") String env);

    /** 保存配置信息 */
    int saveConfig(DeployConfig config);

    /** 查询配置信息 */
    DeployConfig selectConfig();
}
