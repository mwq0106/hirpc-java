package com.hirpc.registry.impl;

import com.hirpc.config.ApplicationConfig;
import com.hirpc.manager.ConnectionManager;
import com.hirpc.constant.Constants;
import com.hirpc.registry.ServerInRegistry;
import com.hirpc.registry.ServerMatcher;
import com.hirpc.registry.ServiceDiscovery;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
@Component
public class ZooKeeperServiceDiscovery implements ServiceDiscovery, IZkStateListener, IZkChildListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);
    @Resource
    private ConnectionManager connectionManager;
    @Resource
    private ApplicationConfig applicationConfig;
    /**
     * 所有服务在ZooKeeper下的根节点
     */
    private final String rootPath = Constants.ZK_SERVICE_ROOT;
    private final String providerPath = Constants.ZK_SERVICE_PROVIDER;
    private final String consumerPath = Constants.ZK_SERVICE_CONSUMER;

    private boolean reRegister;

    /**
     * ZooKeeper客户端实例
     */
    private ZkClient zkClient;

    /**
     * 缓存查询出的符合条件的服务列表，key值是包名+|+匹配条件，value是符合条件的服务列表
     */
    private volatile Map<String, List<ServerInRegistry>> cachedServiceAddress = new ConcurrentHashMap<>();
    /**
     * 保存需要使用的服务及其匹配条件，服务节点变化时使用，需要判断是否与该服务器建立连接
     * key值是包名+|+匹配条件，value是匹配条件
     */
    private volatile Map<String,ServerInRegistry> matchConditionMap = new ConcurrentHashMap<>();
    /**
     * 进程ID
     */
    private String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    /**
     * 本机ip
     */
    private String localHost;

    public ZooKeeperServiceDiscovery(@Value("${hirpc.registry.address}") String zkAddress){
        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }
        zkClient = new ZkClient(zkAddress, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
        if (!zkClient.exists(rootPath)) {
            zkClient.createPersistent(rootPath);
        }
    }
    @Override
    public void discoverAndConnectServers(final String servicePath, ServerInRegistry matchCondition) throws Exception{
        String matchConditionKey = servicePath + "|" + matchCondition.toRegistryString();
        matchConditionMap.put(matchConditionKey,matchCondition);
        //发现服务获取匹配的服务器列表
        List<ServerInRegistry> matchedServers = discoverService(servicePath,matchCondition);

        //注册消费者路径
        final String serviceConsumerPath = rootPath + "/" + servicePath + consumerPath;
        if (!zkClient.exists(serviceConsumerPath)) {
            zkClient.createPersistent(serviceConsumerPath);
        }
        registerConsumer(servicePath);

        //连接服务器
        connectionManager.updateConnectedServer(matchConditionKey,matchedServers);
    }
    private void registerConsumer(final String servicePath){
        String serviceConsumerPath = rootPath + "/" + servicePath + consumerPath;
        StringBuilder sb = new StringBuilder();
        sb.append("/");
        try {
            localHost = InetAddress.getLocalHost().getHostAddress();
        }catch (Throwable e){
            logger.debug("无法获取本机ip");
        }
        sb.append("host=").append(localHost);
        sb.append("&applicationName=").append(applicationConfig.getName());
        sb.append("&pid=").append(pid);
        sb.append("&timestamp=").append(System.currentTimeMillis());
        String consumerFullPath = serviceConsumerPath + sb.toString();

        try {
            if (!this.zkClient.exists(consumerFullPath)) {
                this.zkClient.createEphemeral(consumerFullPath);
            }
        } catch (ZkNodeExistsException e) {

        }
    }
    private void updateServer(final String servicePath, ServerInRegistry matchCondition) throws Exception{
        List<ServerInRegistry> matchedServers = discoverService(servicePath,matchCondition);
        String matchConditionKey = servicePath + "|" + matchCondition.toRegistryString();
        connectionManager.updateConnectedServer(matchConditionKey,matchedServers);
    }

    private List<ServerInRegistry> discoverService(final String servicePath, ServerInRegistry matchCondition) throws Exception {
        final String serviceProviderPath = rootPath + "/" + servicePath + providerPath;
        final String cachedServiceAddressKey = servicePath + matchCondition.toRegistryString();


        if (!zkClient.exists(serviceProviderPath)) {
            throw new RuntimeException(String.format("服务路径(%s)不存在", servicePath));
        }
        //注册监听
        zkClient.subscribeChildChanges(serviceProviderPath, this);

        List<ServerInRegistry> serviceNodes = cachedServiceAddress.get(cachedServiceAddressKey);
        if(serviceNodes != null && !serviceNodes.isEmpty()){
            logger.debug("命中缓存,获取到{}服务的{}个可用节点", servicePath, serviceNodes.size());
            return serviceNodes;
        }


        List<String> childNodes = zkClient.getChildren(serviceProviderPath);
        if (childNodes == null || childNodes.size() == 0) {
            logger.debug("服务路径{}下无可用服务器节点",servicePath);
            return null;
        }
        List<ServerInRegistry> matchNodes = new LinkedList<>();
        for (String node:childNodes){
            ServerInRegistry serverInRegistry = ServerInRegistry.parse(node);
            if(ServerMatcher.isMatch(serverInRegistry,matchCondition)){
                matchNodes.add(serverInRegistry);
            }
        }
        //放入缓存
        cachedServiceAddress.put(cachedServiceAddressKey, matchNodes);
        logger.debug("获取到{}服务的{}个可用节点", servicePath, childNodes.size());
        return matchNodes;
    }
    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        if (state == Watcher.Event.KeeperState.SyncConnected) {
            logger.debug("观察到ZooKeeper事件SyncConnected");
            if(reRegister){
                reRegister = false;
                logger.debug("重新注册消费者");
                for (String key: matchConditionMap.keySet()){
                    registerConsumer(key.split("|")[0]);
                }
            }
        }
        if (state == Watcher.Event.KeeperState.Disconnected) {
            logger.debug("检测到zookeeper事件:Disconnected,清除缓存");
            cachedServiceAddress.clear(); //监听丢失
        }
        if (state == Watcher.Event.KeeperState.Expired) {
            logger.debug("检测到zookeeper事件:Expired,清除缓存");
            cachedServiceAddress.clear();
        }
    }
    @Override
    public void handleNewSession() throws Exception {
        logger.debug("ZooKeeper创建新的会话");
        reRegister = true;
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        logger.error("ZooKeeper会话过期,创建新的会话,但是失败");
    }

    /**
     * 子节点发生改变后通知
     *
     * @param parentPath    The parent path
     * @param currentChilds The children or null if the root node (parent path) was deleted.
     * @throws Exception
     */
    @Override
    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
        if(parentPath.endsWith(providerPath)){
            logger.debug("服务{}的子节点发生变化,清除该服务对应的缓存并更新连接信息", parentPath);
            String servicePath = parentPath.substring(rootPath.length() + 1,parentPath.length()-providerPath.length());
            //删除所有这个服务的缓存
            Iterator<Map.Entry<String, List<ServerInRegistry>>> it = cachedServiceAddress.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, List<ServerInRegistry>> entry = it.next();
                if(entry.getKey().startsWith(servicePath)){
                    it.remove();
                }
            }
            //更新需要连接的服务器
            for(Map.Entry<String, ServerInRegistry> entry : matchConditionMap.entrySet()){
                if(entry.getKey().startsWith(servicePath)){
                    updateServer(servicePath,entry.getValue());
                }
            }
        }
    }
}
