package com.hirpc.client;

import com.hirpc.annotation.RpcReference;
import com.hirpc.failmode.FailMode;
import com.hirpc.loadbalance.Balancer;
import com.hirpc.future.RpcFuture;
import com.hirpc.handler.RpcClientHandler;
import com.hirpc.manager.ConnectionManager;
import com.hirpc.protocol.Request;
import com.hirpc.protocol.Response;
import com.hirpc.registry.ServerInRegistry;
import com.hirpc.registry.ServiceDiscovery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author mwq0106
 * @date 2019/9/21
 */
@Component
public class RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    /**
     * 代理对象的缓存
     */
    private Map<String, Object> serviceProxyCached = new HashMap<>();

    @Resource
    private ConnectionManager connectionManager;

    /**
     * 异步调用执行回调函数线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    /**
     * 生成服务代理对象
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> clazz, final RpcReference rpcReference){
        final String servicePath = clazz.getName();
        Object proxy;
        if ((proxy = serviceProxyCached.get(servicePath)) == null) {
            synchronized (this) {
                if ((proxy = serviceProxyCached.get(servicePath)) == null) {
                    proxy = Proxy.newProxyInstance(
                            clazz.getClassLoader(),
                            new Class<?>[]{clazz},
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                                    Request request = new Request();
                                    request.setServicePath(clazz.getName());
                                    request.setServiceName(method.getName());
                                    request.setParameterType(method.getParameterTypes());
                                    request.setParameterValue(args);
                                    //todo 其他信息需要从其他地方获取,比如header,serviceVersion可以考虑从注解，hirpcVersion从全局配置获取


                                    ServerInRegistry condition = getServerMatchCondition(rpcReference);
                                    String matchCondition = request.getServicePath() + "|" + condition.toRegistryString();
                                    List<RpcClientHandler> clientHandlers = connectionManager.getClientHandlers(matchCondition);
                                    if (clientHandlers == null || clientHandlers.isEmpty()) {
                                        throw new RuntimeException("未查询到服务：" + request.getServicePath());
                                    }
                                    //todo 这里还需要再加上匹配条件作为key?
                                    RpcClientHandler electedHandler = rpcReference.loadBalance().balancer.elect(request.getServicePath(),clientHandlers);
                                    logger.debug("选取服务节点：{}", electedHandler.getServerAddress());

                                    long requestStartTime = System.currentTimeMillis();
                                    Response response;
                                    try {
                                        RpcFuture rpcFuture = electedHandler.sendRequest(request);
                                        response = (Response) rpcFuture.get();
                                    }catch (Throwable e){
                                        //失败策略
                                        if(rpcReference.failMode().equals(FailMode.FAIL_RETRY)){
                                            RpcFuture rpcFuture = electedHandler.sendRequest(request);
                                            response = (Response) rpcFuture.get();
                                        }else if(rpcReference.failMode().equals(FailMode.FAIL_OVER)){
                                            electedHandler = rpcReference.loadBalance().balancer.elect(request.getServicePath(),clientHandlers);
                                            RpcFuture rpcFuture = electedHandler.sendRequest(request);
                                            response = (Response) rpcFuture.get();
                                        }else {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    long requestTimeCost = System.currentTimeMillis() - requestStartTime;

                                    if (response == null) {
                                        throw new RuntimeException(String.format("空的服务器响应(请求号为%s)", request.getId()));
                                    }

                                    logger.debug("请求{}耗时：{}ms", request.getId(), requestTimeCost);

                                    if (response.getException() != null) {
                                        throw response.getException();
                                    } else {
                                        return response.getResult();
                                    }

                                }
                            });
                    serviceProxyCached.put(servicePath, proxy);
                }
            }
        }
        return (T) proxy;
    }
    private ServerInRegistry getServerMatchCondition(RpcReference rpcReference){
        //todo RpcReference注解修改需要更新
        ServerInRegistry serverInRegistry = new ServerInRegistry();
        if(StringUtils.isNotBlank(rpcReference.group())){
            serverInRegistry.setGroup(rpcReference.group());
        }
        if (StringUtils.isNotBlank(rpcReference.version())){
            serverInRegistry.setVersion(rpcReference.version());
        }
        return serverInRegistry;
    }
    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

}
