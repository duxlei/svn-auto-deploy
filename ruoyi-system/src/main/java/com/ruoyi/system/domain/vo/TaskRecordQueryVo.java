/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.domain.vo;

import java.util.Date;

/**
 * 任务列表查询对象
 * @author duhg
 * @date 2022/10/28 19:23
 */
public class TaskRecordQueryVo {
    private String jiraNo;
    private String demandName;
    private Integer status;
    private Date iterateWeek;
    private Date startTime;
    private Date endTime;
    private String env;

    public String getJiraNo() {
        return jiraNo;
    }

    public void setJiraNo(String jiraNo) {
        this.jiraNo = jiraNo;
    }

    public String getDemandName() {
        return demandName;
    }

    public void setDemandName(String demandName) {
        this.demandName = demandName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getIterateWeek() {
        return iterateWeek;
    }

    public void setIterateWeek(Date iterateWeek) {
        this.iterateWeek = iterateWeek;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
