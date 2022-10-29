/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.mapper.TaskRecordMapper;
import com.ruoyi.system.service.ITaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
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

    @Override
    public int addTask(TaskRecord taskRecord) {
        TaskRecordQueryVo query = new TaskRecordQueryVo();
        query.setJiraNo(taskRecord.getJiraNo());
        query.setEnv(taskRecord.getEnv());
        List<TaskRecord> records = taskRecordMapper.selectList(query);
        if (!CollectionUtils.isEmpty(records)) {
            return taskRecordMapper.updateByJiraNo(taskRecord);
        } else {
            return taskRecordMapper.insert(taskRecord);
        }
    }

    @Override
    public int importTask(List<TaskRecord> taskList, String env, String createBy) {
        if (CollectionUtils.isEmpty(taskList)) {
            return 0;
        }
        for (TaskRecord task : taskList) {
            if (StringUtils.isEmpty(task.getJiraNo()) || task.getJiraNo().length() > 20) {
                throw new ServiceException("参数错误，JIRA编号为空或超长(限制20字符)！");
            }
            if (StringUtils.isEmpty(task.getDemandName()) || task.getDemandName().length() > 100) {
                throw new ServiceException("参数错误，需求名为空或超长(限制100字符)！");
            }
            if (StringUtils.isEmpty(task.getPrincipal()) || task.getPrincipal().length() > 50) {
                throw new ServiceException("参数错误，责任人为空或超长(限制50字符)！");
            }
            if (StringUtils.isNotEmpty(task.getRemark()) && task.getRemark().length() > 200) {
                throw new ServiceException("参数错误，备注信息超长(限制200字符)！");
            }
            if (StringUtils.isNotEmpty(task.getRelateDemand()) && task.getRelateDemand().length() > 100) {
                throw new ServiceException("参数错误，关联业务需求超长(限制100字符)！");
            }
            task.setIterateWeek(new Date()); // TODO 需要从Excel中获取
            task.setOutDlls(""); // 导入默认将DLL设置为空
            task.setDemandType(1); // 设置默认类型
            task.setEnv(env); // 设置环境
            task.setCreateBy(createBy); // 设置创建人
        }
        return taskRecordMapper.batchInsert(taskList);
    }
}
