/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.TaskRecord;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.util.*;

/**
 * @author duhg
 * @date 2022/10/30 16:04
 */
@Component
public class DefaultDeployProcess implements DeployProcess {

    public static String SVN_REPO = "svn://192.168.56.100";

    public static String LOCAL_DIR = "svn_wc";
    public static File WORK_DIR = new File(LOCAL_DIR);
    public static String USER_NAME = "duhg";
    public static String PASS_WD = "123456";
    public static List<String> IGNORE_FILE = Arrays.asList(".svn", ".git");

    @Override
    public void deploy(String env, List<TaskRecord> taskRecords) throws SVNException {

        // checkout并更新当前env分支的代码
        checkoutAndUpdateEnv(env);

        // 循环执行每个任务的合并逻辑
        List<DeployTaskItem> deployTaskItems = mergeBranch(taskRecords, env);

        // 执行编译脚本
        compileDlls(taskRecords);

        // 复制DLL和静态资源
        copyDllAndStatic(deployTaskItems);

        // 向远程仓库push代码
        pushRepo(env);

        // 清除wc
        clearWc();

    }

    /** 清除wc */
    private void clearWc() {
        // TODO 清除wc
    }

    /** 向远程仓库push代码 */
    private void pushRepo(String env) {
        // TODO 向远程仓库push代码
    }

    /** 复制DLL和静态资源 */
    private void copyDllAndStatic(List<DeployTaskItem> deployTaskItems) {
        // TODO 复制DLL和静态资源
    }

    /**
     * checkout并更新当前env分支的代码
     */
    private void checkoutAndUpdateEnv(String env) throws SVNException {
        // checkout
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        SVNURL SVN_URL = SVNURL.parseURIEncoded(SVN_REPO);
        ;
        updateClient.doCheckout(SVN_URL, WORK_DIR, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);

        // switch
        SVNURL SW_URL = SVNURL.parseURIEncoded("svn://192.168.56.100/" + env);
        updateClient.doSwitch(WORK_DIR, SW_URL, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
    }

    /**
     * 循环执行每个任务的合并逻辑
     */
    private List<DeployTaskItem> mergeBranch(List<TaskRecord> taskRecords, String env) throws SVNException {
        List<DeployTaskItem> deployTaskItems = new ArrayList<>();
        // 根据任务的JIRA编号，获取所有需要合并的commit
        Map<String, List<SVNLogEntry>> mergeMap = collectCommit(taskRecords, env);
        // 每个发布任务执行合并流程，当出现冲突时停止整个流程
        for (TaskRecord taskRecord : taskRecords) {
            // 合并本任务关联的提交
            List<SVNLogEntry> logEntries = mergeMap.get(taskRecord.getJiraNo());
            String log = mergeTaskCommit(logEntries);
            // 修改任务状态为【已合并】
            DeployTaskItem deployTaskItem = new DeployTaskItem();
            deployTaskItem.setLog(log);
            deployTaskItem.setTaskStatus(1);
            deployTaskItem.setTaskRecord(taskRecord);
            deployTaskItems.add(deployTaskItem);
        }
        return deployTaskItems;
    }

    /** 合并Commit */
    private String mergeTaskCommit(List<SVNLogEntry> logEntries) {
        // TODO 合并Commit
        return "";
    }

    /** 收集需要合并的Commit */
    private Map<String, List<SVNLogEntry>> collectCommit(List<TaskRecord> taskRecords, String env) throws SVNException {
        SVNURL SVN_URL = SVNURL.parseURIEncoded(SVN_REPO); // TODO 要设置成被合并的分支
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        SVNRepository repository = SVNRepositoryFactory.create(SVN_URL);
        // 身份验证
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USER_NAME, PASS_WD.toCharArray());
        repository.setAuthenticationManager(authManager);

        // TODO 收集的时候startRevision可以优化，每次都从开头收集相对耗时
        // 收集需要被合并的提交（按照JIRA号分组，后续每个JIRA号执行合并流程）
        Map<String, List<SVNLogEntry>> mergeMap = new HashMap<>();
        repository.log(new String[]{""}, 0, -1, true, true,
                logEntry -> {
                    // 只收集提交message里包含本次发布所涉及的JIRA编号的提交
                    String message = logEntry.getMessage();
                    if (StringUtils.isEmpty(message)) {
                        return;
                    }
                    TaskRecord matchTask = taskRecords.stream().filter(e -> message.contains(e.getJiraNo())).findFirst().orElse(null);
                    if (matchTask == null) {
                        return;
                    }
                    mergeMap.getOrDefault(matchTask.getJiraNo(), new ArrayList<>()).add(logEntry);
                });

        return mergeMap;
    }

    /** 执行编译脚本 */
    private void compileDlls(List<TaskRecord> taskRecords) {
        // TODO 执行编译脚本
    }

    private void printLog(SVNLogEntry svnlogentry) {
        Map<String, SVNLogEntryPath> changedPaths = svnlogentry.getChangedPaths();
        System.out.println("revision: " + svnlogentry.getRevision());
        System.out.println("author  : " + svnlogentry.getAuthor());
        System.out.println("message : " + svnlogentry.getMessage());
        StringBuffer result = new StringBuffer();
        if (changedPaths != null && !changedPaths.isEmpty()) {
            for (Iterator paths = changedPaths.values().iterator(); paths.hasNext(); ) {
                result.append('\n');
                SVNLogEntryPath path = (SVNLogEntryPath) paths.next();
                result.append(path.toString());
            }
        }
        System.out.println("changedPaths : " + result);
        System.out.println("===================");
    }
}
