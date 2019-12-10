package com.hirpc.loadbalance;

import com.hirpc.loadbalance.impl.ConsistentHashBalancer;
import com.hirpc.loadbalance.impl.RandomBalancer;
import com.hirpc.loadbalance.impl.RoundRobinBalancer;

/**
 * @author mwq0106
 * @date 2019/12/3
 */
public enum LoadBalance {
    RANDOM(new RandomBalancer()),
    ROUND_ROBIN(new RoundRobinBalancer()),
    CONSISTENT_HASH(new ConsistentHashBalancer());
    public Balancer balancer;
    private LoadBalance(Balancer balancer) {
        this.balancer = balancer;
    }
}
