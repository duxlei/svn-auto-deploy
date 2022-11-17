/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PathUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DeployConfig;
import com.ruoyi.system.domain.TaskLog;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.mapper.TaskLogMapper;
import com.ruoyi.system.mapper.TaskRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author duhg
 * @date 2022/10/30 16:04
 */
@Component
public class DefaultDeployProcess implements DeployProcess {

    private static final Logger log = LoggerFactory.getLogger(DefaultDeployProcess.class);

//    @Value("${deploy.svnRepoUrl}")
//    public String SVN_REPO = "svn://192.168.56.100";

    public static File WORK_DIR = new File(SvnConstant.LOCAL_DIR);
    public static String USER_NAME = "duhg";
    public static String PASS_WD = "123456";
    public static String STATIC_PATH = "update";
    public static List<String> IGNORE_FILE = Arrays.asList(".svn", ".git");

    // 静态资源类型（可以配置化）
    public static List<String> STATIC_FILE_EXT = Arrays.asList(".xml", ".js", ".jpeg", ".jpg", ".png", ".gif", ".html", ".htm", ".css", ".css", ".json", ".svg");

    @Autowired
    private SvnRepoHolder svnRepoHolder;

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    @Autowired
    private TaskLogMapper taskLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deploy(String deployer, Env env, List<TaskRecord> taskRecords) throws Exception {
        // TODO 需要有个全局的日志记录，全流程记录，组后落库

        // 1.checkout并更新当前env分支的代码
        checkoutAndUpdateEnv(env);

        // 2.循环执行每个任务的合并逻辑
        List<DeployTaskItem> deployTaskItems = mergeBranch(env, taskRecords);
        if (deployTaskItems.stream().anyMatch(e -> Objects.equals(e.getTaskStatus(), TaskStatusEnum.MERGE_ERR.val()))) {
            // 如果有合并失败需要发送邮件
            sendNotifyEmail(deployTaskItems, env, "合并失败");
            // 保存发布信息
            saveDeployInfo(deployTaskItems, env, deployer);
            return;
        }

        // 3.执行编译脚本
        boolean compileResult = compileDlls(taskRecords, env, STATIC_PATH);
        if (compileResult) {
            // 编译成功，更新发布任务状态已编译
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.COMPILED.val()));
        } else {
            // 编译失败，所有任务状态都置为编译失败状态
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.COMPILE_ERR.val()));
            // 如果有合并失败需要发送邮件
            sendNotifyEmail(deployTaskItems, env, "编译失败");
            // 保存发布信息
            saveDeployInfo(deployTaskItems, env, deployer);
            return;
        }

        // 4.复制DLL和静态资源
        copyDllAndStatic(deployTaskItems, env, STATIC_PATH);

        // 5.向远程仓库commit代码
        boolean pushResult = pushRepo(deployer);
        if (pushResult) {
            // commit成功，更新发布任务状态发布成功
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.SUCCESS.val()));
        } else {
            // 推送失败，所有任务状态都置为推送失败状态
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.PUSH_ERR.val()));
            // 如果有合并失败需要发送邮件
            sendNotifyEmail(deployTaskItems, env, "Commit失败");
        }

        // 6.清除wc
        String clearMsg = clearWc();
        if (clearMsg != null) {
            deployTaskItems.forEach(e -> e.setLog(e.getLog() + "\n\n清除wc失败：\n" + clearMsg));
        }

        // 更新发布任务状态、插入发布日志
        saveDeployInfo(deployTaskItems, env, deployer);
    }

    /** 清除wc */
    private String clearWc() {
        // 清除wc
        try {
            FileUtil.del(WORK_DIR);
        } catch (Exception e) {
            return getExceptStack(e);
        }
        return null;
    }

    /** 向远程仓库commit代码 */
    private boolean pushRepo(String deployer) throws SVNException {
        // 添加所有无版本控制的文件（新文件）
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        clientManager.getStatusClient().doStatus(WORK_DIR, SVNRevision.HEAD, SVNDepth.INFINITY, false, true, false, false, status -> {
            if (status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                log.info("add file {}", status.getFile().getName());
                clientManager.getWCClient().doAdd(status.getFile(), false, false, false, SVNDepth.INFINITY, false, true);
            }
        }, null);

        // 向远程仓库commit代码
        SVNCommitClient commitClient = clientManager.getCommitClient();
        SVNCommitPacket svnCommitPacket = commitClient.doCollectCommitItems(new File[]{WORK_DIR}, false, true, SVNDepth.INFINITY, null);
        commitClient.doCommit(svnCommitPacket, false, deployer + ": 使用SVN发版系统提交");
        return true;
    }

    /** 复制DLL和静态资源 */
    private void copyDllAndStatic(List<DeployTaskItem> deployTaskItems, Env env, String staticPath) throws SVNException {
//        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        // 复制DLL和静态资源
        String dateName = DateUtils.dateTime() + "-" + env.getName();
        List<String> staticFileList = new ArrayList<>();
        for (DeployTaskItem deployTaskItem : deployTaskItems) {
            TaskRecord taskRecord = deployTaskItem.getTaskRecord();
            // 复制SQL（只复制SQL文件名称和JIRA编号关联的SQL文件）
            List<String> sqlFileList = collectSql(taskRecord.getJiraNo(), env.getSrcPath());
            // 复制静态资源（只复制本次发布变化的文件）
            List<String> statics = collectStatic(deployTaskItem, env.getSrcPath());
            staticFileList.addAll(statics);

//            OptionalLong opt = deployTaskItem.getLogEntries().stream().mapToLong(SVNLogEntry::getRevision).max();
//            SVNRevision copyRevision = opt.isPresent() ? SVNRevision.create(opt.getAsLong()) : SVNRevision.HEAD;
//            List<SVNCopySource> scs = new ArrayList<>();

            String dirName = taskRecord.getPrincipal() + "-" + taskRecord.getJiraNo() + "-" + taskRecord.getDemandName();
            // 执行文件复制逻辑（需要保持目录结构）
            for (String file : sqlFileList) {
                File copyFile = new File(file);
                if (!copyFile.exists()) {
                    throw new RuntimeException("发布失败，复制文件时部分文件不存在\n分支：" + env.getSrcPath() + "\n文件：" + copyFile.getAbsolutePath());
                }
                File distFile = new File(WORK_DIR, PathUtils.peekPath(staticPath, dateName, dirName, "sql", copyFile.getName()));
                FileUtil.copyFile(copyFile, distFile, StandardCopyOption.REPLACE_EXISTING);
//                SVNCopySource svnCopySource = new SVNCopySource(copyRevision, copyRevision, copyFile);
//                scs.add(svnCopySource);
//                clientManager.getWCClient().doDelete(distFile, true, false);
            }
//            File distFile = new File(WORK_DIR, PathUtils.peekPath(staticPath, dateName, dirName, "sql"));
//            SVNCopySource[] sources = scs.toArray(new SVNCopySource[0]);
//            clientManager.getCopyClient().doCopy(sources, distFile, false, true, false);
        }

        for (String file : staticFileList) {
            File copyFile = new File(WORK_DIR, PathUtils.peekPath(file));
            if (!copyFile.exists()) {
                throw new RuntimeException("发布失败，复制文件时部分文件不存在\n分支：" + env.getSrcPath() + "\n文件：" + copyFile.getAbsolutePath());
            }
            File distFile = new File(WORK_DIR, PathUtils.peekPath(staticPath, dateName, "other", copyFile.getName()));
            FileUtil.copyFile(copyFile, distFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * checkout并更新当前env分支的代码
     */
    private void checkoutAndUpdateEnv(Env env) throws SVNException {
        if (WORK_DIR.exists()) {
            // 清理已存在的工作区
            clearWc();
        }

        // checkout
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        SVNURL SVN_URL = SVNURL.parseURIEncoded(env.getRepoUrl());

        updateClient.doCheckout(SVN_URL, WORK_DIR, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
    }

    /**
     * 循环执行每个任务的合并逻辑
     */
    private List<DeployTaskItem> mergeBranch(Env env, List<TaskRecord> taskRecords) throws SVNException {
        List<DeployTaskItem> deployTaskItems = new ArrayList<>();
        // 根据任务的JIRA编号，获取所有需要合并的commit
        Map<String, List<SVNLogEntry>> mergeMap = collectCommit(taskRecords, env);
        // 每个发布任务执行合并流程，当出现冲突时停止整个流程
        for (TaskRecord taskRecord : taskRecords) {
            DeployTaskItem deployTaskItem = new DeployTaskItem();
            String log;
            int status = TaskStatusEnum.MERGED.val(); //默认状态为合并成功
            // 上游合并分支为空就不合并（适用于sit分支）
            if (StringUtils.isEmpty(env.getSrcPath())) {
                log = "无上游分支不合并";
            } else {
                try {
                    // 合并本任务关联的提交
                    List<SVNLogEntry> logEntries = mergeMap.get(taskRecord.getJiraNo());
                    deployTaskItem.setLogEntries(logEntries);
                    if (CollectionUtils.isEmpty(logEntries)) {
                        log = "没有匹配的提交记录";
                    } else {
                        log = mergeTaskCommit(env, logEntries);
                    }
                } catch (SVNException e) {
                    SVNErrorCode errorCode = e.getErrorMessage().getErrorCode();
                    if (SVNErrorCode.WC_SCHEDULE_CONFLICT.getCode() == errorCode.getCode() ||
                        SVNErrorCode.WC_FOUND_CONFLICT.getCode() == errorCode.getCode()) {
                        log = "合并过程中出现冲突，请处理\n" + e.getErrorMessage().getFullMessage();
                    } else {
                        log = "发布失败\n" + getExceptStack(e);
                    }
                    status = TaskStatusEnum.MERGE_ERR.val(); //合并失败
                }
            }
            deployTaskItem.setLog(log);
            deployTaskItem.setTaskStatus(status);
            deployTaskItem.setTaskRecord(taskRecord);
            deployTaskItems.add(deployTaskItem);
        }
        return deployTaskItems;
    }

    /** 获取异常栈信息 */
    private String getExceptStack(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            t.printStackTrace(pw);
            return sw.toString();
        } finally {
            pw.close();
        }
    }

    /** 合并Commit */
    private String mergeTaskCommit(Env env, List<SVNLogEntry> logEntries) throws SVNException {
        // 合并Commit
        StringBuffer sb = new StringBuffer();
        for (SVNLogEntry logEntry : logEntries) {
            // 解析提交日志
            sb.append(parseLog(logEntry));
            // 合并
            svnRepoHolder.merge(env.getRepoUrl(), env.getSrcPath(), env.getPath(), logEntry.getRevision());
        }
        // 检查是否有冲突文件
        List<String> conflictList = new ArrayList<>();
        File distFile = new File(WORK_DIR, PathUtils.peekPath(env.getPath()));
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        clientManager.getStatusClient().doStatus(distFile, SVNRevision.HEAD, SVNDepth.INFINITY, false, true, false, false, status -> {
            if (status == null) {
                return;
            }
            SVNTreeConflictDescription treeConflict = status.getTreeConflict();
            if (treeConflict !=  null && treeConflict.isTreeConflict()) {
                String fn = status.getFile().getAbsolutePath().substring(WORK_DIR.getAbsolutePath().length() + 1).replace(File.separator, "/");
                conflictList.add(fn);
            }
        }, null);
        if (!CollectionUtils.isEmpty(conflictList)) {
            String errorMsg = "合并冲突\n" + String.join("\n", conflictList);
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT, errorMsg));
        }

        return sb.toString();
    }

    /** 收集需要合并的Commit */
    private Map<String, List<SVNLogEntry>> collectCommit(List<TaskRecord> taskRecords, Env env) throws SVNException {
        if (StringUtils.isEmpty(env.getSrcPath())) {
            return new HashMap<>();
        }
        // 收集需要被合并的提交（按照JIRA号分组，后续每个JIRA号执行合并流程）
        Map<String, List<SVNLogEntry>> mergeMap = new HashMap<>();
        // TODO 收集的时候startRevision可以优化，每次都从开头收集相对耗时
        svnRepoHolder.log(env.getRepoUrl(), env.getSrcPath(), 0, -1, logEntry -> {
            // 只收集提交message里包含本次发布所涉及的JIRA编号的提交
            String message = logEntry.getMessage();
            if (StringUtils.isEmpty(message)) {
                return;
            }
            TaskRecord matchTask = taskRecords.stream().filter(e -> message.contains(e.getJiraNo())).findFirst().orElse(null);
            if (matchTask == null) {
                return;
            }
            List<SVNLogEntry> entries = mergeMap.getOrDefault(matchTask.getJiraNo(), new ArrayList<>());
            entries.add(logEntry);
            mergeMap.put(matchTask.getJiraNo(), entries);
        });

        return mergeMap;
    }

    /** 收集需要复制sql文件 */
    private List<String> collectSql(List<DeployTaskItem> deployTaskItems, String distPath) {
        List<String> jiraNos = deployTaskItems.stream().map(e -> e.getTaskRecord().getJiraNo()).collect(Collectors.toList());
        File distDir = new File(WORK_DIR, distPath);
        List<String> result = new ArrayList<>();
        FileUtil.walkFiles(distDir, file -> {
            if (jiraNos.stream().anyMatch(e -> file.getName().contains(e)) && file.getName().endsWith(".sql")) {
                result.add(file.getAbsolutePath().substring(WORK_DIR.getAbsolutePath().length()));
            }
        });
        return result;
    }
    private List<String> collectSql(String jiraNo, String path) {
        File distDir = new File(WORK_DIR, path);
        List<String> result = new ArrayList<>();
        FileUtil.walkFiles(distDir, file -> {
            if (file.getName().contains(jiraNo) && file.getName().endsWith(".sql")) {
                result.add(file.getAbsolutePath());
            }
        });
        return result;
    }
    /** 收集需要复制静态资源文件 */
    private List<String> collectStatic(List<DeployTaskItem> deployTaskItems, String srcPath) {
        List<String> result = new ArrayList<>();
        for (DeployTaskItem deployTaskItem : deployTaskItems) {
            result.addAll(collectStatic(deployTaskItem, srcPath));
        }
        return result;
    }
    private List<String> collectStatic(DeployTaskItem deployTaskItem, String srcPath) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(deployTaskItem.getLogEntries())) {
            return result;
        }
        for (SVNLogEntry logEntry : deployTaskItem.getLogEntries()) {
            Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
            if (changedPaths == null || changedPaths.isEmpty()) {
                continue;
            }
            for (SVNLogEntryPath entryPath : changedPaths.values()) {
                SVNNodeKind nodeKind = entryPath.getKind();
                String filePath = entryPath.getPath();
                if (nodeKind != null &&
                    nodeKind.getID() == SVNNodeKind.FILE.getID() &&
                    STATIC_FILE_EXT.stream().anyMatch(filePath::endsWith)) {
                    result.add(filePath);
                }
            }
        }
        return result;
    }

    /** 执行编译脚本 */
    private boolean compileDlls(List<TaskRecord> taskRecords, Env env, String staticPath) {
        // TODO 执行编译脚本
//        List<String> allDlls = taskRecords.stream()
//                .map(TaskRecord::getOutDlls)
//                .filter(StringUtils::isNotEmpty)
//                .map(dlls -> Arrays.stream(dlls.split(",")).collect(Collectors.toList()))
//                .flatMap(Collection::stream)
//                .distinct()
//                .collect(Collectors.toList());

        String dateName = DateUtils.dateTime() + "-" + env.getName();
        for (TaskRecord taskRecord : taskRecords) {
            if (StringUtils.isEmpty(taskRecord.getOutDlls())) {
                continue;
            }
            String dirName = taskRecord.getPrincipal() + "-" + taskRecord.getJiraNo() + "-" + taskRecord.getDemandName();
            for (String dll : taskRecord.getOutDlls().split(",")) {
                String fp = PathUtils.peekPath(staticPath, dateName, "dll", dll);
                FileUtil.del(new File(WORK_DIR, fp));
                File touch = FileUtil.touch(WORK_DIR, fp);
                FileUtil.appendString(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date()), touch, CharsetUtil.UTF_8);
            }
        }

        return true;
    }

    /** TODO 发布失败发送邮件 */
    private void sendNotifyEmail(List<DeployTaskItem> deployTaskItems, Env env, String msg) {
        // TODO 发送通知邮件
        DeployConfig deployConfig = taskRecordMapper.selectConfig();
        if (deployConfig == null) {
            log.error("未配置发布信息");
            return;
        }
        if (StringUtils.isEmpty(deployConfig.getNotifyEmails())) {
            log.info("未配置通知邮箱");
            return;
        }
        List<String> emails = Arrays.stream(deployConfig.getNotifyEmails().split(",")).collect(Collectors.toList());
        try {
            DeployConfig.MailConfig mailConfig = JSON.parseObject(deployConfig.getMailConfig(), DeployConfig.MailConfig.class);
            String title = String.format("SVN自动发版通知(分支%s) - %s", env.getName(), msg);
            StringBuilder content = new StringBuilder("<pre>");
            deployTaskItems.forEach(e -> {
                // 合并Commit
//                StringBuffer sb = new StringBuffer();
//                for (SVNLogEntry logEntry : e.getLogEntries()) {
//                    // 解析提交日志
//                    sb.append(parseLog(logEntry));
//                }
                content.append(String.format("JIRA：%s\n需求名：%s\n", e.getTaskRecord().getJiraNo(), e.getTaskRecord().getDemandName()));
                content.append("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\n");
                content.append(e.getLog());
                content.append("\n");
            });
            content.append("</pre>");
            sendMail(mailConfig, emails, title, content.toString());
        } catch (Exception e) {
            log.warn("发送通知邮件失败", e);
        }
    }

    private void sendMail(DeployConfig.MailConfig mailConfig, List<String> emails, String title, String content) {
        MailAccount account = new MailAccount();
        account.setHost(mailConfig.getHost());
        account.setPort(mailConfig.getPort());
        account.setAuth(true);
        account.setFrom(mailConfig.getFrom());
        account.setPass(mailConfig.getPass());
        MailUtil.send(account, emails, title, content, true);
    }

    /** 保存发布信息 */
    private void saveDeployInfo(List<DeployTaskItem> deployTaskItems, Env env, String deployer) {
        // 插入发布日志
        List<TaskLog> taskLogs = deployTaskItems.stream().map(e -> new TaskLog(e.getTaskRecord().getId(), e.getLog(), e.getTaskStatus(), deployer)).collect(Collectors.toList());
        taskLogMapper.batchInsert(taskLogs);
        // 更新发布任务状态
        List<TaskRecord> records = deployTaskItems.stream().map(e -> {
            e.getTaskRecord().setStatus(e.getTaskStatus());
            e.getTaskRecord().setUpdateBy(deployer);
            return e.getTaskRecord();
        }).collect(Collectors.toList());
        taskRecordMapper.batchUpdate(records, env.getPath());
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

    private String parseLog(SVNLogEntry svnlogentry) {
        StringBuffer result = new StringBuffer("================");
        result.append("\nrevision: " + svnlogentry.getRevision());
        result.append("\nauthor  : " + svnlogentry.getAuthor());
        result.append("\nmessage : " + svnlogentry.getMessage());
        result.append("\n变更文件列表 : ");
        Map<String, SVNLogEntryPath> changedPaths = svnlogentry.getChangedPaths();
        if (changedPaths != null && !changedPaths.isEmpty()) {
            for (Iterator paths = changedPaths.values().iterator(); paths.hasNext(); ) {
                result.append('\n');
                SVNLogEntryPath path = (SVNLogEntryPath) paths.next();
                result.append(path.toString());
            }
        }
        result.append("\n");
        return result.toString();
    }
}
