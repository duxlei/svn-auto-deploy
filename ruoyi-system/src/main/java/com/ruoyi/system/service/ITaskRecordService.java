package com.ruoyi.system.service;

import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;

import java.util.List;

/**
 * 发布任务 服务层
 * 
 * @author ruoyi
 */
public interface ITaskRecordService {

    /** 查询发布任务列表 */
    List<TaskRecord> selectList(TaskRecordQueryVo queryVo);

    /** 保存更新dlls */
    int saveDll(Long id, String outDlls);
}