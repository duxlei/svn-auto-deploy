/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * SVN仓库
 * @author duhg
 * @date 2022/10/31 21:56
 */
@Component
public class SvnRepoHolder {

    @Value("${deploy.repo.username:duhg}")
    public String USER_NAME;
    @Value("${deploy.repo.password:123456}")
    public String PASS_WD;

    private SVNClientManager clientManager;

    @PostConstruct
    public void init() throws SVNException {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
//        SVNRepositoryFactoryImpl.setup();
//        FSRepositoryFactory.setup();
//        this.rootSvnUrl = SVNURL.parseURIEncoded(SVN_REPO);
//        SVNRepository repository = SVNRepositoryFactory.create(this.rootSvnUrl);
//        // 身份验证
//        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER_NAME, PASS_WD.toCharArray());
//        repository.setAuthenticationManager(authManager);
//        this.repository = repository;
    }

    private SVNRepository getRepo(String repoUrl) throws SVNException {
        SVNURL rootSvnUrl = SVNURL.parseURIEncoded(repoUrl);
        SVNRepository repository = SVNRepositoryFactory.create(rootSvnUrl);
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER_NAME, PASS_WD.toCharArray());
        repository.setAuthenticationManager(authManager);
        return repository;
    }

    /** 获取目录最新的Revision版本号 */
    public Long getLastRevision(String repoUrl, String path) throws SVNException {
        SVNRepository repository = getRepo(repoUrl);
        SVNDirEntry dirEntry = repository.info(path, repository.getLatestRevision());
        return dirEntry == null ? null : dirEntry.getRevision();
    }

    /** 切换到仓库根目录 */
    public void sw2Root(String repoUrl, File wcDir) throws SVNException {
        sw(repoUrl, wcDir, "/");
    }

    /** 切换到仓库指定目录 */
    public void sw(String repoUrl, File wcDir, String path) throws SVNException {
        SVNURL swUrl = getRemoteUrl(repoUrl, path);

        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        updateClient.doSwitch(wcDir, swUrl, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
    }

    /** 合并 */
    public void merge(String repoUrl, String srcPath, String distPath, long revision) throws SVNException {
        // 合并分支
        SVNDiffClient diffClient = clientManager.getDiffClient();
        File distDir = new File(SvnConstant.LOCAL_DIR, distPath);

        Long distLastRevision = getLastRevision(repoUrl, distPath);
        if (distLastRevision == null) {
            throw new RuntimeException("合并出错, 源目录未找到最新版本");
        }
        if (distLastRevision >= revision) {
            distLastRevision = revision - 1;
        }
        if (distLastRevision < 0) {
            distLastRevision = 0L;
        }

        SVNURL srcUrl = getRemoteUrl(repoUrl, srcPath);
        List<SVNRevisionRange> svnRevisionRanges = Collections.singletonList(new SVNRevisionRange(SVNRevision.create(distLastRevision), SVNRevision.create(revision)));
        diffClient.doMerge(srcUrl, SVNRevision.create(revision), svnRevisionRanges, distDir, SVNDepth.INFINITY, false, false, false, false);
    }

    /** 根据相对目录获取远程仓库地址 */
    private SVNURL getRemoteUrl(String repoUrl, String path) throws SVNException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return SVNURL.parseURIEncoded(repoUrl + path);
    }

    /** 遍历日志 */
    public void log(String repoUrl, String path, long startRevision, long endRevision, Consumer<SVNLogEntry> consumer) throws SVNException {
        SVNRepository repository = getRepo(repoUrl);
        repository.log(new String[]{path}, startRevision, endRevision, true, true, consumer::accept);
    }

}
