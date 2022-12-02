/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

/**
 * @author duhg
 * @date 2022/11/10 19:46
 */
public class Env {
    /** 发布的分支名 */
    private String name;
    /** 发布分支的相对路径 */
    private String path;
    /** 源分支相对地址（被合并的分支地址） */
    private String srcPath;
    /** 远程仓库地址 */
    private String repoUrl;

    /** 编译命令 */
    private String compileCmd;

    /** 编译等待时间(单位秒) */
    private Long compileWait;

    public Env(String name, String path, String srcPath, String repoUrl, String compileCmd, Long compileWait) {
        this.name = name;
        this.path = path;
        this.srcPath = srcPath;
        this.repoUrl = repoUrl;
        this.compileCmd = compileCmd;
        this.compileWait = compileWait;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
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
}
