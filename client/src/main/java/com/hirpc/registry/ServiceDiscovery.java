package com.hirpc.registry;

import java.util.List;

/**
 * @author mwq0106
 * @date 2019/9/21
 */
public interface ServiceDiscovery {

    void discoverAndConnectServers(final String servicePath, ServerInRegistry matchCondition) throws Exception;
}
