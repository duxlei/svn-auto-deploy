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
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.service.ITaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /**
     * 获取发布任务列表
     */
    @PreAuthorize("@ss.hasPermi('deploy:list')")
    @GetMapping("/list")
    public TableDataInfo list(TaskRecordQueryVo queryVo) {
        startPage();
        List<TaskRecord> list = taskRecordService.selectList(queryVo);
        return getDataTable(list);
    }

    /**
     * 保存更新dlls
     */
    @PreAuthorize("@ss.hasPermi('deploy:update')")
    @GetMapping("/saveDll")
    public AjaxResult saveDll(@RequestParam Long id, @RequestParam String outDlls) {
        return toAjax(taskRecordService.saveDll(id, outDlls));
    }

    /**
     * 添加发布任务
     */
    @PreAuthorize("@ss.hasPermi('deploy:add')")
    @PostMapping("/addTask")
    public AjaxResult addTask(@RequestBody TaskRecord taskRecord) {
        // 设置创建人
        taskRecord.setCreateBy(getUsername());
        return toAjax(taskRecordService.addTask(taskRecord));
    }

    /**
     * 批量导入发布任务
     */
    @Log(title = "发布任务", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('deploy:import')")
    @PostMapping("/importTask")
    public AjaxResult importTask(MultipartFile file, String env) throws Exception {
        if (StringUtils.isEmpty(env)) {
            throw new ServiceException("参数错误，环境字段为空！");
        }
        if (file == null) {
            throw new ServiceException("参数错误，导入文件为空！");
        }
        ExcelUtil<TaskRecord> util = new ExcelUtil<>(TaskRecord.class);
        List<TaskRecord> taskList = util.importExcel(file.getInputStream(), 5);
        String createBy = getUsername();
        return toAjax(taskRecordService.importTask(taskList, env, createBy));
    }

}
