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
 * @date 2022/11/3 20:47
 */
public enum TaskStatusEnum {
    UN_DEPLOY   (0), //未发布
    MERGED      (1), //已合并
    MERGE_ERR   (2), //合并失败
    COMPILED    (3), //已编译
    COMPILE_ERR (4), //编译失败
    PUSH_ERR    (5), //推送失败
    SUCCESS     (6), //发布成功
    FAIL        (7), //发布失败
    ;
    private Integer val;

    TaskStatusEnum(Integer val) {
        this.val = val;
    }

    public Integer val() {
        return val;
    }
}
