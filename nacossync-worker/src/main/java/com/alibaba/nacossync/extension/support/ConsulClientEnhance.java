package com.alibaba.nacossync.extension.support;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;;

/**
 * @description: 支持PUT 方法传递自定义Header 头
 * @author: wenhui.bai
 * @email: wenhui.bai@shihengtech.com
 * @create: 2022-02-17 15:43
 **/
public class ConsulClientEnhance extends ConsulClient {

    private final ConsulRawClientEnhance consulRawClientEnhance;


    public ConsulClientEnhance(String agentHost, int agentPort) {
        super(new ConsulRawClientEnhance(agentHost, agentPort));
        this.consulRawClientEnhance = new ConsulRawClientEnhance(agentHost, agentPort);
    }

    public Response<Void> agentServiceDeregister(String serviceId, String token,String serviceInstanceAddress) {
        return  consulRawClientEnhance.agentServiceDeregister(serviceId, "", serviceInstanceAddress);
    }
}
