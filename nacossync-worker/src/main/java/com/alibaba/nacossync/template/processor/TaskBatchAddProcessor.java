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
package com.alibaba.nacossync.template.processor;

import com.alibaba.nacos.common.http.HttpUtils;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.exception.SkyWalkerException;
import com.alibaba.nacossync.extension.SyncManagerService;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.pojo.request.TaskBatchAddRequest;
import com.alibaba.nacossync.pojo.result.TaskBatchAddResult;
import com.alibaba.nacossync.template.Processor;
import com.alibaba.nacossync.util.SkyWalkerUtil;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author NacosSync
 * @version $Id: TaskAddProcessor.java, v 0.1 2018-09-30 PM11:40 NacosSync Exp $$
 */
@Slf4j
@Service
public class TaskBatchAddProcessor implements Processor<TaskBatchAddRequest, TaskBatchAddResult> {

    private final SyncManagerService syncManagerService;

    private final TaskAccessService taskAccessService;

    private final ClusterAccessService clusterAccessService;

    private static final String DEFAULT_CONSUL_SERVICE_NAME = "consul";

    private final ObjectMapper objectMapper;

    public TaskBatchAddProcessor(SyncManagerService syncManagerService,
                                 TaskAccessService taskAccessService, ClusterAccessService clusterAccessService,
                                 ObjectMapper objectMapper) {
        this.syncManagerService = syncManagerService;
        this.taskAccessService = taskAccessService;
        this.clusterAccessService = clusterAccessService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(TaskBatchAddRequest taskBatchAddRequest, TaskBatchAddResult taskBatchAddResult,
                        Object... others) throws Exception {

        ClusterDO destCluster = clusterAccessService.findByClusterId(taskBatchAddRequest.getDestClusterId());

        ClusterDO sourceCluster = clusterAccessService.findByClusterId(taskBatchAddRequest.getSourceClusterId());

        if (null == destCluster || null == sourceCluster) {

            throw new SkyWalkerException("请检查源或者目标集群是否存在");

        }

        if (null == syncManagerService.getSyncService(sourceCluster.getClusterId(), destCluster.getClusterId())) {

            throw new SkyWalkerException("不支持当前同步类型");
        }

        // only support consul
        String clusterType = sourceCluster.getClusterType();
        if (!ClusterTypeEnum.CONSUL.getCode().equals(clusterType)) {
            throw new SkyWalkerException("批量同步当前只支持Consul");
        }


        String connectKeyListString = sourceCluster.getConnectKeyList();

        List<String> connectKeyList = objectMapper.readerForListOf(String.class)
                .readValue(connectKeyListString);

        String sourceConsulServerAddress = connectKeyList.get(0);
        String consulAddress = String.format("http://%s/v1/catalog/services", sourceConsulServerAddress);
        URI uri = HttpUtils.buildUri(consulAddress, null);
        log.info("[NacosSync] 从consul 原集群批量同步服务 {}" , consulAddress);

        ConsulClient consulClient = new ConsulClient(uri.getHost(), uri.getPort());

        CatalogServicesRequest request = CatalogServicesRequest.newBuilder().build();

        Response<Map<String, List<String>>> catalogServices = consulClient.getCatalogServices(request);
        catalogServices.getValue().remove(DEFAULT_CONSUL_SERVICE_NAME);

        if (ObjectUtils.isEmpty(catalogServices) || ObjectUtils.isEmpty(catalogServices.getValue().keySet())) {
            throw new SkyWalkerException("批量同步读取服务列表为空，请检查。");
        }

        Map<String, List<String>> sourceConsulServiceCache = catalogServices.getValue();
        sourceConsulServiceCache.remove(DEFAULT_CONSUL_SERVICE_NAME);
        Set<String> sourceServiceNames = sourceConsulServiceCache.keySet();

        for (String sourceServiceName : sourceServiceNames) {

            taskBatchAddRequest.setServiceName(sourceServiceName);
            String taskId = SkyWalkerUtil.generateTaskId(taskBatchAddRequest);

            TaskDO taskDO = taskAccessService.findByTaskId(taskId);

            if (null == taskDO) {

                taskDO = new TaskDO();
                taskDO.setTaskId(taskId);
                taskDO.setDestClusterId(taskBatchAddRequest.getDestClusterId());
                taskDO.setSourceClusterId(taskBatchAddRequest.getSourceClusterId());

                taskDO.setServiceName(sourceServiceName);
                taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
                taskDO.setWorkerIp(SkyWalkerUtil.getLocalIp());
                taskDO.setOperationId(SkyWalkerUtil.generateOperationId());

            } else {

                taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
                taskDO.setOperationId(SkyWalkerUtil.generateOperationId());
            }

            taskAccessService.addTask(taskDO);
        }
    }
}
