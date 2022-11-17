/*
 * Copyright 2022 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package com.ruoyi.system.service.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sun.nio.ch.ThreadPool;

import javax.annotation.PostConstruct;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author duhg
 * @date 2022/11/17 20:44
 */
@Component
public class DeployExecutor {
    private static final Logger log = LoggerFactory.getLogger(DeployExecutor.class);

    private ThreadPoolExecutor executor;

    private final int corePoolSize = 1;
    private final int maximumPoolSize = 1;
    private final int keepAliveTime = 60;
    private final int queueSize = 50;

    @PostConstruct
    public void init() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                queue);
    }

    public void runJob(Runnable job) {
        try {
            log.info("队列等待任务数: {}", executor.getQueue().size());
            executor.execute(job);
        } catch (Exception e) {
            throw new RuntimeException("发布队列已满请稍后发布");
        }
    }

}
