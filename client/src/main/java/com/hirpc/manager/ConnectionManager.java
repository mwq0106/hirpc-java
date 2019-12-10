package com.hirpc.manager;

import com.hirpc.config.ProtocolConfig;
import com.hirpc.handler.RpcClientHandler;
import com.hirpc.protocol.PbEntityManager;
import com.hirpc.protocol.Request;
import com.hirpc.protocol.codec.RpcDecoder;
import com.hirpc.protocol.codec.RpcEncoder;
import com.hirpc.registry.ServerInRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author mwq0106
 * @date 2019/9/21
 */
@Component
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * key值是ip:port,value是RpcClientHandler
     */
    private Map<String, RpcClientHandler> handlerMap = new ConcurrentHashMap<>();
    /**
     * key值是服务路径+|+服务属性，value是符合条件的服务器列表
     */
    private Map<String, List<ServerInRegistry>> connectedServer = new ConcurrentHashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
    private NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
    @Resource
    private ProtocolConfig protocolConfig;

    @Resource
    private PbEntityManager pbEntityManager;
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long connectTimeoutMillis = 6000;

    /**
     * 根据服务路径加服务属性获取所有可用的handler
     * @param matchConditionKey 服务路径+|+服务属性
     * @return
     */
    public List<RpcClientHandler> getClientHandlers(String matchConditionKey){
        List<ServerInRegistry> servers = connectedServer.get(matchConditionKey);
        if(servers == null || servers.isEmpty()){
            try {
                boolean available = waitingForHandler();
                if (available) {
                    servers = connectedServer.get(matchConditionKey);
                }
                if(servers == null || servers.isEmpty()){
                    throw new RuntimeException("没有可用的服务器来进行服务调用，查找条件:" + matchConditionKey);
                }
            } catch (InterruptedException e) {
                logger.debug("waitingForHandler time out");
                throw new RuntimeException("没有可用的服务器来进行服务调用，查找条件:" + matchConditionKey);
            }
        }
        List<RpcClientHandler> clientHandlers = new LinkedList<>();
        for (ServerInRegistry server: servers){
            clientHandlers.add(handlerMap.get(server.getAddress()));
        }
        return clientHandlers;
    }

    /**
     * 根据服务路径加服务属性更新连接服务器
     * @param matchConditionKey 连接服务器的条件，服务路径+|+服务属性
     * @param allServer 要连接的服务器
     */
    public void updateConnectedServer(String matchConditionKey,List<ServerInRegistry> allServer) {
        if(allServer == null || allServer.isEmpty()){
            logger.warn("No available server node. All server nodes are down");
            for (String key: handlerMap.keySet()){
                //我们需要关闭已经连接上服务器的链接吗？
                handlerMap.get(key).close();
            }
            handlerMap.clear();
            connectedServer.clear();
            return;
        }
        //与新服务器建立链接
        for (ServerInRegistry serverInRegistry : allServer){
            if(!handlerMap.containsKey(serverInRegistry.getAddress())){
                connectServerNode(matchConditionKey,serverInRegistry);
            }
        }
        HashSet<String> newAllServerNodeSet = new HashSet<>();
        for (ServerInRegistry serverInRegistry : allServer){
            newAllServerNodeSet.add(serverInRegistry.getAddress());
        }
        //删除不在列表中的链接
        Iterator<Map.Entry<String, RpcClientHandler>> it = handlerMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, RpcClientHandler> entry = it.next();
            if(!newAllServerNodeSet.contains(entry.getKey())){
                RpcClientHandler handler = entry.getValue();
                logger.debug("Remove invalid server node:" + handler.getServerAddress());
                handler.close();
                it.remove();
            }
        }
        List<ServerInRegistry> connectedServers = connectedServer.get(matchConditionKey);
        Iterator<ServerInRegistry> serverInRegistryIterator = connectedServers.iterator();
        while (serverInRegistryIterator.hasNext()){
            ServerInRegistry serverInRegistry = serverInRegistryIterator.next();
            if(!newAllServerNodeSet.contains(serverInRegistry.getAddress())){
                serverInRegistryIterator.remove();
            }
        }
    }
    private void connectServerNode(final String matchConditionKey,final ServerInRegistry serverInRegistry){
        final String host = serverInRegistry.getHost();
        final int port = Integer.parseInt(serverInRegistry.getPort());
        final String serverAddress = serverInRegistry.getAddress();
        if(connectedServer.get(matchConditionKey) == null){
            List<ServerInRegistry> emptyServer = new LinkedList<>();
            connectedServer.put(matchConditionKey,emptyServer);
        }
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(nioEventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) {
                                channel.pipeline()
                                        .addLast(new IdleStateHandler(0, protocolConfig.getHeartbeatTime(), 0, MILLISECONDS))
                                        .addLast(new RpcEncoder(Request.class,protocolConfig))
                                        .addLast(new RpcDecoder(pbEntityManager))
                                        .addLast(new RpcClientHandler(serverAddress));
                            }
                        })
                        .option(ChannelOption.TCP_NODELAY, true);
                logger.debug("连接到服务器：{}", serverAddress);
                ChannelFuture future = bootstrap.connect(host, port);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) {
                        if (channelFuture.isSuccess()) {
//                            logger.debug("Successfully connect to rpc remote server:" + serverAddress);
                            logger.debug("成功连接到RPC远程服务器:" + serverAddress);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            handlerMap.put(serverAddress,handler);
                            List<ServerInRegistry> servers = connectedServer.get(matchConditionKey);
                            servers.add(serverInRegistry);
                            signalAvailableHandler();
                        }else {
//                            logger.error("Fail to connect to remote server:" + serverAddress);
                            logger.error("连接RPC远程服务器失败，地址:" + serverAddress);
                        }
                    }
                });
            }
        });
    }
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }
    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
