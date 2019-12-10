package com.hirpc.protocol.codec;

import com.hirpc.config.ProtocolConfig;
import com.hirpc.protocol.PbEntityManager;
import com.hirpc.protocol.Request;
import com.hirpc.protocol.Response;
import com.hirpc.protocol.protobuf.RequestPb;
import com.hirpc.protocol.protobuf.ResponsePb;
import com.hirpc.protocol.util.Bytes;
import com.hirpc.protocol.util.KryoSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author mwq0106
 * @date 2019/9/25
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    private Method requestDecodeMethod;
    private Method responseDecodeMethod;
    private PbEntityManager pbEntityManager;
    {
        try {
            requestDecodeMethod = RequestPb.RequestInner.class.getMethod("parseFrom",byte[].class);
            responseDecodeMethod = ResponsePb.ResponseInner.class.getMethod("parseFrom",byte[].class);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public RpcDecoder(PbEntityManager pbEntityManager){
        this.pbEntityManager = pbEntityManager;
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        logger.debug("收到数据包");
        int save = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();
        byte[] header = new byte[Math.min(readable, ExchangeCodec.HEADER_LENGTH)];
        byteBuf.readBytes(header);
        //去除脏数据
        if (readable > 0 && header[0] != ExchangeCodec.MAGIC_HIGH
                || readable > 1 && header[1] != ExchangeCodec.MAGIC_LOW) {
            int length = header.length;
            if (header.length < readable) {
                header = Bytes.copyOf(header, readable);
                byteBuf.readBytes(header, length, readable - length);
            }
            for (int i = 1; i < header.length - 1; i++) {
                if (header[i] == ExchangeCodec.MAGIC_HIGH && header[i + 1] == ExchangeCodec.MAGIC_LOW) {
                    byteBuf.readerIndex(byteBuf.readerIndex() - header.length + i);
                    header = Bytes.copyOf(header, i);
                    break;
                }
            }
            logger.warn("未识别的网络数据包,将丢弃,{}",header);
            return;
        }
        if (readable < ExchangeCodec.HEADER_LENGTH ) {
            byteBuf.readerIndex(save);
            return;
        }
        if(header[2] != ExchangeCodec.PROTOCOL_VERSION){
            throw new RuntimeException("协议版本错误");
        }
        byte messageType = header[3];
        byte serializationType = header[4];
        if(messageType == ExchangeCodec.MESSAGE_TYPE_HEARTBEAT){
            long requestId = Bytes.bytesToLong(header,7);
            logger.debug("收到心跳,id:" + requestId);
            return;
        }
        int bodyLength = Bytes.bytesToInt(header,15);
        //检查数据是否已经完全到达
        if (readable < ExchangeCodec.HEADER_LENGTH + bodyLength) {
            byteBuf.readerIndex(save);
            return;
        }
        if(serializationType == ExchangeCodec.SERIALIZATION_TYPE_KRYO){
            byte[] body = new byte[bodyLength];
            byteBuf.readBytes(body);
            if(messageType == ExchangeCodec.MESSAGE_TYPE_REQUEST) {
                long requestId = Bytes.bytesToLong(header, 7);
                logger.debug("收到请求,id:" + requestId);
                Request request = KryoSerializer.deserialize(body);
                list.add(request);
            } else if(messageType == ExchangeCodec.MESSAGE_TYPE_RESPONSE) {
                long responseId = Bytes.bytesToLong(header, 7);
                logger.debug("收到response Id:" + responseId);
                Response response = KryoSerializer.deserialize(body);
                list.add(response);
            }else if(messageType == ExchangeCodec.MESSAGE_TYPE_ONEWAY){

            }else {

            }
        }else if(serializationType == ExchangeCodec.SERIALIZATION_TYPE_PROTOBUF){
            if(messageType == ExchangeCodec.MESSAGE_TYPE_REQUEST){
                long requestId = Bytes.bytesToLong(header,7);
                logger.debug("收到请求,id:" + requestId);

                int body1Length = byteBuf.readInt();
                byte[] body1Data = new byte[body1Length];
                byteBuf.readBytes(body1Data);
                Object body1 = requestDecodeMethod.invoke(null,body1Data);
                RequestPb.RequestInner requestInner = (RequestPb.RequestInner) body1;
                Request request = new Request();
                request.setServiceName(requestInner.getServiceName());
                request.setServicePath(requestInner.getServicePath());
                request.setData(requestInner.getData());
                request.setHeader(requestInner.getHeaderMap());
                request.setId(requestId);
                request.setServiceVersion(requestInner.getServiceVersion());

                int parameterTypesCount = requestInner.getParameterTypesCount();
                Class[] parameterType = new Class[parameterTypesCount];
                Object[] parameterValue = new Object[parameterTypesCount];
                for (int i = 0; i < parameterTypesCount; i++) {
                    Class pbClass = pbEntityManager.getProtobufEntity(requestInner.getParameterTypes(i));
                    if(pbClass == null){
                        throw new RuntimeException("服务端没有所传的ProtoBuf参数类型:" + requestInner.getParameterTypes(i));
                    }
                    parameterType[i] = pbClass;
                    int parameterByteLength = byteBuf.readInt();
                    byte[] parameterData = new byte[parameterByteLength];
                    byteBuf.readBytes(parameterData);
                    Method method2 = pbClass.getMethod("parseFrom",byte[].class);
                    Object parameterObject = method2.invoke(null,parameterData);
                    parameterValue[i] = parameterObject;
                }
                request.setParameterType(parameterType);
                request.setParameterValue(parameterValue);
                list.add(request);
            }else if(messageType == ExchangeCodec.MESSAGE_TYPE_RESPONSE){
                long responseId = Bytes.bytesToLong(header,7);
                logger.debug("收到response Id:" + responseId);
                int body1Length = byteBuf.readInt();
                byte[] body1Data = new byte[body1Length];
                byteBuf.readBytes(body1Data);

                Object body1 = responseDecodeMethod.invoke(null,body1Data);
                ResponsePb.ResponseInner responseInner = (ResponsePb.ResponseInner) body1;
                String returnType = responseInner.getReturnType();
                Class pbClass = pbEntityManager.getProtobufEntity(returnType);
                if(pbClass == null){
                    throw new RuntimeException("客户端没有所传的ProtoBuf参数类型:"+returnType);
                }

                Method method2 = pbClass.getMethod("parseFrom",byte[].class);
                int body2Length = byteBuf.readInt();
                byte[] body2Data = new byte[body2Length];
                byteBuf.readBytes(body2Data);

                Object body2 = method2.invoke(null,body2Data);
                Response response = new Response();
                response.setId(responseId);
                response.setResult(body2);
                if(StringUtils.isNotBlank(responseInner.getError())){
                    response.setException(new RuntimeException(responseInner.getError()));
                }
                list.add(response);
            }else if(messageType == ExchangeCodec.MESSAGE_TYPE_ONEWAY){

            }else {
                throw new RuntimeException("不支持的消息类型");
            }
        }else {
            throw new RuntimeException("不支持的序列化类型");
        }

    }

}
