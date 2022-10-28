/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.web.controller.deploy;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.TaskRecord;
import com.ruoyi.system.domain.vo.TaskRecordQueryVo;
import com.ruoyi.system.service.ITaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
