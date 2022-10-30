/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;

import java.util.Date;

/**
 * 发布任务主表
 * @author duhg
 * @date 2022/10/28 19:04
 */
public class TaskRecord {
    private Long id;
    @Excel(name = "JIRAKEY")
    private String jiraNo;
    @Excel(name = "名称")
    private String demandName;
    private Integer demandType;
    @Excel(name = "关联业务需求")
    private String relateDemand;
    @Excel(name = "责任人")
    private String principal;
    @Excel(name = "备注")
    private String remark;
    private Date iterateWeek;
    private String outDlls;
    private Integer status;
    private String env;
    private Date createTime;
    private String createBy;
    private Date updateTime;
    private String updateBy;
    private Boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getDemandType() {
        return demandType;
    }

    public void setDemandType(Integer demandType) {
        this.demandType = demandType;
    }

    public String getRelateDemand() {
        return relateDemand;
    }

    public void setRelateDemand(String relateDemand) {
        this.relateDemand = relateDemand;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getIterateWeek() {
        return iterateWeek;
    }

    public void setIterateWeek(Date iterateWeek) {
        this.iterateWeek = iterateWeek;
    }

    public String getOutDlls() {
        return outDlls;
    }

    public void setOutDlls(String outDlls) {
        this.outDlls = outDlls;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
