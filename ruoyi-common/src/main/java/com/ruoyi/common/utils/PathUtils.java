/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.common.utils;

import java.io.File;
import java.util.regex.Matcher;

/**
 * @author duhg
 * @date 2022/11/7 21:14
 */
public class PathUtils {

    /**
     * 将地址统一转成 /abc 格式
     * eg: abc   => /abc
     * eg: abc/  => /abc
     * eg: /abc/ => /abc
     * @param paths
     * @return
     */
    public static String peekPath(String ... paths) {
        StringBuilder peek = new StringBuilder();
        for (String path : paths) {
            path = path.replaceAll("/", Matcher.quoteReplacement(File.separator));
            if (!path.startsWith(File.separator)) {
                path = File.separator + path;
            }
            peek.append(path);
        }
        return peek.toString();
    }

}
