package com.alibaba.nacossync.api;

import com.alibaba.nacossync.extension.holder.ConsulServerHolder;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.alibaba.nacossync.pojo.request.ClusterAddRequest;
import com.alibaba.nacossync.pojo.request.ClusterDeleteRequest;
import com.alibaba.nacossync.pojo.request.ClusterDetailQueryRequest;
import com.alibaba.nacossync.pojo.request.ClusterListQueryRequest;
import com.alibaba.nacossync.pojo.result.*;
import com.alibaba.nacossync.template.processor.ClusterAddProcessor;
import com.alibaba.nacossync.template.processor.ClusterDeleteProcessor;
import com.alibaba.nacossync.template.processor.ClusterDetailQueryProcessor;
import com.alibaba.nacossync.template.processor.ClusterListQueryProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class ClusterApiTest {
    @Mock
    ClusterAddProcessor clusterAddProcessor;
    @Mock
    ClusterDeleteProcessor clusterDeleteProcessor;
    @Mock
    ClusterDetailQueryProcessor clusterDetailQueryProcessor;
    @Mock
    ClusterListQueryProcessor clusterListQueryProcessor;
    @Mock
    ConsulServerHolder destConsulServerHolder;
    @Mock
    Logger log;
    @InjectMocks
    ClusterApi clusterApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClusters() throws Exception {
        ClusterListQueryResult result = clusterApi.clusters(new ClusterListQueryRequest());
        Assert.assertEquals(new ClusterListQueryResult(), result);
    }

    @Test
    public void testGetByTaskId() throws Exception {
        ClusterDetailQueryResult result = clusterApi.getByTaskId(new ClusterDetailQueryRequest());
        Assert.assertEquals(new ClusterDetailQueryResult(), result);
    }

    @Test
    public void testDeleteCluster() throws Exception {
        ClusterDeleteResult result = clusterApi.deleteCluster(new ClusterDeleteRequest());
        Assert.assertEquals(new ClusterDeleteResult(), result);
    }

    @Test
    public void testClusterAdd() throws Exception {
        ClusterAddResult result = clusterApi.clusterAdd(new ClusterAddRequest());
        Assert.assertEquals(new ClusterAddResult(), result);
    }

    @Test
    public void testGetClusterType() throws Exception {
        ClusterTypeResult result = clusterApi.getClusterType();
        Assert.assertEquals(new ClusterTypeResult(Arrays.<String>asList("String")), result);
    }

    @Test
    public void testSyncResult() throws Exception {
        when(destConsulServerHolder.get(anyString())).thenReturn(new ConsulClientEnhance("agentHost", 0));

        ClusterSyncResult result = clusterApi.syncResult("sourceClusterId", "destClusterId");
        Assert.assertEquals(new ClusterSyncResult(), result);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme