/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacossync.timer;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author wenhui.bai
 * @version $Id: SkyWalkerServices.java, v 0.1 2018-09-26 AM1:39 NacosSync Exp $$
 */
@Slf4j
@Service
public class CheckServiceOnlyOneInstanceTaskTimer implements CommandLineRunner {

    @Autowired
    private MetricsManager metricsManager;

    @Autowired
    private SkyWalkerCacheServices skyWalkerCacheServices;

    @Autowired
    private TaskAccessService taskAccessService;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    public void run(String... args) {
        /** Fetch the task list from the database every 3 seconds */
        scheduledExecutorService.scheduleWithFixedDelay(new CheckRunningStatusThread(), 0, 3000,
                TimeUnit.MILLISECONDS);

    }

    private class CheckRunningStatusThread implements Runnable {

        @Override
        public void run() {

            Long start = System.currentTimeMillis();
            try {


                Iterable<TaskDO> taskDOS = taskAccessService.findAll();

                taskDOS.forEach(taskDO -> {

                    if ((null != skyWalkerCacheServices.getFinishedTask(taskDO))) {
                        return;
                    }

                    log.info("从数据库中查询到所有同步任务，检查服务实例分布情况，如果有必要则发出一个同步补偿事件，对应服务: {}" , taskDO.getServiceName());
                    // TODO: 检查该服务实例是否为1个或者服务实例超过1个但是只分布在1个Consul Client 节点上，此时需要向其它Consul Client 节点同步一份,防止一个Consul Client 宕机导致服务实例不可被发现

                });

            } catch (Exception e) {
                log.warn("CheckRunningStatusThread Exception", e);
            }

            metricsManager.record(MetricsStatisticsType.DISPATCHER_TASK, System.currentTimeMillis() - start);
        }
    }
}
