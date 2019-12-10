package com.hirpc.loadbalance.impl;

import com.hirpc.loadbalance.Balancer;
import com.hirpc.handler.RpcClientHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public class RoundRobinBalancer implements Balancer {

    private volatile Map<String, AtomicInteger> requestMap = new HashMap<>();
    @Override
    public RpcClientHandler elect(String service, List<RpcClientHandler> candidates) {
        if (!requestMap.containsKey(service)) {
            requestMap.put(service, new AtomicInteger(0));
        }
        int index = requestMap.get(service).getAndAdd(1) %  candidates.size();

        return candidates.get(index);
    }

}