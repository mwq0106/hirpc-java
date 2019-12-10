package com.hirpc.loadbalance.impl;

import com.hirpc.handler.RpcClientHandler;
import com.hirpc.loadbalance.Balancer;

import java.util.List;
import java.util.Random;

/**
 * @author mwq0106
 * @date 2019/12/2
 */
public class RandomBalancer implements Balancer {
    private Random random = new Random();
    @Override
    public RpcClientHandler elect(String service, List<RpcClientHandler> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }
}
