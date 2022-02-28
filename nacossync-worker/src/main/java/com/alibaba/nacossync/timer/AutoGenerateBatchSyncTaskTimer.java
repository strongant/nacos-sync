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

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.QueryCondition;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.pojo.request.TaskBatchAddRequest;
import com.alibaba.nacossync.pojo.result.TaskBatchAddResult;
import com.alibaba.nacossync.template.SkyWalkerTemplate;
import com.alibaba.nacossync.template.processor.TaskBatchAddProcessor;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 自动生成批量同步的Consul服务
 * @author wenhui.bai
 * @version $Id: SkyWalkerServices.java, v 0.1 2018-09-26 AM1:39 NacosSync Exp $$
 */
@Slf4j
@Service
@Api
public class AutoGenerateBatchSyncTaskTimer implements CommandLineRunner {

    @Autowired
    private MetricsManager metricsManager;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    private  TaskAccessService taskAccessService;

    @Autowired
    private TaskBatchAddProcessor taskBatchAddProcessor;


    @Override
    public void run(String... args) {
        /** Fetch the task list from the database every 3 seconds */
        scheduledExecutorService.scheduleWithFixedDelay(new AutoGenerateConsulSyncTaskThread(), 0, 3000,
                TimeUnit.MILLISECONDS);

    }

    private class AutoGenerateConsulSyncTaskThread implements Runnable {

        @Override
        public void run() {

            Long start = System.currentTimeMillis();

            try {
                List<TaskDO> tasks = taskAccessService.findPageNoCriteria(0, 1).get().collect(Collectors.toList());
                if (ObjectUtils.isEmpty(tasks)) {
                    return;
                }

                TaskDO taskDO = tasks.get(0);

                TaskBatchAddRequest taskBatchAddRequest = new TaskBatchAddRequest();
                taskBatchAddRequest.setSourceClusterId(taskDO.getSourceClusterId());
                taskBatchAddRequest.setDestClusterId(taskDO.getDestClusterId());

                SkyWalkerTemplate.run(taskBatchAddProcessor, taskBatchAddRequest, new TaskBatchAddResult());

            } catch (Exception e) {
                log.error("AutoGenerateConsulSyncTaskThread Exception", e);
            }

            metricsManager.record(MetricsStatisticsType.BATCH_GENERATE_SYNC_TASK, System.currentTimeMillis() - start);
        }
    }
}
