package com.hirpc.protocol.codec;

import com.hirpc.config.ApplicationConfig;
import com.hirpc.config.ProtocolConfig;
import com.hirpc.constant.Constants;
import com.hirpc.protocol.Request;
import com.hirpc.protocol.Response;
import com.hirpc.protocol.protobuf.RequestPb;
import com.hirpc.protocol.protobuf.ResponsePb;
import com.hirpc.protocol.util.Bytes;
import com.hirpc.protocol.util.KryoSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mwq0106
 * @date 2019/9/22
 */

public class RpcEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);

    private static Set<Class> supportEncodeClass;
    private byte serialization;
    private String hirpcVersion;
    static {
        supportEncodeClass = new HashSet<>();
        supportEncodeClass.add(Request.class);
        supportEncodeClass.add(Response.class);
    }
    /**
     * 要编码的对象类型
     */
    private Class<?> clazz;
    public RpcEncoder(Class<?> clazz,ProtocolConfig protocolConfig) {
        this.hirpcVersion = Constants.HIRPC_VERSION;
        this.serialization = protocolConfig.getSerializationByte();
        if(supportEncodeClass.contains(clazz)){
            this.clazz = clazz;
        }else {
            throw new RuntimeException("该类型不支持编码");
        }
    }
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if (!clazz.isInstance(o)) {
            throw new RuntimeException("非法的编码请求：不兼容的编码类型:" + clazz);
        }
        byte[] header = new byte[ExchangeCodec.HEADER_LENGTH];

        header[0] = ExchangeCodec.MAGIC_HIGH;
        header[1] = ExchangeCodec.MAGIC_LOW;
        header[2] = ExchangeCodec.PROTOCOL_VERSION;
        header[6] = ExchangeCodec.UNUSED;

        if(clazz.equals(Request.class)){
            Request request = (Request) o;
            header[4] = this.serialization;
            header[5] = ExchangeCodec.NORMAL_STATUS;
            Bytes.longToBytes(request.getId(),header,7);

            if(request.isHeartBeat()){
                header[3] = ExchangeCodec.MESSAGE_TYPE_HEARTBEAT;
                Bytes.intToBytes(0,header,15);
                byteBuf.writeBytes(header);
                return;
            }else if(request.isOneWay()){
                header[3] = ExchangeCodec.MESSAGE_TYPE_ONEWAY;
            }else {
                header[3] = ExchangeCodec.MESSAGE_TYPE_REQUEST;
            }
            //默认是正常请求
            if(this.serialization == ExchangeCodec.SERIALIZATION_TYPE_KRYO){
                byte[] body = KryoSerializer.serialize(request);
                Bytes.intToBytes(body.length,header,15);
                byteBuf.writeBytes(header);
                byteBuf.writeBytes(body);
                return;
            }else if(this.serialization == ExchangeCodec.SERIALIZATION_TYPE_PROTOBUF){
                //除了Pb参数之外的全部信息
                byte[] body1;
                //pb参数数组
                byte[][] body2;
                RequestPb.RequestInner.Builder builder = RequestPb.RequestInner.newBuilder();
                if(request.getData() != null){
                    builder.setData(request.getData());
                }
                builder.setHirpcVersion(this.hirpcVersion);
                builder.setServicePath(request.getServicePath());

                String[] fullNames = getPbDescriptorFullName(request);
                for (int i = 0; i < fullNames.length; i++) {
                    builder.addParameterTypes(fullNames[i]);
                }

                builder.setServiceName(request.getServiceName());
                if(request.getServiceVersion() != null){
                    builder.setServiceVersion(request.getServiceVersion());
                }
                RequestPb.RequestInner requestInner = builder.build();
                int bodyLength = 0;
                body1 = requestInner.toByteArray();
                bodyLength += (body1.length + 4);
                body2 = getPbParameterValueBytes(request);
                for (int i = 0; i < body2.length; i++) {
                    bodyLength += (body2[i].length + 4);
                }
                Bytes.intToBytes(bodyLength,header,15);
                byteBuf.writeBytes(header);
                byteBuf.writeInt(body1.length);
                byteBuf.writeBytes(body1);
                for (int i = 0; i < body2.length; i++) {
                    byteBuf.writeInt(body2[i].length);
                    byteBuf.writeBytes(body2[i]);
                }
                return;
            }else {
                throw new RuntimeException("不支持的序列化类型!");
            }
        }else if(clazz.equals(Response.class)){
            header[3] = ExchangeCodec.MESSAGE_TYPE_RESPONSE;
            header[4] = this.serialization;
            header[5] = ExchangeCodec.NORMAL_STATUS;
            Response response = (Response) o;
            Bytes.longToBytes(response.getId(),header,7);
            if(this.serialization == ExchangeCodec.SERIALIZATION_TYPE_KRYO){
                byte[] body = KryoSerializer.serialize(response);
                Bytes.intToBytes(body.length,header,15);
                byteBuf.writeBytes(header);
                byteBuf.writeBytes(body);
                return;
            }else if(this.serialization == ExchangeCodec.SERIALIZATION_TYPE_PROTOBUF){
                ResponsePb.ResponseInner.Builder builder = ResponsePb.ResponseInner.newBuilder();
                if(response.getException() != null){
                    builder.setError(response.getException().toString());
                }
                builder.setReturnType(getPbDescriptorFullName(response));

                ResponsePb.ResponseInner responseInner = builder.build();
                int bodyLength = 0;

                //除了Pb参数之外的全部信息
                byte[] body1;
                //pb参数
                byte[] body2;

                body1 = responseInner.toByteArray();
                body2 = getPbReturnValueBytes(response);

                bodyLength += (body1.length + 4);
                bodyLength += (body2.length + 4);
                Bytes.intToBytes(bodyLength,header,15);
                byteBuf.writeBytes(header);
                byteBuf.writeInt(body1.length);
                byteBuf.writeBytes(body1);
                byteBuf.writeInt(body2.length);
                byteBuf.writeBytes(body2);
                return;
            }else {
                throw new RuntimeException("不支持的序列化类型!");
            }

        }else {
            throw new IllegalStateException("非法的编码请求：不兼容的编码类型" + clazz);
        }

    }
    private String getPbDescriptorFullName(Response response) throws Exception{
        Class pbClass=response.getResult().getClass();
        Method method1=pbClass.getMethod("getDescriptorForType");
        Object descriptor=method1.invoke(response.getResult());
        Class descriptorClass=descriptor.getClass();
        Method method2=descriptorClass.getMethod("getFullName");
        Object fullName=method2.invoke(descriptor);
        return (String) fullName;
    }
    private String[] getPbDescriptorFullName(Request request) throws Exception{
        int parameterTypeCount = request.getParameterType().length;
        String[] result = new String[parameterTypeCount];
        for (int i = 0; i < parameterTypeCount; i++) {
            Class pbClass = request.getParameterType()[i];
            Method method1 = pbClass.getMethod("getDescriptorForType");
            Object descriptor = method1.invoke(request.getParameterValue()[i]);
            Class descriptorClass = descriptor.getClass();
            Method method2 = descriptorClass.getMethod("getFullName");
            Object fullName = method2.invoke(descriptor);
            result[i]= (String) fullName;
        }
        return result;
    }
    private byte[][] getPbParameterValueBytes(Request request) throws Exception{
        byte[][] result = new byte[request.getParameterType().length][];
        for (int i = 0; i < request.getParameterType().length; i++) {
            Class pbClass = request.getParameterType()[i];
            Method method = pbClass.getMethod("toByteArray");
            result[i] = (byte[]) method.invoke(request.getParameterValue()[i]);
        }
        return result;
    }
    private byte[] getPbReturnValueBytes(Response response) throws Exception{
        Class pbClass=response.getResult().getClass();
        Method method=pbClass.getMethod("toByteArray");
        Object result=method.invoke(response.getResult());
        return (byte[]) result;
    }
    public static void main(String[] args) throws Exception
    {
        RequestPb.RequestInner.Builder builder=RequestPb.RequestInner.newBuilder();
        builder.addParameterTypes("a");
        builder.setHirpcVersion("");
        builder.setServicePath("");
        builder.setServiceName("");
        builder.setServiceVersion("");
        RequestPb.RequestInner requestInner=builder.build();
        System.out.println(requestInner.getDescriptorForType().getFullName());
        Class pbClass=requestInner.getClass();
        Method method1=pbClass.getMethod("getDescriptorForType");
        Object descriptor=method1.invoke(requestInner);
        Class descriptorClass=descriptor.getClass();
        Method method2=descriptorClass.getMethod("getFullName");
        Object fullName=method2.invoke(descriptor);
        System.out.println(fullName);
    }
}
