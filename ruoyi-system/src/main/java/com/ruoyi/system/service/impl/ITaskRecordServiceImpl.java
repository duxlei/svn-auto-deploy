/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.impl;

import com.alibaba.fastjson2.JSON;
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
import com.ruoyi.system.service.deploy.DeployExecutor;
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

    @Autowired
    private DeployExecutor deployExecutor;

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
                throw new ServiceException("???????????????JIRA?????????????????????(??????20??????)???");
            }
            if (StringUtils.isEmpty(task.getDemandName()) || task.getDemandName().length() > 100) {
                throw new ServiceException("???????????????????????????????????????(??????100??????)???");
            }
            if (StringUtils.isEmpty(task.getPrincipal()) || task.getPrincipal().length() > 50) {
                throw new ServiceException("???????????????????????????????????????(??????50??????)???");
            }
            if (StringUtils.isNotEmpty(task.getRemark()) && task.getRemark().length() > 200) {
                throw new ServiceException("?????????????????????????????????(??????200??????)???");
            }
            if (StringUtils.isNotEmpty(task.getRelateDemand()) && task.getRelateDemand().length() > 100) {
                throw new ServiceException("???????????????????????????????????????(??????100??????)???");
            }
            task.setIterateWeek(new Date()); // TODO ?????????Excel?????????
            task.setOutDlls(""); // ???????????????DLL????????????
            task.setDemandType(1); // ??????????????????
            task.setEnv(env); // ????????????
        }
        List<String> jiraNoList = taskList.stream().map(TaskRecord::getJiraNo).collect(Collectors.toList());
        TaskRecordQueryVo query = new TaskRecordQueryVo();
        query.setJiraNoList(jiraNoList);
        query.setEnv(env);
        List<TaskRecord> existTasks = taskRecordMapper.selectList(query);
        List<String> existJiraNoList = existTasks.stream().map(TaskRecord::getJiraNo).collect(Collectors.toList());

        // ?????????????????????
        List<TaskRecord> insertList = taskList.stream().filter(e -> !existJiraNoList.contains(e.getJiraNo())).collect(Collectors.toList());
        // ?????????????????????
        List<TaskRecord> updateList = taskList.stream().filter(e -> existJiraNoList.contains(e.getJiraNo())).collect(Collectors.toList());

        insertList.forEach(e -> e.setCreateBy(createBy)); // ?????????????????????????????????
        updateList.forEach(e -> e.setUpdateBy(createBy)); // ?????????????????????????????????

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
        // ?????????????????????????????????
        TaskRecordQueryVo query = new TaskRecordQueryVo();
        query.setIds(taskIds);
        List<TaskRecord> taskRecords = taskRecordMapper.selectList(query);
        if (CollectionUtils.isEmpty(taskRecords)) {
            return 0;
        }
        // ???????????????????????????????????????
        Env env = getEnv(envName);

        // ???????????????????????????????????????
        // ??????????????????
        deployExecutor.runJob(() -> {
            try {
                deployProcess.deploy(opt, env, taskRecords);
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SVNException) {
                    SVNErrorCode errorCode = ((SVNException) e).getErrorMessage().getErrorCode();
                    if (SVNErrorCode.WC_SCHEDULE_CONFLICT.getCode() == errorCode.getCode() ||
                        SVNErrorCode.WC_FOUND_CONFLICT.getCode() == errorCode.getCode()) {
                        throw new RuntimeException("?????????????????????????????????????????????");
                    }
                }
                throw new RuntimeException("????????????");
            }
        });

        return taskIds.size();
    }

    /** ???????????????????????? */
    private Env getEnv(String envName) {
        SysMenu query = new SysMenu();
        query.setMenuName(envName);
        List<SysMenu> sysMenus = menuMapper.selectMenuList(query);
        if (CollectionUtils.isEmpty(sysMenus)) {
            throw new RuntimeException("?????????????????????");
        }
        if (StringUtils.isEmpty(sysMenus.get(0).getEnvPath())) {
            throw new RuntimeException("????????????????????????");
        }
        String envPath = sysMenus.get(0).getEnvPath();
        String srcPath = sysMenus.get(0).getSrcPath();
        DeployConfig config = getConfig();
        String svnUrl = config.getSvnUrl();
        if (StringUtils.isEmpty(svnUrl)) {
            throw new RuntimeException("SVN?????????????????????");
        }
        return new Env(envName, envPath, srcPath, svnUrl, config.getCompileCmd(), config.getCompileWait());
    }

    @Override
    public List<TaskLog> detail(Long taskId) {
        List<TaskLog> taskLogs = taskLogMapper.selectByTaskId(taskId);
        return taskLogs;
    }

    @Override
    public int saveConfig(DeployConfig config) {
        config.setMailConfig(JSON.toJSONString(config.getMail()));
        return taskRecordMapper.saveConfig(config);
    }

    @Override
    public DeployConfig getConfig() {
        DeployConfig deployConfig = taskRecordMapper.selectConfig();
        try {
            DeployConfig.MailConfig mailConfig = JSON.parseObject(deployConfig.getMailConfig(), DeployConfig.MailConfig.class);
            deployConfig.setMail(mailConfig == null ? new DeployConfig.MailConfig() : mailConfig);
        } catch (Exception e) {
            deployConfig.setMail(new DeployConfig.MailConfig());
        }
        return deployConfig;
    }
}
