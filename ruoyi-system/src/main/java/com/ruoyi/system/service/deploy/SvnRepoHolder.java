/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SVN仓库
 * @author duhg
 * @date 2022/10/31 21:56
 */
@Component
public class SvnRepoHolder {

    public static String SVN_REPO = "svn://192.168.56.100";
    public static String USER_NAME = "duhg";
    public static String PASS_WD = "123456";

    private SVNURL rootSvnUrl;
    private SVNRepository repository;
    private final SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);

    @PostConstruct
    public void init() throws SVNException {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        this.rootSvnUrl = SVNURL.parseURIEncoded(SVN_REPO);
        SVNRepository repository = SVNRepositoryFactory.create(this.rootSvnUrl);
        // 身份验证
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER_NAME, PASS_WD.toCharArray());
        repository.setAuthenticationManager(authManager);
        this.repository = repository;
    }

    /** 获取目录最新的Revision版本号 */
    public Long getLastRevision(String path) throws SVNException {
        SVNDirEntry dirEntry = repository.info(path, repository.getLatestRevision());
        return dirEntry == null ? null : dirEntry.getRevision();
    }

    /** 切换到仓库根目录 */
    public void sw2Root(File wcDir) throws SVNException {
        sw(wcDir, "/");
    }

    /** 切换到仓库指定目录 */
    public void sw(File wcDir, String path) throws SVNException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        SVNURL swUrl = SVNURL.parseURIEncoded(SVN_REPO + path);

        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        updateClient.doSwitch(wcDir, swUrl, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
    }

    /** 合并 */
    public void merge(String src, String dist, long revision) throws SVNException {
        // TODO 合并分支
        SVNDiffClient diffClient = clientManager.getDiffClient();
        File distDir = new File(dist);

        Long srcLastRevision = getLastRevision(src);
        if (srcLastRevision == null) {
            throw new RuntimeException("合并出错, 源目录未找到最新版本");
        }

        SVNURL srcUrl = SVNURL.parseURIEncoded(SVN_REPO + src);
        List<SVNRevisionRange> svnRevisionRanges = Collections.singletonList(new SVNRevisionRange(SVNRevision.create(revision), SVNRevision.create(revision)));
        diffClient.doMerge(srcUrl, SVNRevision.create(srcLastRevision), svnRevisionRanges, distDir, SVNDepth.INFINITY, false, false, false, false);
    }

}
