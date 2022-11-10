/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DeployConfig;
import com.ruoyi.system.domain.TaskLog;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.TaskLogMapper;
import com.ruoyi.system.mapper.TaskRecordMapper;
import com.ruoyi.system.service.ITaskRecordService;
import com.ruoyi.system.service.deploy.DefaultDeployProcess;
import com.ruoyi.system.service.deploy.Env;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author duhg
 * @date 2022/10/28 19:42
 */
@Service
public class ITaskRecordServiceImpl implements ITaskRecordService {

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    @Autowired
    private TaskLogMapper taskLogMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private DefaultDeployProcess deployProcess;

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
    @Transactional(rollbackFor = Exception.class)
    public int importTask(List<TaskRecord> taskList, String env, String createBy) {
        taskList = taskList.stream().filter(Objects::nonNull).collect(Collectors.toList());
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
        }
        List<String> jiraNoList = taskList.stream().map(TaskRecord::getJiraNo).collect(Collectors.toList());
        TaskRecordQueryVo query = new TaskRecordQueryVo();
        query.setJiraNoList(jiraNoList);
        query.setEnv(env);
        List<TaskRecord> existTasks = taskRecordMapper.selectList(query);
        List<String> existJiraNoList = existTasks.stream().map(TaskRecord::getJiraNo).collect(Collectors.toList());

        // 需要插入的任务
        List<TaskRecord> insertList = taskList.stream().filter(e -> !existJiraNoList.contains(e.getJiraNo())).collect(Collectors.toList());
        // 需要更新的任务
        List<TaskRecord> updateList = taskList.stream().filter(e -> existJiraNoList.contains(e.getJiraNo())).collect(Collectors.toList());

        insertList.forEach(e -> e.setCreateBy(createBy)); // 为插入的任务设置创建人
        updateList.forEach(e -> e.setUpdateBy(createBy)); // 为更新的任务设置更新人

        if (!CollectionUtils.isEmpty(insertList)) {
            taskRecordMapper.batchInsert(insertList);
        }
        if (!CollectionUtils.isEmpty(updateList)) {
            taskRecordMapper.batchUpdate(updateList, env);
        }

        return taskList.size();
    }

    @Override
    public int deploy(List<Long> taskIds, String envName, String opt) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return 0;
        }
        // 将所有发布任务查询出来
        TaskRecordQueryVo query = new TaskRecordQueryVo();
        query.setIds(taskIds);
        List<TaskRecord> taskRecords = taskRecordMapper.selectList(query);
        if (CollectionUtils.isEmpty(taskRecords)) {
            return 0;
        }
        // TODO 获取当前分支上游的合并分支
        Env env = getEnv(envName);

        // TODO 排队逻辑

        // 执行发布流程
        try {
            deployProcess.deploy(opt, env, taskRecords);
        } catch (Exception e) {
            if (e instanceof SVNException) {
                SVNErrorCode errorCode = ((SVNException) e).getErrorMessage().getErrorCode();
                if (SVNErrorCode.WC_SCHEDULE_CONFLICT.getCode() == errorCode.getCode() ||
                    SVNErrorCode.WC_FOUND_CONFLICT.getCode() == errorCode.getCode()) {
                    throw new RuntimeException("合并过程中出现冲突，请手动处理");
                }
            }
            e.printStackTrace();
            throw new RuntimeException("发布失败");
        }

        return taskIds.size();
    }

    /** 获取发布环境信息 */
    private Env getEnv(String envName) {
        SysMenu query = new SysMenu();
        query.setMenuName(envName);
        List<SysMenu> sysMenus = menuMapper.selectMenuList(query);
        if (CollectionUtils.isEmpty(sysMenus)) {
            throw new RuntimeException("环境菜单未配置");
        }
        if (StringUtils.isEmpty(sysMenus.get(0).getEnvPath())) {
            throw new RuntimeException("分支信息配置错误");
        }
        String envPath = sysMenus.get(0).getEnvPath();
        String srcPath = sysMenus.get(0).getSrcPath();
        DeployConfig config = getConfig();
        String svnUrl = config.getSvnUrl();
        if (StringUtils.isEmpty(svnUrl)) {
            throw new RuntimeException("SVN仓库地址未配置");
        }
        return new Env(envName, envPath, srcPath, svnUrl);
    }

    @Override
    public List<TaskLog> detail(Long taskId) {
        List<TaskLog> taskLogs = taskLogMapper.selectByTaskId(taskId);
        return taskLogs;
    }

    @Override
    public int saveConfig(DeployConfig config) {
        return taskRecordMapper.saveConfig(config);
    }

    @Override
    public DeployConfig getConfig() {
        return taskRecordMapper.selectConfig();
    }
}
