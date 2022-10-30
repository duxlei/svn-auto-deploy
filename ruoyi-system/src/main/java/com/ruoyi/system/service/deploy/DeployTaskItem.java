/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import com.ruoyi.system.domain.TaskRecord;
import org.tmatesoft.svn.core.SVNLogEntry;

import java.util.List;

/**
 * @author duhg
 * @date 2022/10/30 16:02
 */
public class DeployTaskItem {
    private Integer taskStatus;
    private String log;
    private TaskRecord taskRecord;
    private List<SVNLogEntry> logEntries;

    public Integer getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(Integer taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public TaskRecord getTaskRecord() {
        return taskRecord;
    }

    public void setTaskRecord(TaskRecord taskRecord) {
        this.taskRecord = taskRecord;
    }

    public List<SVNLogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<SVNLogEntry> logEntries) {
        this.logEntries = logEntries;
    }
}
