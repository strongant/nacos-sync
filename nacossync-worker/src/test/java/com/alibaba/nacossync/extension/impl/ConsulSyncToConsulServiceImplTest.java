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

        result = ConsulSyncToConsulServiceImpl.findHealthURL("HTTP GET http://10.102.213.251/contextpath/actuator/health: 200  Output: {\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"diskSpace\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"total\\\":1798842351616,\\\"free\\\":784414380032,\\\"threshold\\\":10485760}},\\\"redis\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"version\\\":\\\"4.0.14\\\"}},\\\"db\\\":{\\\"status\\\":\\\"UP\\\",\\\"details\\\":{\\\"database\\\":\\\"MySQL\\\",\\\"hello\\\":1}},\\\"refreshScope\\\":{\\\"status\\\":\\\"UP\\\"},\\\"hystrix\\\":{\\\"status\\\":\\\"UP\\\"}}}");
        Assert.assertEquals("10.102.213.251/contextpath/actuator/health",result);
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