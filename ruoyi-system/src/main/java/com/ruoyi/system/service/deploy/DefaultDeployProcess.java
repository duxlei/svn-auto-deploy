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
import org.springframework.security.core.parameters.P;
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
    public static File WORK_COMPILE = new File(SvnConstant.COMPILE_DIR);
    public static String USER_NAME = "duhg";
    public static String PASS_WD = "123456";
    public static String STATIC_PATH = "update";
    public static List<String> IGNORE_FILE = Arrays.asList(".svn", ".git");

    // ???????????????????????????????????????
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
        // TODO ??????????????????????????????????????????????????????????????????

        // 1.checkout???????????????env???????????????
        checkoutAndUpdateEnv(env);

        // 2.???????????????????????????????????????
        List<DeployTaskItem> deployTaskItems = mergeBranch(env, taskRecords);
        if (deployTaskItems.stream().anyMatch(e -> Objects.equals(e.getTaskStatus(), TaskStatusEnum.MERGE_ERR.val()))) {
            // ???????????????????????????????????????
            sendNotifyEmail(deployTaskItems, env, "????????????");
            // ??????????????????
            saveDeployInfo(deployTaskItems, env, deployer);
            return;
        }

        // 3.??????????????????
        boolean compileResult = compileDlls(deployTaskItems, env, STATIC_PATH);
        if (compileResult) {
            // ????????????????????????????????????????????????
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.COMPILED.val()));
        } else {
            // ????????????????????????????????????????????????????????????
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.COMPILE_ERR.val()));
            // ???????????????????????????????????????
            sendNotifyEmail(deployTaskItems, env, "????????????");
            // ??????????????????
            saveDeployInfo(deployTaskItems, env, deployer);
            return;
        }

        // 4.??????DLL???????????????
        copyDllAndStatic(deployTaskItems, env, STATIC_PATH);

        // 5.???????????????commit??????
        boolean pushResult = pushRepo(deployer);
        if (pushResult) {
            // commit?????????????????????????????????????????????
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.SUCCESS.val()));
        } else {
            // ????????????????????????????????????????????????????????????
            deployTaskItems.forEach(e -> e.setTaskStatus(TaskStatusEnum.PUSH_ERR.val()));
            // ???????????????????????????????????????
            sendNotifyEmail(deployTaskItems, env, "Commit??????");
        }

        // 6.??????wc
        String clearMsg = clearWc();
        if (clearMsg != null) {
            deployTaskItems.forEach(e -> e.setLog(e.getLog() + "\n\n??????wc?????????\n" + clearMsg));
        }

        // ?????????????????????????????????????????????
        saveDeployInfo(deployTaskItems, env, deployer);
    }

    /** ??????wc */
    private String clearWc() {
        // ??????wc
        try {
            FileUtil.del(WORK_DIR);
            FileUtil.del(WORK_COMPILE);
        } catch (Exception e) {
            return getExceptStack(e);
        }
        return null;
    }

    /** ???????????????commit?????? */
    private boolean pushRepo(String deployer) throws SVNException {
        // ???????????????????????????????????????????????????
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        clientManager.getStatusClient().doStatus(WORK_DIR, SVNRevision.HEAD, SVNDepth.INFINITY, false, true, false, false, status -> {
            if (status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                log.info("add file {}", status.getFile().getName());
                clientManager.getWCClient().doAdd(status.getFile(), false, false, false, SVNDepth.INFINITY, false, true);
            }
        }, null);

        // ???????????????commit??????
        SVNCommitClient commitClient = clientManager.getCommitClient();
        SVNCommitPacket svnCommitPacket = commitClient.doCollectCommitItems(new File[]{WORK_DIR}, false, true, SVNDepth.INFINITY, null);
        commitClient.doCommit(svnCommitPacket, false, deployer + ": ??????SVN??????????????????");
        return true;
    }

    /** ??????DLL??????????????? */
    private void copyDllAndStatic(List<DeployTaskItem> deployTaskItems, Env env, String staticPath) throws SVNException {
//        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        // ??????DLL???????????????
        String dateName = DateUtils.dateTime() + "-" + env.getName();
        List<String> staticFileList = new ArrayList<>();
        for (DeployTaskItem deployTaskItem : deployTaskItems) {
            TaskRecord taskRecord = deployTaskItem.getTaskRecord();
            // ??????SQL????????????SQL???????????????JIRA???????????????SQL?????????
            List<String> sqlFileList = collectSql(taskRecord.getJiraNo(), env.getSrcPath());
            // ????????????????????????????????????????????????????????????
            List<String> statics = collectStatic(deployTaskItem, env.getSrcPath());
            staticFileList.addAll(statics);

//            OptionalLong opt = deployTaskItem.getLogEntries().stream().mapToLong(SVNLogEntry::getRevision).max();
//            SVNRevision copyRevision = opt.isPresent() ? SVNRevision.create(opt.getAsLong()) : SVNRevision.HEAD;
//            List<SVNCopySource> scs = new ArrayList<>();

            String dirName = taskRecord.getPrincipal() + "-" + taskRecord.getJiraNo() + "-" + taskRecord.getDemandName();
            // ??????????????????????????????????????????????????????
            for (String file : sqlFileList) {
                File copyFile = new File(file);
                if (!copyFile.exists()) {
                    throw new RuntimeException("???????????????????????????????????????????????????\n?????????" + env.getSrcPath() + "\n?????????" + copyFile.getAbsolutePath());
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
                throw new RuntimeException("???????????????????????????????????????????????????\n?????????" + env.getSrcPath() + "\n?????????" + copyFile.getAbsolutePath());
            }
            File distFile = new File(WORK_DIR, PathUtils.peekPath(staticPath, dateName, "other", copyFile.getName()));
            FileUtil.copyFile(copyFile, distFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * checkout???????????????env???????????????
     */
    private void checkoutAndUpdateEnv(Env env) throws SVNException {
        if (WORK_DIR.exists()) {
            // ???????????????????????????
            clearWc();
        }

        // checkout
        SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(false), USER_NAME, PASS_WD);
        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        SVNURL SVN_URL = SVNURL.parseURIEncoded(env.getRepoUrl());

        updateClient.doCheckout(SVN_URL, WORK_DIR, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
    }

    /**
     * ???????????????????????????????????????
     */
    private List<DeployTaskItem> mergeBranch(Env env, List<TaskRecord> taskRecords) throws SVNException {
        List<DeployTaskItem> deployTaskItems = new ArrayList<>();
        // ???????????????JIRA????????????????????????????????????commit
        Map<String, List<SVNLogEntry>> mergeMap = collectCommit(taskRecords, env);
        // ???????????????????????????????????????????????????????????????????????????
        for (TaskRecord taskRecord : taskRecords) {
            DeployTaskItem deployTaskItem = new DeployTaskItem();
            String log;
            int status = TaskStatusEnum.MERGED.val(); //???????????????????????????
            // ????????????????????????????????????????????????sit?????????
            if (StringUtils.isEmpty(env.getSrcPath())) {
                log = "????????????????????????";
            } else {
                try {
                    // ??????????????????????????????
                    List<SVNLogEntry> logEntries = mergeMap.get(taskRecord.getJiraNo());
                    deployTaskItem.setLogEntries(logEntries);
                    if (CollectionUtils.isEmpty(logEntries)) {
                        log = "???????????????????????????";
                    } else {
                        log = mergeTaskCommit(env, logEntries);
                    }
                } catch (SVNException e) {
                    SVNErrorCode errorCode = e.getErrorMessage().getErrorCode();
                    if (SVNErrorCode.WC_SCHEDULE_CONFLICT.getCode() == errorCode.getCode() ||
                        SVNErrorCode.WC_FOUND_CONFLICT.getCode() == errorCode.getCode()) {
                        log = "???????????????????????????????????????\n" + e.getErrorMessage().getFullMessage();
                    } else {
                        log = "????????????\n" + getExceptStack(e);
                    }
                    status = TaskStatusEnum.MERGE_ERR.val(); //????????????
                }
            }
            deployTaskItem.setLog(log);
            deployTaskItem.setTaskStatus(status);
            deployTaskItem.setTaskRecord(taskRecord);
            deployTaskItems.add(deployTaskItem);
        }
        return deployTaskItems;
    }

    /** ????????????????????? */
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

    /** ??????Commit */
    private String mergeTaskCommit(Env env, List<SVNLogEntry> logEntries) throws SVNException {
        // ??????Commit
        StringBuffer sb = new StringBuffer();
        for (SVNLogEntry logEntry : logEntries) {
            // ??????????????????
            sb.append(parseLog(logEntry));
            // ??????
            svnRepoHolder.merge(env.getRepoUrl(), env.getSrcPath(), env.getPath(), logEntry.getRevision());
        }
        // ???????????????????????????
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
            String errorMsg = "????????????\n" + String.join("\n", conflictList);
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT, errorMsg));
        }

        return sb.toString();
    }

    /** ?????????????????????Commit */
    private Map<String, List<SVNLogEntry>> collectCommit(List<TaskRecord> taskRecords, Env env) throws SVNException {
        if (StringUtils.isEmpty(env.getSrcPath())) {
            return new HashMap<>();
        }
        // ???????????????????????????????????????JIRA????????????????????????JIRA????????????????????????
        Map<String, List<SVNLogEntry>> mergeMap = new HashMap<>();
        // TODO ???????????????startRevision???????????????????????????????????????????????????
        svnRepoHolder.log(env.getRepoUrl(), env.getSrcPath(), 0, -1, logEntry -> {
            // ???????????????message?????????????????????????????????JIRA???????????????
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

    /** ??????????????????sql?????? */
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
    /** ???????????????????????????????????? */
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

    /** ?????????????????? */
    private boolean compileDlls(List<DeployTaskItem> deployTaskItems, Env env, String staticPath) {

        try {
            List<TaskRecord> taskRecords = deployTaskItems.stream().map(DeployTaskItem::getTaskRecord).collect(Collectors.toList());
//            Set<String> dlls = new HashSet<>();

//            for (TaskRecord taskRecord : taskRecords) {
//                if (StringUtils.isEmpty(taskRecord.getOutDlls())) {
//                    continue;
//                }
//                String dirName = taskRecord.getPrincipal() + "-" + taskRecord.getJiraNo() + "-" + taskRecord.getDemandName();
//                for (String dll : taskRecord.getOutDlls().split(",")) {
//                    String fp = PathUtils.peekPath(staticPath, dateName, "dll", dll);
//                    FileUtil.del(new File(WORK_DIR, fp));
//                    File touch = FileUtil.touch(WORK_DIR, fp);
//                    FileUtil.appendString(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date()), touch, CharsetUtil.UTF_8);
//                }
//                dlls.addAll(Arrays.stream(taskRecord.getOutDlls().split(",")).collect(Collectors.toList()));
//            }

            // ?????????????????????dll
            List<String> allDlls = taskRecords.stream()
                    .map(TaskRecord::getOutDlls)
                    .filter(StringUtils::isNotEmpty)
                    .map(dlls -> Arrays.stream(dlls.split(",")).collect(Collectors.toList()))
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());

            File compileDir = new File(WORK_DIR, env.getPath());
            FileUtil.copyContent(compileDir.toPath(), WORK_COMPILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String slnPath = getSlnPath(WORK_COMPILE);
            // ??????????????????
            for (String dll : allDlls) {
                String cmd = env.getCompileCmd() + " " + slnPath + " /Project " + dll + " /rebuild";
                System.out.println(cmd);
                Runtime.getRuntime().exec("cmd /c " + cmd);
            }

            // ??????????????????????????????
            Thread.sleep(env.getCompileWait() * 1000);

            // ??????Dll??????
            String dateName = DateUtils.dateTime() + "-" + env.getName();
            for (String dll : allDlls) {
                String src = PathUtils.peekPath("Debug", dll + ".dll");
                File srcFile = new File(WORK_COMPILE, src);
                if (!srcFile.exists()) {
                    src = PathUtils.peekPath("x64", "Debug", dll + ".dll");
                    srcFile = new File(WORK_COMPILE, src);
                    if (!srcFile.exists()) {
                        throw new RuntimeException(dll + ".dll ???????????????");
                    }
                }
                String dest = PathUtils.peekPath(staticPath, dateName, "dll", dll + ".dll");
                File destFile = new File(WORK_DIR, dest);
                FileUtil.del(destFile);
                FileUtil.copyFile(srcFile, destFile);
            }
        } catch (Exception e) {
            for (DeployTaskItem taskItem : deployTaskItems) {
                taskItem.setLog(e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private String getSlnPath(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new RuntimeException("??????DLL????????????sln??????");
        }
        String slnPath = "";
        for (File file : files) {
            if (file.getName().endsWith(".sln")) {
                slnPath = file.getAbsolutePath();
                break;
            }
        }
        if (StringUtils.isEmpty(slnPath)) {
            throw new RuntimeException("??????DLL????????????sln??????");
        }
        return slnPath;
    }

    /** ???????????????????????? */
    private void sendNotifyEmail(List<DeployTaskItem> deployTaskItems, Env env, String msg) {
        // ??????????????????
        DeployConfig deployConfig = taskRecordMapper.selectConfig();
        if (deployConfig == null) {
            log.error("?????????????????????");
            return;
        }
        if (StringUtils.isEmpty(deployConfig.getNotifyEmails())) {
            log.info("?????????????????????");
            return;
        }
        List<String> emails = Arrays.stream(deployConfig.getNotifyEmails().split(",")).collect(Collectors.toList());
        try {
            DeployConfig.MailConfig mailConfig = JSON.parseObject(deployConfig.getMailConfig(), DeployConfig.MailConfig.class);
            String title = String.format("SVN??????????????????(??????%s) - %s", env.getName(), msg);
            StringBuilder content = new StringBuilder("<pre>");
            deployTaskItems.forEach(e -> {
                // ??????Commit
                content.append(String.format("JIRA???%s\n????????????%s\n", e.getTaskRecord().getJiraNo(), e.getTaskRecord().getDemandName()));
                content.append("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\n");
                content.append(e.getLog());
                content.append("\n");
            });
            content.append("</pre>");
//            sendMail(mailConfig, emails, title, content.toString());
        } catch (Exception e) {
            log.warn("????????????????????????", e);
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

    /** ?????????????????? */
    private void saveDeployInfo(List<DeployTaskItem> deployTaskItems, Env env, String deployer) {
        // ??????????????????
        List<TaskLog> taskLogs = deployTaskItems.stream().map(e -> new TaskLog(e.getTaskRecord().getId(), e.getLog(), e.getTaskStatus(), deployer)).collect(Collectors.toList());
        taskLogMapper.batchInsert(taskLogs);
        // ????????????????????????
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
        result.append("\n?????????????????? : ");
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
