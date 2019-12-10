package com.hirpc.config;

import com.hirpc.constant.Constants;
import com.hirpc.protocol.codec.ExchangeCodec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author mwq0106
 * @date 2019/9/25
 */
@Configuration
@EnableConfigurationProperties(ProtocolConfig.class)
@ConfigurationProperties(prefix = "hirpc.protocol")
public class ProtocolConfig {

    private String name;

    private String host;

    private Integer port;

    private String serialization = Constants.PROTOCOL_SERIALIZATION_PROTOBUF;

    private String protobufScanBasePackages;

    private Integer heartbeatTime = Constants.DEFAULT_PROTOCOL_HEARTBEATTIME;

    public void setHeartbeatTime(Integer heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public Integer getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setProtobufScanBasePackages(String protobufScanBasePackages) {
        this.protobufScanBasePackages = protobufScanBasePackages;
    }

    public String getProtobufScanBasePackages() {
        return protobufScanBasePackages;
    }


    public byte getSerializationByte(){
        if(serialization.equals(Constants.PROTOCOL_SERIALIZATION_PROTOBUF)){
            return ExchangeCodec.SERIALIZATION_TYPE_PROTOBUF;
        }else if(serialization.equals(Constants.PROTOCOL_SERIALIZATION_KRYO)){
            return ExchangeCodec.SERIALIZATION_TYPE_KRYO;
        }else {
            throw new RuntimeException("协议版本不正确");
        }
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public String getSerialization() {
        return serialization;
    }

}
