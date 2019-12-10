package com.hirpc.registry;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public interface ServiceRegistry {
    /**
     * 向注册中心注册服务
     *
     * @param servicePath 服务路径
     * @param serverInfo   提供服务的服务器的信息
     */
    void registerService(String servicePath, ServerInRegistry serverInfo);

}
