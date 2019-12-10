package com.hirpc.server;

import com.hirpc.annotation.RpcService;
import com.hirpc.annotation.RpcServiceAnnotationParser;
import com.hirpc.config.ApplicationConfig;
import com.hirpc.config.ProtocolConfig;
import com.hirpc.handler.RpcServerHandler;
import com.hirpc.protocol.PbEntityManager;
import com.hirpc.protocol.Response;
import com.hirpc.protocol.codec.RpcDecoder;
import com.hirpc.protocol.codec.RpcEncoder;
import com.hirpc.registry.ServerInRegistry;
import com.hirpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
@Component
public class RpcServer implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
    @Resource
    private ApplicationConfig applicationConfig;
    @Resource
    private ProtocolConfig protocolConfig;
    @Resource
    PbEntityManager pbEntityManager;
    /**
     * 保存服务bean的map
     */
    private Map<String, ServiceBeanWrapper> serviceMap = new HashMap<>();

    /**
     * 注册服务的接口
     */
    private ServiceRegistry serviceRegistry;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Map<String, Object> map = context.getBeansWithAnnotation(RpcService.class);
        if (map == null || map.size() == 0) {
            logger.warn("没有扫描到需要注册的服务");
            return;
        }
        for (Object serviceBean : map.values()) {
            RpcService serviceAnnotation = serviceBean.getClass().getAnnotation(RpcService.class);
            logger.debug("扫描到服务：{}", serviceBean.getClass().getName());
            if(serviceAnnotation.interfaceClass().equals(void.class)){
                Class[] interfaces = serviceBean.getClass().getInterfaces();
                if(interfaces.length == 0){
                    throw new RuntimeException("该服务没有指明注解服务接口也没有实现接口");
                }
                for (Class c:interfaces){
                    String annotationInfo = RpcServiceAnnotationParser.parseAnnotation(serviceAnnotation);
                    ServiceBeanWrapper serviceBeanWrapper = new ServiceBeanWrapper(serviceBean,annotationInfo);
                    serviceMap.put(c.getName(), serviceBeanWrapper);
                }
            }else {
                String serviceName = serviceAnnotation.interfaceClass().getName();
                String annotationInfo = RpcServiceAnnotationParser.parseAnnotation(serviceAnnotation);
                ServiceBeanWrapper serviceBeanWrapper = new ServiceBeanWrapper(serviceBean,annotationInfo);
                serviceMap.put(serviceName, serviceBeanWrapper);
            }
        }
    }

    public void start(){
        if (serviceRegistry == null) {
            throw new RuntimeException("服务中心不可用");
        }
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    //心跳
                                    .addLast(new IdleStateHandler(protocolConfig.getHeartbeatTime() * 3, 0, 0, MILLISECONDS))
                                    //第一个OutboundHandler，用于编码RPC响应
                                    .addLast(new RpcEncoder(Response.class,protocolConfig))
                                    //第一个InboundHandler，用于解码RPC请求
                                    .addLast(new RpcDecoder(pbEntityManager))
                                    //第二个InboundHandler，用于处理RPC请求并生成RPC响应
                                    .addLast(new RpcServerHandler(getPureServiceBeanMap()));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            int port = protocolConfig.getPort();

            ChannelFuture future = bootstrap.bind("0.0.0.0",port).sync();

            logger.debug("服务器已启动（端口号：{}）", port);

            registerServices();
            logger.debug("服务已全部启动完毕");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("启动服务器过程中发生异常", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    private void registerServices() {
        if (serviceRegistry == null) {
            throw new RuntimeException("服务中心不可用");
        }
        for (String servicePath : serviceMap.keySet()) {
            logger.debug("向注册中心注册服务：{}", servicePath);
            ServiceBeanWrapper serviceBeanWrapper = serviceMap.get(servicePath);
            StringBuilder zkRegistryNodeName = new StringBuilder();
            zkRegistryNodeName.append("host=").append(protocolConfig.getHost());
            zkRegistryNodeName.append("&port=").append(protocolConfig.getPort());
            if(StringUtils.isNotBlank(applicationConfig.getName())){
                zkRegistryNodeName.append("&applicationName=").append(applicationConfig.getName());
            }
            if(StringUtils.isNotBlank(serviceBeanWrapper.getAnnotationInfo())){
                zkRegistryNodeName.append("&").append(serviceBeanWrapper.getAnnotationInfo());
            }
            logger.debug("Regist to registry:"+zkRegistryNodeName.toString());
            ServerInRegistry serverInRegistry = ServerInRegistry.parse(zkRegistryNodeName.toString());

            serviceRegistry.registerService(servicePath, serverInRegistry);
        }
    }
    public void stop(){
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
            logger.debug("HOOK：workerGroup");
        }
        if (!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
            logger.debug("HOOK：bossGroup");
        }
        logger.debug("HOOK：RPC服务器已关闭");
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    private Map<String,Object> getPureServiceBeanMap(){
        Map<String,Object> pureBeanMap = new HashMap<>();
        for (String servicePath : serviceMap.keySet()) {
            pureBeanMap.put(servicePath,serviceMap.get(servicePath).getServiceBean());
        }
        return pureBeanMap;
    }
}
