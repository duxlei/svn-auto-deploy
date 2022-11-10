/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import com.ruoyi.system.domain.TaskRecord;

import java.util.List;

/**
 * @author duhg
 * @date 2022/10/30 16:02
 */
public interface DeployProcess {

    /** 发布 */
    void deploy(String deployer, Env env, List<TaskRecord> taskRecords) throws Exception;

}
