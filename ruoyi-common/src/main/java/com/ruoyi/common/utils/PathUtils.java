/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.common.utils;

import java.io.File;

/**
 * @author duhg
 * @date 2022/11/7 21:14
 */
public class PathUtils {

    public static String peekPath(String ... paths) {
        StringBuilder peek = new StringBuilder();
        for (String path : paths) {
            if (!path.startsWith("/")) {
                path = File.separator + path;
            }
            peek.append(path);
        }
        return peek.toString();
    }

}
