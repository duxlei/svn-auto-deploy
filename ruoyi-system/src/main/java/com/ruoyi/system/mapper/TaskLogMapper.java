package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.TaskLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发布日志 数据层
 * 
 * @author ruoyi
 */
public interface TaskLogMapper {

    /**
     * 更具发布任务Id 查询发布日志列表
     */
    List<TaskLog> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 新增任务日志
     */
    int insert(TaskLog task);

    /**
     * 批量新增任务日志
     */
    int batchInsert(@Param("tasks") List<TaskLog> tasks);

    /**
     * 更新任务日志
     */
    int update(TaskLog task);

}
