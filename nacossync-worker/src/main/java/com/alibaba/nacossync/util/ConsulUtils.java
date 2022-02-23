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
package com.alibaba.nacossync.util;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacossync.extension.support.ConsulClientEnhance;
import com.ecwid.consul.v1.agent.model.Member;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author paderlol
 * @date: 2019-04-25 00:01
 */
public class ConsulUtils {
    public static Map<String, String> transferMetadata(List<String> tags) {
        Map<String, String> metadata = new HashMap<>();
        if (!CollectionUtils.isEmpty(tags)) {
            return tags.stream().filter(tag -> tag.split("=", -1).length == 2).map(tag -> tag.split("=", -1))
                .collect(Collectors.toMap(
                        tagSplitArray -> tagSplitArray[0]
                                .replaceAll("\\.","_")
                                .replaceAll("\'","")
                                .replaceAll("\"",""),
                        tagSplitArray -> tagSplitArray[1] , (v1,v2) -> v1));
        }
        return metadata;
    }


    /**
     * 获取所有的Consul Client 节点的IP地址
     * @param destConsulClient
     * @return
     */
    public static Set<String> getConsulClientNodeAddressSet(ConsulClientEnhance destConsulClient) {
        if (Objects.isNull(destConsulClient)) {
            throw new IllegalArgumentException("destConsulClient is Null");
        }

        List<Member> agentMembers = destConsulClient.getAgentMembers().getValue();
        List<Member> consulClientNodeList = agentMembers
                .stream()
                .filter(it -> it.getTags().containsKey("role") && it.getTags().get("role").equals("node"))
                .collect(Collectors.toList());

        Set<String> consulClientNodeSet = consulClientNodeList.stream().map(it -> it.getAddress()).collect(Collectors.toSet());
        return consulClientNodeSet;
    }
}
