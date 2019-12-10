package com.hirpc.loadbalance;

import com.hirpc.handler.RpcClientHandler;

import java.util.List;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public interface Balancer {

    /**
     * 从候选地址<code>candidates</code>中根据一定的负载均衡算法选出一台服务器的地址
     *
     * @param service 服务
     * @param candidates  服务的候选地址
     * @return 选中的服务器的地址
     */
    RpcClientHandler elect(String service, List<RpcClientHandler> candidates);

}