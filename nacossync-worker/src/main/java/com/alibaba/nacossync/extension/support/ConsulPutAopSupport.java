package com.alibaba.nacossync.extension.support;

import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.HttpRequest;
import com.ecwid.consul.v1.agent.model.NewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import java.net.URI;

/**
 * @description: 同步服务在向目标Consul 集群注册时，使用服务实例的请求IP伪造，用于nginx ip_hash 注册
 * @author: wenhui.bai
 * @email: wenhui.bai@shihengtech.com
 * @create: 2022-02-17 10:53
 **/
@Slf4j
@Aspect
@Component
public class ConsulPutAopSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @Before("execution(* com.ecwid.consul.transport.AbstractHttpTransport.makePutRequest(..))")
    public void logBeforeMethodCall(JoinPoint joinPoint) throws JsonProcessingException {

        HttpRequest request = (HttpRequest) joinPoint.getArgs()[0];
        String content = request.getContent();
        NewService serviceInstance = objectMapper.readValue(content, NewService.class);
        String serviceInstanceAddress = serviceInstance.getAddress();
        request.getHeaders().put(HttpHeaders.X_FORWARDED_FOR,serviceInstanceAddress);

        log.info("[NacosSync]  注册服务实例时，向目标Consul集群请求头A添加 X-Forwarded-For  : {}",  request.getHeaders());
    }


}
