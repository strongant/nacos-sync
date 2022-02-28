package com.alibaba.nacossync.extension.impl;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.event.SpecialSyncEventBus;
import com.alibaba.nacossync.extension.holder.ConsulServerHolder;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.ecwid.consul.v1.agent.model.NewService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@Slf4j
public class ConsulSyncToConsulServiceImplTest {
    @Mock
    MetricsManager metricsManager;
    @Mock
    ConsulServerHolder consulServerHolder;
    @Mock
    SkyWalkerCacheServices skyWalkerCacheServices;
    @Mock
    ConsulServerHolder destConsulServerHolder;
    @Mock
    SpecialSyncEventBus specialSyncEventBus;
    @InjectMocks
    ConsulSyncToConsulServiceImpl consulSyncToConsulServiceImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDelete() throws Exception {
        when(consulServerHolder.get(anyString())).thenReturn(new ConsulClientEnhance("agentHost", 0));
        when(destConsulServerHolder.get(anyString())).thenReturn(new ConsulClientEnhance("agentHost", 0));

        boolean result = consulSyncToConsulServiceImpl.delete(new TaskDO());
        Assert.assertEquals(true, result);
    }

    @Test
    public void testSync() throws Exception {
        when(consulServerHolder.get(anyString())).thenReturn(new ConsulClientEnhance("agentHost", 0));
        when(skyWalkerCacheServices.getClusterType(anyString())).thenReturn(ClusterTypeEnum.CS);
        when(destConsulServerHolder.get(anyString())).thenReturn(new ConsulClientEnhance("agentHost", 0));

        boolean result = consulSyncToConsulServiceImpl.sync(new TaskDO());
        Assert.assertEquals(true, result);
    }

    @Test
    public void testBuildSyncInstance() throws Exception {
        when(skyWalkerCacheServices.getClusterType(anyString())).thenReturn(ClusterTypeEnum.CS);

        NewService result = consulSyncToConsulServiceImpl.buildSyncInstance(null, new TaskDO());
        Assert.assertEquals(null, result);
    }

    @Test
    public void testFindHealthURL() throws Exception {
        String result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.213.251: 200  Output: {\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"diskSpace\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"total\\\":1798842351616,\\\"free\\\":784414380032,\\\"threshold\\\":10485760}},\\\"redis\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"version\\\":\\\"4.0.14\\\"}},\\\"db\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"database\\\":\\\"MySQL\\\",\\\"hello\\\":1}},\\\"refreshScope\\\":{\\\"status\\\":\\\"UP\\\"},\\\"hystrix\\\":{\\\"status\\\":\\\"UP\\\"}}}");
        Assert.assertEquals("10.102.213.251",result);

        result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.5.34:9066/health: 200  Output: {\\\"description\\\":\\\"Composite Discovery Client\\\",\\\"status\\\":\\\"UP\\\"}");
        Assert.assertEquals("10.102.5.34:9066/health",result);

        result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.213.251/contextpath/actuator/health: 200  Output: {\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"diskSpace\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"total\\\":1798842351616,\\\"free\\\":784414380032,\\\"threshold\\\":10485760}},\\\"redis\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"version\\\":\\\"4.0.14\\\"}},\\\"db\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"database\\\":\\\"MySQL\\\",\\\"hello\\\":1}},\\\"refreshScope\\\":{\\\"status\\\":\\\"UP\\\"},\\\"hystrix\\\":{\\\"status\\\":\\\"UP\\\"}}}");
        Assert.assertEquals("10.102.213.251/contextpath/actuator/health",result);

        result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.98.246:2184/health: 200  Output: ],\\\"unex2-item-sync-pim\\\":[\\\"ip=10.110.13.32-${NODEFLAG}\\\",\\\"ip=10.102.194.45-\\\",\\\"NODEFLAG=default\\\",\\\"ip=10.102.113.203-\\\",\\\"env=sit\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"secure=false\\\"],\\\"unex2-item-sync-pim-test\\\":[\\\"ip=10.110.13.230-${NODEFLAG}\\\",\\\"env=sit\\\",\\\"secure=false\\\",\\\"ip=10.110.13.100-${NODEFLAG}\\\",\\\"ip=10.188.213.66-${NODEFLAG}\\\",\\\"NODEFLAG=default\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"ip=10.101.6.207-${NODEFLAG}\\\"],\\\"unex2-shop\\\":[\\\"ip=10.110.13.169-${NODEFLAG}\\\",\\\"secure=false\\\",\\\"ip=10.101.6.84-${NODEFLAG}\\\",\\\"env=sit\\\",\\\"contextPath=/unex2-shop\\\",\\\"NODEFLAG=default\\\",\\\"env=${spring.profiles.active}\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"ip=10.110.13.66-${NODEFLAG}\\\",\\\"ip=192.168.3.16-${NODEFLAG}\\\",\\\"ip=10.102.213.156-\\\"],\\\"user-member\\\":[\\\"ip=10.102.205.23-\\\",\\\"NODEFLAG=default\\\",\\\"env=sit\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"contextPath=/user-member2\\\",\\\"secure=false\\\"],\\\"user-member-dev\\\":[\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"ip=10.110.6.170-${NODEFLAG}\\\",\\\"secure=false\\\",\\\"ip=10.101.6.84-${NODEFLAG}\\\",\\\"ip=10.101.6.207-${NODEFLAG}\\\",\\\"NODEFLAG=default\\\",\\\"env=${spring.profiles.active}\\\"],\\\"voucher-engine\\\":[\\\"NODEFLAG=default\\\",\\\"ip=10.102.36.145-\\\",\\\"env=sit\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"contextPath=/voucher-engine\\\",\\\"secure=false\\\"],\\\"voucher-engine-operation\\\":[\\\"contextPath=/voucher-engine-operation\\\",\\\"NODEFLAG=default\\\",\\\"ip=10.102.224.148-${NODEFLAG}\\\",\\\"env=sit\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"version=stable\\\",\\\"secure=false\\\"],\\\"voucher-engine-test\\\":[\\\"ip=10.110.6.221-default\\\",\\\"ip=10.101.6.85-default\\\",\\\"ip=10.101.6.207-default\\\",\\\"${spring.cloud.consul.discovery.customize_tags}\\\",\\\"secure=false\\\",\\\"ip=10.101.6.209-${NODEFLAG}\\\",\\\"ip=10.101.6.209-default\\\",\\\"NODEFLAG=default\\\",\\\"ip=10.101.6.84-default\\\",\\\"ip=10.101.6.208-${NODEFLAG}\\\",\\\"env=sit\\\",\\\"version=stable\\\",\\\"ip=10.110.6.125-${NODEFLAG}\\\",\\\"ip=10.101.6.208-default\\\"],\\\"wms-plugin\\\":[\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms-plugin\\\",\\\"env=sit\\\",\\\"project=wms4\\\"],\\\"wms4-adapter\\\":[\\\"wms4-adapter\\\"],\\\"wms4-allocating\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-allocating\\\"],\\\"wms4-asn\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-asn\\\"],\\\"wms4-auth\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-auth\\\"],\\\"wms4-bill\\\":[\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-bill\\\",\\\"env=sit\\\",\\\"project=wms4\\\"],\\\"wms4-bussiness-exception\\\":[\\\"app=wms4-bussiness-exception\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\"],\\\"wms4-console-web\\\":[\\\"wms4-console-web\\\"],\\\"wms4-declaration\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-declaration\\\"],\\\"wms4-feedback\\\":[\\\"secure=false\\\",\\\"app=wms4-feedback\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\"],\\\"wms4-handover\\\":[\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-handover\\\",\\\"env=sit\\\",\\\"project=wms4\\\"],\\\"wms4-info\\\":[\\\"wms4-info\\\"],\\\"wms4-inner\\\":[\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-inner\\\",\\\"env=sit\\\"],\\\"wms4-op-web\\\":[\\\"wms4-op-web\\\"],\\\"wms4-operation\\\":[\\\"wms4-operation\\\"],\\\"wms4-picking\\\":[\\\"app=wms4-picking\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\"],\\\"wms4-putaway\\\":[\\\"app=wms4-putaway\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\"],\\\"wms4-qc\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-qc\\\"],\\\"wms4-review\\\":[\\\"app=wms4-review\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\"],\\\"wms4-seeding\\\":[\\\"secure=false\\\",\\\"app=wms4-seeding\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\"],\\\"wms4-snapshot\\\":[\\\"app=wms4-snapshot\\\",\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\"],\\\"wms4-timetask\\\":[\\\"env=sit\\\",\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-timetask\\\"],\\\"wms4-upper\\\":[\\\"wms4-upper\\\"],\\\"wms4-wave\\\":[\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-wave\\\",\\\"env=sit\\\",\\\"project=wms4\\\"],\\\"wms4-web\\\":[\\\"wms4-web\\\"],\\\"wms4-work\\\":[\\\"project=wms4\\\",\\\"product=wms4\\\",\\\"boot=2\\\",\\\"secure=false\\\",\\\"app=wms4-work\\\",\\\"env=sit\\\"]}}}");
        Assert.assertEquals("10.102.98.246:2184/health",result);

        result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.149.201:2100/actuator/health: 200  Output: {\\\"status\\\":\\\"UP\\\"}");
        Assert.assertEquals("10.102.149.201:2100/actuator/health",result);
    }



    @Test
    public void testNeedSync() throws Exception {
        boolean result = consulSyncToConsulServiceImpl.needSync(new HashMap<String, String>() {{
            put("String", "String");
        }});
        Assert.assertEquals(true, result);
    }

    @Test
    public void testNeedDelete() throws Exception {
        boolean result = consulSyncToConsulServiceImpl.needDelete(new HashMap<String, String>() {{
            put("String", "String");
        }}, new TaskDO());
        Assert.assertEquals(true, result);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme