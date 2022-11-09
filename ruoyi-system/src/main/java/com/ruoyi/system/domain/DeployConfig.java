/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.domain;

/**
 * @author duhg
 * @date 2022/11/9 22:19
 */
public class DeployConfig {

    private String svnUrl;

    private Integer excelSkipRow;

    private String notifyEmails;

    public String getSvnUrl() {
        return svnUrl;
    }

    public void setSvnUrl(String svnUrl) {
        this.svnUrl = svnUrl;
    }

    public Integer getExcelSkipRow() {
        return excelSkipRow;
    }

    public void setExcelSkipRow(Integer excelSkipRow) {
        this.excelSkipRow = excelSkipRow;
    }

    public String getNotifyEmails() {
        return notifyEmails;
    }

    public void setNotifyEmails(String notifyEmails) {
        this.notifyEmails = notifyEmails;
    }
}
