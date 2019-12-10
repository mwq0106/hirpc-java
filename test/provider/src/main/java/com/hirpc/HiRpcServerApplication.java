package com.hirpc;

import com.hirpc.config.ProtocolConfig;
import com.hirpc.registry.ServiceRegistry;
import com.hirpc.registry.impl.ZooKeeperServiceRegistry;
import com.hirpc.server.RpcServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

/**
 * @author mwq0106
 * @date 2019/9/26
 */
@SpringBootApplication
public class HiRpcServerApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(HiRpcServerApplication.class)
                // 非 Web 应用
                .web(WebApplicationType.NONE)
                .run(args);
    }
    @Value("${hirpc.registry.address}")
    private String zkAddresses;
    @Resource
    private ProtocolConfig protocolConfig;
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RpcServer rpcServer() {
        protocolConfig.setPort(9081);
        RpcServer rpcServer = new RpcServer();
        ServiceRegistry serviceRegistry = new ZooKeeperServiceRegistry(zkAddresses);
        rpcServer.setServiceRegistry(serviceRegistry);
        return rpcServer;
    }
}
