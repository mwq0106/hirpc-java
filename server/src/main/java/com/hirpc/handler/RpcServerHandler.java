package com.hirpc.handler;

import com.hirpc.protocol.Request;
import com.hirpc.protocol.Response;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<Request> {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    /**
     * 保存服务bean的map
     */
    private Map<String, Object> serviceMap = new HashMap<>();
//    private long lastHeartbeat = System.currentTimeMillis();
    private static final ConcurrentMap<String,Channel> CHANNEL_MAP = new ConcurrentHashMap<>();
    public RpcServerHandler(Map<String,Object> serviceBeanMap){
        this.serviceMap = serviceBeanMap;
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        logger.debug("服务端userEventTriggered事件");
//        logger.debug("距离上次时间:"+(System.currentTimeMillis()-lastHeartbeat));
//        lastHeartbeat = System.currentTimeMillis();
        if (evt instanceof IdleStateEvent) {
            try {
                ctx.channel().close();
            }finally {
//                CHANNEL_MAP.remove(ctx.channel().remoteAddress());
            }
        }
        super.userEventTriggered(ctx, evt);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext context, final Request request) throws Exception {
        logger.debug("请求处理开始,id：{}", request.getId());
        Response response = new Response();
        response.setId(request.getId());
        try {
            Object result = handleRequest(request);
            response.setResult(result);
        } catch (Exception e) {
            logger.error("请求处理({})过程中出错", request.getId(), e);
            response.setException(e);
        }

        context.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
//                channelFuture.channel().close();

                logger.debug("请求处理完毕：{}", request.getId());
            }
        });
    }
    private Object handleRequest(Request request) throws Exception {
        String serviceFullname = request.getServicePath();

        Object serviceBean = serviceMap.get(serviceFullname);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("未找到与(%s)相对应的服务", serviceFullname));
        }

        Class<?> clazz = serviceBean.getClass();

        Method method = clazz.getMethod(request.getServiceName(), request.getParameterType());
        return method.invoke(serviceBean, request.getParameterValue());
    }
}
