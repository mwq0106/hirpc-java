package com.hirpc.loadbalance.impl;

import com.hirpc.loadbalance.Balancer;
import com.hirpc.handler.RpcClientHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public class RoundRobinBalancer implements Balancer {

    private volatile Map<String, AtomicInteger> requestMap = new ConcurrentHashMap<>();
    @Override
    public RpcClientHandler elect(String service, List<RpcClientHandler> candidates) {
        AtomicInteger lastIndex = requestMap.get(service);
        if (lastIndex == null) {
            requestMap.put(service, new AtomicInteger(0));
            lastIndex = requestMap.get(service);
        }
        int index = lastIndex.getAndAdd(1) %  candidates.size();

        return candidates.get(index);
    }

}