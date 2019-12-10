package com.hirpc.handler;

import com.hirpc.future.RpcFuture;
import com.hirpc.protocol.Request;
import com.hirpc.protocol.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mwq0106
 * @date 2019/9/22
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Response> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    private volatile ConcurrentHashMap<Long, RpcFuture> pendingRPC = new ConcurrentHashMap<>();
    private Channel channel;
    private String serverAddress;
    public RpcClientHandler(String serverAddress){
        this.serverAddress = serverAddress;
    }
//    private long lastHeartbeat = System.currentTimeMillis();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) {
        logger.debug("收到服务器响应,id：{}", response.getId());
        Long requestId = response.getId();
        RpcFuture rpcFuture = pendingRPC.get(requestId);
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);
            rpcFuture.done(response);
        }
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        logger.debug("客户端userEventTriggered事件");
//        logger.debug("距离上次时间:"+(System.currentTimeMillis()-lastHeartbeat));
//        lastHeartbeat = System.currentTimeMillis();
        if (evt instanceof IdleStateEvent) {
            try {
                Request req = new Request();
                req.setHeartBeat(true);
                ctx.channel().writeAndFlush(req).sync();
            }finally {
                if (ctx.channel() != null && !ctx.channel().isActive()) {
//                    CHANNEL_MAP.remove(ch);
                }
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }
    public RpcFuture sendRequest(Request request) {
        if(isChannelClosed()){
            throw new RuntimeException("通道已关闭,发送请求失败,请求ID:" + request.getId());
        }
        RpcFuture rpcFuture = new RpcFuture(request);
        try {
            pendingRPC.put(request.getId(), rpcFuture);
            channel.writeAndFlush(request).sync();
        }catch (Throwable e){
            pendingRPC.remove(request.getId());
            throw new RuntimeException("发送请求失败,请求ID:" + request.getId() + ",cause:" + e.getMessage(),e);
        }
        return rpcFuture;
    }
    public boolean isChannelClosed(){
        return !channel.isOpen();
    }
    public void close() {
        channel.close();
    }
    public String getServerAddress(){
        return this.serverAddress;
    }
}
