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
package com.alibaba.nacossync.api;

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.holder.ConsulServerHolder;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.alibaba.nacossync.pojo.request.ClusterAddRequest;
import com.alibaba.nacossync.pojo.request.ClusterDeleteRequest;
import com.alibaba.nacossync.pojo.request.ClusterDetailQueryRequest;
import com.alibaba.nacossync.pojo.request.ClusterListQueryRequest;
import com.alibaba.nacossync.pojo.result.*;
import com.alibaba.nacossync.template.SkyWalkerTemplate;
import com.alibaba.nacossync.template.processor.ClusterAddProcessor;
import com.alibaba.nacossync.template.processor.ClusterDeleteProcessor;
import com.alibaba.nacossync.template.processor.ClusterDetailQueryProcessor;
import com.alibaba.nacossync.template.processor.ClusterListQueryProcessor;
import com.alibaba.nacossync.timer.CheckServiceOnlyOneInstanceTaskTimer;
import com.alibaba.nacossync.util.ConsulUtils;
import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.CatalogNodesRequest;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author NacosSync
 * @version $Id: ClusterApi.java, v 0.1 2018-09-25 PM9:30 NacosSync Exp $$
 */
@Slf4j
@RestController
@Api
public class ClusterApi {

    private final ClusterAddProcessor clusterAddProcessor;

    private final ClusterDeleteProcessor clusterDeleteProcessor;

    private final ClusterDetailQueryProcessor clusterDetailQueryProcessor;

    private final ClusterListQueryProcessor clusterListQueryProcessor;

    private final ConsulServerHolder destConsulServerHolder;

    public ClusterApi(
        ClusterAddProcessor clusterAddProcessor, ClusterDeleteProcessor clusterDeleteProcessor,
        ClusterDetailQueryProcessor clusterDetailQueryProcessor, ClusterListQueryProcessor clusterListQueryProcessor,
        ConsulServerHolder destConsulServerHolder) {
        this.clusterAddProcessor = clusterAddProcessor;
        this.clusterDeleteProcessor = clusterDeleteProcessor;
        this.clusterDetailQueryProcessor = clusterDetailQueryProcessor;
        this.clusterListQueryProcessor = clusterListQueryProcessor;
        this.destConsulServerHolder = destConsulServerHolder;
    }

    @RequestMapping(path = "/v1/cluster/list", method = RequestMethod.GET)
    public ClusterListQueryResult clusters(ClusterListQueryRequest clusterListQueryRequest) {

        return SkyWalkerTemplate.run(clusterListQueryProcessor, clusterListQueryRequest,
                new ClusterListQueryResult());
    }

    @RequestMapping(path = "/v1/cluster/detail", method = RequestMethod.GET)
    public ClusterDetailQueryResult getByTaskId(ClusterDetailQueryRequest clusterDetailQueryRequest) {

        return SkyWalkerTemplate.run(clusterDetailQueryProcessor, clusterDetailQueryRequest,
                new ClusterDetailQueryResult());
    }

    @RequestMapping(path = "/v1/cluster/delete", method = RequestMethod.DELETE)
    public ClusterDeleteResult deleteCluster(ClusterDeleteRequest clusterDeleteRequest) {

        return SkyWalkerTemplate.run(clusterDeleteProcessor, clusterDeleteRequest,
                new ClusterDeleteResult());

    }

    @RequestMapping(path = "/v1/cluster/add", method = RequestMethod.POST)
    public ClusterAddResult clusterAdd(@RequestBody ClusterAddRequest clusterAddRequest) {

        return SkyWalkerTemplate
                .run(clusterAddProcessor, clusterAddRequest, new ClusterAddResult());
    }

    @RequestMapping(path = "/v1/cluster/types", method = RequestMethod.GET)
    public ClusterTypeResult getClusterType() {

        return new ClusterTypeResult(ClusterTypeEnum.getClusterTypeCodes());
    }


    @RequestMapping(path = "/v1/cluster/syncResult", method = RequestMethod.GET)
    public ClusterSyncResult syncResult(@RequestParam("sourceClusterId") String sourceClusterId, @RequestParam("destClusterId") String destClusterId) {

        ClusterSyncResult clusterSyncResult = new ClusterSyncResult();


        ConsulClientEnhance sourceConsulClientEnhance = getConsulClientEnhance(sourceClusterId);
        ConsulClientEnhance destConsulClientEnhance = getConsulClientEnhance(destClusterId);


        compareHealthServiceInstances(sourceConsulClientEnhance, destConsulClientEnhance,clusterSyncResult);


        CatalogNodesRequest catalogNodesRequest = CatalogNodesRequest.newBuilder()
                .setQueryParams(QueryParams.DEFAULT)
                .build();

        log.info("原集群:{} , 目标集群:{} 服务实例同步结果:{}" , sourceConsulClientEnhance.getCatalogNodes(catalogNodesRequest).getValue(),
                destConsulClientEnhance.getCatalogNodes(catalogNodesRequest).getValue(),clusterSyncResult.getSuccess());

        return clusterSyncResult;
    }


    /**
     * 自动摘除目标consul 集群上的所有服务实例忽略服务实例健康检查状态
     * @param destClusterId
     * @return
     */
    @RequestMapping(path = "/v1/cluster/deregisterAllServiceInstances", method = RequestMethod.GET)
    public ClusterSyncResult deregisterAllServiceInstances(@RequestParam("destClusterId") String destClusterId) {
        ConsulClientEnhance destConsulClientEnhance = getConsulClientEnhance(destClusterId);
        ClusterSyncResult clusterSyncResult = new ClusterSyncResult();

        if (Objects.isNull(destConsulClientEnhance)) {
            clusterSyncResult.setSuccess(false);
            return clusterSyncResult;
        }

        CatalogServicesRequest catalogServicesRequest = CatalogServicesRequest.newBuilder()
                .setQueryParams(QueryParams.DEFAULT)
                .build();

        Map<String, List<String>> catalogServices = destConsulClientEnhance.getCatalogServices(catalogServicesRequest).getValue();

        for (String key : catalogServices.keySet()) {

            if (key.equals("consul")) {
                continue;
            }

            HealthServicesRequest servicesRequest = HealthServicesRequest.newBuilder()
                    .setQueryParams(QueryParams.DEFAULT)
                    .build();

            List<HealthService> healthServiceList = destConsulClientEnhance.getHealthServices(key, servicesRequest).getValue();
            for (HealthService healthService : healthServiceList) {
                try {
                    doDeregisterService(healthService);
                } catch (Exception e) {
                    log.warn("反注册实例失败 服务实例ID:{}" , healthService.getService().getId()  ,e );
                }
            }
        }

        return clusterSyncResult;
    }


    /**
     * 自动摘除Consul 集群中服务状态为critical状态的服务实例
     * @param destClusterId
     * @return
     */
    @RequestMapping(path = "/v1/cluster/deregisterWithCriticalStatus", method = RequestMethod.GET)
    public ClusterSyncResult deregisterWithCriticalStatus(@RequestParam("destClusterId") String destClusterId) {

        ConsulClientEnhance destConsulClientEnhance = getConsulClientEnhance(destClusterId);
        ClusterSyncResult clusterSyncResult = new ClusterSyncResult();

        if (Objects.isNull(destConsulClientEnhance)) {
            clusterSyncResult.setSuccess(false);
            return clusterSyncResult;
        }

        CatalogServicesRequest catalogServicesRequest = CatalogServicesRequest.newBuilder()
                .setQueryParams(QueryParams.DEFAULT)
                .build();

        Map<String, List<String>> catalogServices = destConsulClientEnhance.getCatalogServices(catalogServicesRequest).getValue();

        for (String key : catalogServices.keySet()) {

            if (key.equals("consul")) {
                continue;
            }

            HealthServicesRequest servicesRequest = HealthServicesRequest.newBuilder()
                    .setPassing(false)
                    .setQueryParams(QueryParams.DEFAULT)
                    .build();

            List<HealthService> healthServiceList = destConsulClientEnhance.getHealthServices(key, servicesRequest).getValue();
            for (HealthService healthService : healthServiceList) {
                try {
                    if(!ConsulUtils.healthServiceValid(healthService.getChecks()) ){
                        doDeregisterService(healthService);
                    }
                } catch (Exception e) {
                    log.warn("反注册实例失败 服务实例ID:{}" , healthService.getService().getId()  ,e );
                }
            }
        }

        return clusterSyncResult;
    }

    private ConsulClientEnhance getConsulClientEnhance(@RequestParam("destClusterId") String destClusterId) {
        return destConsulServerHolder.get(destClusterId);
    }

    private void doDeregisterService(HealthService healthService) {
        String nodeAddress = healthService.getNode().getAddress();
        ConsulClient consulClient = new ConsulClient(nodeAddress, 8500);
        String id = healthService.getService().getId();
        Response<Void> deregister = consulClient.agentServiceDeregister(id);
        log.info("反注册服务实例ID:{}, 结果:{}",id, GsonFactory.getGson().toJson(deregister));
    }

    @RequestMapping(path = "/v1/cluster/registerMulti", method = RequestMethod.GET)
    public ClusterSyncResult registerMulti(@RequestParam("enable") boolean enable) {
        CheckServiceOnlyOneInstanceTaskTimer.registerMulti = enable;
        return new ClusterSyncResult();
    }

    private void compareHealthServiceInstances(ConsulClientEnhance sourceConsulClientEnhance,
                                                  ConsulClientEnhance destConsulClientEnhance,
                                                  ClusterSyncResult clusterSyncResult) {

        boolean result = true;

        CatalogServicesRequest catalogServicesRequest = CatalogServicesRequest.newBuilder()
                .setQueryParams(QueryParams.DEFAULT)
                .build();

        Map<String, List<String>> catalogServices = sourceConsulClientEnhance.getCatalogServices(catalogServicesRequest).getValue();

        int sourceServiceInstanceCount = 0;
        int destServiceInstanceCount = 0;

        for (String key : catalogServices.keySet()) {

            HealthServicesRequest servicesRequest = HealthServicesRequest.newBuilder()
                    .setPassing(true)
                    .setQueryParams(QueryParams.DEFAULT)
                    .build();

            List<HealthService> sourceHealthServiceList = sourceConsulClientEnhance.getHealthServices(key, servicesRequest).getValue();
            List<HealthService> destHealthServiceList = destConsulClientEnhance.getHealthServices(key, servicesRequest).getValue();

            List<HealthService> sourceUniqueServiceList = ConsulUtils.getUniqueServiceList(sourceHealthServiceList);
            List<HealthService> destUniqueServiceList = ConsulUtils.getUniqueServiceList(destHealthServiceList);

            sourceServiceInstanceCount += sourceUniqueServiceList.size();
            destServiceInstanceCount += destUniqueServiceList.size();

            if (sourceUniqueServiceList.size() != destUniqueServiceList.size()) {
                result = false;
                sourceUniqueServiceList.removeAll(destUniqueServiceList);
                log.info("服务名为{}的服务，服务实例:{}源集群与目标集群服务实例数量不一致，请检查！",key,sourceUniqueServiceList.get(0).getService().getId());
            }
        }

        clusterSyncResult.setSourceServiceInstanceCount(sourceServiceInstanceCount);
        clusterSyncResult.setDestServiceInstanceCount(destServiceInstanceCount);
        clusterSyncResult.setSyncResult(result);

    }

}
