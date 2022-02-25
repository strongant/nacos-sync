package com.alibaba.nacossync.pojo.result;

import lombok.Data;

/**
 * @description: 统计原集群和目标集群有效服务实例数量
 * @author: wenhui.bai
 * @email: wenhui.bai@shihengtech.com
 * @create: 2022-02-24 09:57
 **/
@Data
public class ClusterSyncResult extends BaseResult {

    /**
     * 原集群有效服务实例数量
     */
    private Integer sourceServiceInstanceCount;


    /**
     * 目标集群有效服务实例数量
     */
    private Integer destServiceInstanceCount;

    /**
     * 同步完成结果
     */
    private boolean syncResult;

}
