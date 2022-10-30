/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.domain.vo;

import java.util.Date;
import java.util.List;

/**
 * 任务列表查询对象
 * @author duhg
 * @date 2022/10/28 19:23
 */
public class TaskRecordQueryVo {
    private Long id;
    private List<Long> ids;
    private String jiraNo;
    private List<String> jiraNoList;
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

    public List<String> getJiraNoList() {
        return jiraNoList;
    }

    public void setJiraNoList(List<String> jiraNoList) {
        this.jiraNoList = jiraNoList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
