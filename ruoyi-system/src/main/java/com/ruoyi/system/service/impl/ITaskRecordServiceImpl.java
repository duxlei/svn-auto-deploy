/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.mapper.TaskRecordMapper;
import com.ruoyi.system.service.ITaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author duhg
 * @date 2022/10/28 19:42
 */
@Service
public class ITaskRecordServiceImpl implements ITaskRecordService {

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    @Override
    public List<TaskRecord> selectList(TaskRecordQueryVo queryVo) {
        return taskRecordMapper.selectList(queryVo);
    }

    @Override
    public int saveDll(Long id, String outDlls) {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setId(id);
        taskRecord.setOutDlls(outDlls);
        return taskRecordMapper.updateById(taskRecord);
    }
}
