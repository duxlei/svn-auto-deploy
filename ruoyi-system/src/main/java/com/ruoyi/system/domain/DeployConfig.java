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

    private String mailConfig;

    private MailConfig mail;

    private String compileCmd;
    private Long compileWait;

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

    public String getMailConfig() {
        return mailConfig;
    }

    public void setMailConfig(String mailConfig) {
        this.mailConfig = mailConfig;
    }

    public MailConfig getMail() {
        return mail;
    }

    public void setMail(MailConfig mail) {
        this.mail = mail;
    }

    public String getCompileCmd() {
        return compileCmd;
    }

    public void setCompileCmd(String compileCmd) {
        this.compileCmd = compileCmd;
    }

    public Long getCompileWait() {
        return compileWait;
    }

    public void setCompileWait(Long compileWait) {
        this.compileWait = compileWait;
    }

    public static class MailConfig {
        private String host;
        private Integer port;
        private String from;
        private String pass;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getPass() {
            return pass;
        }

        public void setPass(String pass) {
            this.pass = pass;
        }
    }
}
