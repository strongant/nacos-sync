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

import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.extension.holder.ConsulServerHolder;
import com.alibaba.nacossync.extension.impl.ConsulSyncToConsulServiceImpl;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private TaskAccessService taskAccessService;

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    private  ConsulServerHolder consulServerHolder;

    @Autowired
    private ConsulSyncToConsulServiceImpl consulSyncToConsulService;


    @Value("${sync.register.max.count:3}")
    private Integer registerMaxCount;
    

    @Override
    public void run(String... args) {
        /** Fetch the task list from the database every 3 seconds */
        scheduledExecutorService.scheduleWithFixedDelay(new CheckServiceOnlyOneInstanceThread(), 0, 3000,
                TimeUnit.MILLISECONDS);

    }

    private class CheckServiceOnlyOneInstanceThread implements Runnable {

        @Override
        public void run() {

            Long start = System.currentTimeMillis();
            try {


                Iterable<TaskDO> taskDOS = taskAccessService.findAll();

                taskDOS.forEach(taskDO -> {

                    if ((TaskStatusEnum.DELETE.getCode().equals(taskDO.getTaskStatus()))) {
                        return;
                    }

                    String serviceName = taskDO.getServiceName();
                    ConsulClientEnhance destConsulClient = consulServerHolder.get(taskDO.getDestClusterId());

                    HealthServicesRequest healthServicesRequest = HealthServicesRequest.newBuilder()
                            .setPassing(true).build();

                    Response<List<HealthService>> healthServices = destConsulClient.getHealthServices(serviceName, healthServicesRequest);
                    List<HealthService> healthServiceList = healthServices.getValue();
                    if (ObjectUtils.isEmpty(healthServiceList)) {
                        return;
                    }

                    Set<String> serviceInstanceDistributionConsulClientSet = new HashSet<>();
                    Map<String,HealthService> serviceInstanceUnique = new HashMap<>();

                    for (HealthService healthService : healthServiceList) {
                        serviceInstanceDistributionConsulClientSet.add(healthService.getNode().getAddress());
                        serviceInstanceUnique.put(healthService.getService().getId(),healthService);
                    }
                    if (serviceInstanceDistributionConsulClientSet.size() >= registerMaxCount) {
                        return;
                    }

                    List<Member> agentMembers = destConsulClient.getAgentMembers().getValue();
                    List<Member> consulClientNodeList = agentMembers
                            .stream()
                            .filter(it -> it.getTags().containsKey("role") && it.getTags().get("role").equals("node"))
                            .collect(Collectors.toList());

                    Set<String> consulClientNodeSet = consulClientNodeList.stream().map(it -> it.getAddress()).collect(Collectors.toSet());
                    consulClientNodeSet.removeAll(serviceInstanceDistributionConsulClientSet);
                    
                    doChoseConsulClientServerRegister(consulClientNodeSet,serviceInstanceUnique,taskDO);
                });

            } catch (Exception e) {
                log.error("CheckServiceOnlyOneInstanceThread Exception", e);
            }

            metricsManager.record(MetricsStatisticsType.DISPATCHER_TASK, System.currentTimeMillis() - start);
        }
    }

    private void doChoseConsulClientServerRegister(Set<String> consulClientNodeSet, Map<String, HealthService> serviceInstanceUnique,
                                                   TaskDO taskDO) {

        String[] serviceIdKeyArray = serviceInstanceUnique.keySet().toArray(new String[]{});
        String[] consulClientNodes = consulClientNodeSet.toArray(new String[]{});

        for (int index = 0; index < serviceIdKeyArray.length; index++) {
            String serviceId = serviceIdKeyArray[index];
            int matchIndex = (index + 1) % consulClientNodeSet.size();
            String consulClientNodeAddress = consulClientNodes[matchIndex];
            ConsulClient consulClient = new ConsulClient(consulClientNodeAddress,8500);
            HealthService healthService = serviceInstanceUnique.get(serviceId);

            try {

                consulClient.agentServiceRegister(consulSyncToConsulService.buildSyncInstance(healthService, taskDO));
                log.info("CheckServiceOnlyOneInstanceThread 冗余注册服务ID:{} , 服务IP: {}  服务端口： {},注册到{} Consul Client Node." ,
                        serviceId , healthService.getService().getAddress(),
                        healthService.getService().getPort()
                        ,consulClientNodeAddress);

            } catch (Exception e) {
                log.error("CheckServiceOnlyOneInstanceThread 冗余注册服务ID:{} , 服务IP: {}  服务端口： {},注册到{} Consul Client Node 异常,将会在下一次重试.",
                        serviceId,healthService.getService().getAddress(),healthService.getService().getPort(),consulClientNodeAddress,e);
            }
        }
    }
}
