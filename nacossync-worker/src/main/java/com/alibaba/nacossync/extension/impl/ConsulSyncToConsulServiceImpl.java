/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alibaba.nacossync.extension.impl;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.constant.SkyWalkerConstants;
import com.alibaba.nacossync.extension.SyncService;
import com.alibaba.nacossync.extension.annotation.NacosSyncService;
import com.alibaba.nacossync.extension.event.SpecialSyncEventBus;
import com.alibaba.nacossync.extension.holder.ConsulServerHolder;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.util.ConsulUtils;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支持 Consul 同步 Consul
 */
@Slf4j
@NacosSyncService(sourceCluster = ClusterTypeEnum.CONSUL, destinationCluster = ClusterTypeEnum.CONSUL)
public class ConsulSyncToConsulServiceImpl implements SyncService {

    private static  final         Pattern pattern = Pattern.compile("http:\\/\\/\\s?(\\d*.\\d*.\\d*.\\d*:?\\d*/[a-zA-Z]*/?[a-zA-Z]*)", Pattern.CASE_INSENSITIVE);

    @Autowired
    private MetricsManager metricsManager;

    private final ConsulServerHolder consulServerHolder;
    private final SkyWalkerCacheServices skyWalkerCacheServices;

    private final ConsulServerHolder destConsulServerHolder;

    private final SpecialSyncEventBus specialSyncEventBus;

    @Autowired
    public ConsulSyncToConsulServiceImpl(ConsulServerHolder consulServerHolder,
                                         SkyWalkerCacheServices skyWalkerCacheServices,
                                         ConsulServerHolder destConsulServerHolder,
                                         SpecialSyncEventBus specialSyncEventBus) {
        this.consulServerHolder = consulServerHolder;
        this.skyWalkerCacheServices = skyWalkerCacheServices;
        this.destConsulServerHolder = destConsulServerHolder;
        this.specialSyncEventBus = specialSyncEventBus;
    }

    @Override
    public boolean delete(TaskDO taskDO) {

        try {
            specialSyncEventBus.unsubscribe(taskDO);

            ConsulClientEnhance destConsulClient = destConsulServerHolder.get(taskDO.getDestClusterId());
            Response<List<HealthService>> allInstances = destConsulClient.getHealthServices(taskDO.getServiceName(), true , QueryParams.DEFAULT);
            Set<String> consulClientNodeAddressSet = ConsulUtils.getConsulClientNodeAddressSet(destConsulClient);

            for (HealthService instance : allInstances.getValue()) {
                if (needDelete(instance.getService().getMeta(), taskDO)) {
                    for (String consulAddress : consulClientNodeAddressSet) {
                        ConsulClient consulClient = new ConsulClient(instance.getNode().getAddress(),8500);
                        try {
                            consulClient.agentServiceDeregister(instance.getService().getId(),null);
                        } catch (Exception e) {
                            log.warn("反注册服务实例失败，serviceName:{}  serviceId：{}" , instance.getService().getService(),instance.getService().getId());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("delete task from consul to consul was failed, taskId:{}", taskDO.getTaskId(), e);
            metricsManager.recordError(MetricsStatisticsType.DELETE_ERROR);
            return false;
        }
        return true;
    }

    @Override
    public boolean sync(TaskDO taskDO) {
        try {
            ConsulClient consulClient = consulServerHolder.get(taskDO.getSourceClusterId());

            ConsulClient destConsulClient = destConsulServerHolder.get(taskDO.getDestClusterId());

            List<HealthService> healthServiceList = consulClient.getHealthServices(taskDO.getServiceName(), true, QueryParams.DEFAULT).getValue();
            List<HealthService> uniqueServiceList = doUniqueServiceList(healthServiceList);

            Set<String> instanceKeys = new HashSet<>();
            overrideAllInstance(taskDO, destConsulClient, uniqueServiceList, instanceKeys);
            cleanAllOldInstance(taskDO, destConsulClient, instanceKeys);
            specialSyncEventBus.subscribe(taskDO, this::sync);
        } catch (Exception e) {
            log.error("Sync task from consul to consul was failed, taskId:{}", taskDO.getTaskId(), e);
            metricsManager.recordError(MetricsStatisticsType.SYNC_ERROR);
            return false;
        }
        return true;
    }

    private List<HealthService> doUniqueServiceList(List<HealthService> healthServiceList) {
        Set<String> ipPortSet = new HashSet<>();
        List<HealthService> newHealthServiceList = Lists.newArrayList();
        for (HealthService healthService : healthServiceList) {
            HealthService.Service service = healthService.getService();
            if (healthService.getChecks().size() > 1 && !ipPortSet.contains(String.format("%s:%s", service.getAddress(), service.getPort())))  {
                newHealthServiceList.add(healthService);
                ipPortSet.add(String.format("%s:%s", service.getAddress(), service.getPort()));
            }
        }
        return newHealthServiceList;
    }

    private void cleanAllOldInstance(TaskDO taskDO, ConsulClient destNamingService, Set<String> instanceKeys) {
        List<HealthService> allInstances = destNamingService.getHealthServices(taskDO.getServiceName(),true,QueryParams.DEFAULT).getValue();

        for (HealthService instance : allInstances) {
            try {
                if (needDelete(instance.getService().getMeta(), taskDO) && !instanceKeys.contains(composeInstanceKey(instance.getService().getAddress(), instance.getService().getPort()))) {
                    ConsulClient consulClient = new ConsulClient(instance.getNode().getAddress(),8500);
                    consulClient.agentServiceDeregister(instance.getService().getId(),null);
                }
            } catch (Exception e) {
                log.warn("服务实例cleanAllOldInstance 异常 , 服务实例ID: {}" ,instance.getService().getId(),e);
            }
        }
    }

    private void overrideAllInstance(TaskDO taskDO, ConsulClient destConsulClient,
        List<HealthService> healthServiceList, Set<String> instanceKeys) throws URISyntaxException {
        for (HealthService healthService : healthServiceList) {
            if (needSync(ConsulUtils.transferMetadata(healthService.getService().getTags()))) {
                try {
                    NewService newService = buildSyncInstance(healthService, taskDO);

                    destConsulClient.agentServiceRegister(newService);
                    instanceKeys.add(composeInstanceKey(healthService.getService().getAddress(),
                        healthService.getService().getPort()));
                } catch (Exception e) {
                    log.warn("Sync task from consul to consul was failed , healthService serviceName: {} address : {} , port : {} " ,
                            healthService.getService().getService(),healthService.getService().getAddress(),healthService.getService().getPort() , e);
                }
            }
        }
    }

    public NewService buildSyncInstance(HealthService instance, TaskDO taskDO) {

        NewService temp = new NewService();
        temp.setAddress(instance.getService().getAddress());
        temp.setPort(instance.getService().getPort());
        temp.setName(instance.getService().getService());
        temp.setTags(instance.getService().getTags());
        temp.setId(instance.getService().getId());
        NewService.Check check = new NewService.Check();
        String httpCheck = null;
        for (Check instanceCheck : instance.getChecks()) {
            if (instanceCheck.getOutput().contains("http")) {
                httpCheck = findHealthURL(instanceCheck.getOutput());
                check.setHttp(String.format("http://%s",httpCheck));
                check.setInterval("10s");
                break;
            }
        }
        temp.setCheck(check);

        Map<String, String> metaData = new HashMap<>(ConsulUtils.transferMetadata(instance.getService().getTags()));
        metaData.put(SkyWalkerConstants.DEST_CLUSTERID_KEY, taskDO.getDestClusterId());
        metaData.put(SkyWalkerConstants.SYNC_SOURCE_KEY,
            skyWalkerCacheServices.getClusterType(taskDO.getSourceClusterId()).getCode());
        metaData.put(SkyWalkerConstants.SOURCE_CLUSTERID_KEY, taskDO.getSourceClusterId());
        temp.setMeta(metaData);
        return temp;
    }

    private String composeInstanceKey(String ip, Integer port) {
        return ip + ":" + port;
    }


    public static String findHealthURL(String text) {
        Matcher matcher = pattern.matcher(text);
        String matchText = null;
        if (matcher.find()) {
            matchText =  matcher.group(1);
        }
        return matchText;
    }

}
