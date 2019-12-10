package com.hirpc.constant;

/**
 * @author mwq0106
 * @date 2019/10/16
 */
public abstract class Constants {

    private Constants() {
    }

    /**
     * ZooKeeper默认会话超时时间
     */
    public static final int DEFAULT_ZK_SESSION_TIMEOUT = 5 * 1000;

    /**
     * ZooKeeper默认连接超时时间
     */
    public static final int DEFAULT_ZK_CONNECTION_TIMEOUT = 3 * 1000;


    public static final String ZK_SERVICE_ROOT = "/hirpc";
    public static final String ZK_SERVICE_PROVIDER = "/provider";
    public static final String ZK_SERVICE_CONSUMER = "/consumer";

    public static final String PROTOCOL_SERIALIZATION_PROTOBUF = "protobuf";
    public static final String PROTOCOL_SERIALIZATION_KRYO = "kryo";

    public static final Integer DEFAULT_PROTOCOL_HEARTBEATTIME = 60;
    public static final String HIRPC_VERSION = "1.0";

}