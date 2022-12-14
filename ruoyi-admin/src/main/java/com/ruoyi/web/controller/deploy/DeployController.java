/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.web.controller.deploy;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.DeployConfig;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ITaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author duhg
 * @date 2022/10/28 18:59
 */
@RestController
@RequestMapping("/deploy")
public class DeployController extends BaseController {

    @Autowired
    private ITaskRecordService taskRecordService;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 获取发布任务列表
     */
    @GetMapping("/list")
    public TableDataInfo list(TaskRecordQueryVo queryVo) {
        List<SysRole> sysRoles = roleService.selectRolesByUserId(getUserId());
        if (sysRoles == null || sysRoles.stream().noneMatch(e -> "admin".equals(e.getRoleKey()))) {
            queryVo.setCreateBy(getUsername());
        }
        startPage();
        List<TaskRecord> list = taskRecordService.selectList(queryVo);
        return getDataTable(list);
    }

    /**
     * 保存更新dlls
     */
    @GetMapping("/saveDll")
    public AjaxResult saveDll(@RequestParam Long id, @RequestParam String outDlls) {
        return toAjax(taskRecordService.saveDll(id, outDlls));
    }

    /**
     * 添加发布任务
     */
    @PostMapping("/addTask")
    public AjaxResult addTask(@RequestBody TaskRecord taskRecord) {
        // 设置创建人
        taskRecord.setCreateBy(getUsername());
        return toAjax(taskRecordService.addTask(taskRecord));
    }

    /**
     * 批量导入发布任务
     */
    @Log(title = "发布任务导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importTask")
    public AjaxResult importTask(MultipartFile file, String env) throws Exception {
        if (StringUtils.isEmpty(env)) {
            throw new ServiceException("参数错误，环境字段为空！");
        }
        if (file == null) {
            throw new ServiceException("参数错误，导入文件为空！");
        }
        ExcelUtil<TaskRecord> util = new ExcelUtil<>(TaskRecord.class);
        List<TaskRecord> taskList = util.importExcel(file.getInputStream(), 1, 5);
        String createBy = getUsername();
        return toAjax(taskRecordService.importTask(taskList, env, createBy));
    }

    /**
     * 执行入发布任务
     */
    @Log(title = "发布任务", businessType = BusinessType.DEPLOY)
    @PostMapping("/deploy")
    public AjaxResult deploy(@RequestParam List<Long> taskIds, @RequestParam String env) throws Exception {
        String opt = getUsername();
        return toAjax(taskRecordService.deploy(taskIds, env, opt));
    }

    /**
     * 查看发布详情
     */
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam Long taskId) throws Exception {
        return AjaxResult.success(taskRecordService.detail(taskId));
    }

    /**
     * 保存配置信息
     */
    @Log(title = "保存配置信息", businessType = BusinessType.DEPLOY_CONFIG)
    @PostMapping("/config")
    public AjaxResult saveConfig(@RequestBody DeployConfig config) throws Exception {
        return AjaxResult.success(taskRecordService.saveConfig(config));
    }

    /**
     * 保存配置信息
     */
    @GetMapping("/config")
    public AjaxResult getConfig() throws Exception {
        return AjaxResult.success(taskRecordService.getConfig());
    }

}
