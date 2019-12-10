package com.hirpc.registry.impl;

import com.hirpc.registry.ServerInRegistry;
import com.hirpc.registry.ServiceRegistry;
import com.hirpc.constant.Constants;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mwq0106
 * @date 2019/10/16
 */

public class ZooKeeperServiceRegistry implements ServiceRegistry, IZkStateListener {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * ZooKeeper客户端实例
     *
     * 用于连接ZooKeeper服务器并根据要注册的服务创建相应节点
     *
     * 注册完服务后不能关闭该ZooKeeper客户端的连接，这是由于要进行心跳检测以确定提供服务的服务器是否掉线
     */
    private final ZkClient zkClient;

    /**
     * 所有服务在ZooKeeper下的根节点
     */
    private final String serviceRoot = Constants.ZK_SERVICE_ROOT;

    private final String serviceProvider = Constants.ZK_SERVICE_PROVIDER;

    private boolean reRegister;
    private Map<String, ServerInRegistry> serviceMap;
    /**
     * 进程ID
     */
    private String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    public ZooKeeperServiceRegistry(String zkAddress) {
        this(zkAddress, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    public ZooKeeperServiceRegistry(String zkAddress, int zkSessionTimeout, int zkConnectionTimeout) {
        if (StringUtils.isBlank(zkAddress)) {
            throw new RuntimeException("无效的ZooKeeper地址");
        }

        this.zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);
        this.zkClient.subscribeStateChanges(this);
        this.serviceMap = new HashMap<>();
        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }

        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }

        logger.debug("服务根节点（持久节点）：{}", serviceRoot);
    }


    @Override
    public void registerService(String servicePath, ServerInRegistry serverInfo) {
        StringBuilder sb = new StringBuilder();

        sb.append(serviceRoot);
        sb.append('/');
        sb.append(servicePath);

        String serviceFullPath = sb.toString();
        logger.debug("注册服务路径（持久节点）：{}", serviceFullPath);
        if (!zkClient.exists(serviceFullPath)) {
            zkClient.createPersistent(serviceFullPath);
        }
        //注册provider节点
        sb.append(serviceProvider);
        String serviceProviderPath = sb.toString();
        if (!zkClient.exists(serviceProviderPath)) {
            zkClient.createPersistent(serviceProviderPath);
        }

        sb.append("/");
        sb.append(serverInfo.toRegistryString());
        sb.append("&pid=").append(pid);
        sb.append("&timestamp=").append(System.currentTimeMillis());
        String serviceNode = sb.toString();
        //注册包含服务地址的临时节点，ZooKeeper客户端断线后该节点会自动被ZooKeeper服务器删除
        try {
            if (!this.zkClient.exists(serviceNode)) {
                this.zkClient.createEphemeral(serviceNode);
            }
        } catch (ZkNodeExistsException e) {

        }

        logger.debug("注册服务节点（临时节点）：{}", serviceNode);
        if (!this.serviceMap.containsKey(servicePath)) {
            this.serviceMap.put(servicePath, serverInfo);
        }
    }
    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        logger.debug("观察到ZooKeeper状态码：{}", state.getIntValue());

        if (reRegister && state == Watcher.Event.KeeperState.SyncConnected) {
            reRegister = false;

            logger.debug("重新注册服务集合");

            for (String servicePath : serviceMap.keySet()) {
                registerService(servicePath, serviceMap.get(servicePath));
            }
        }
    }

    @Override
    public void handleNewSession() throws Exception {
        reRegister = true;

        logger.debug("ZooKeeper会话过期，创建新的会话");
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {

    }
}
