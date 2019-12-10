package com.hirpc.protocol.codec;

/**
 * @author mwq0106
 * @date 2019/9/23
 */
public class ExchangeCodec {
    public static final int HEADER_LENGTH = 19;
    public static final byte MAGIC_HIGH = 0x12;
    public static final byte MAGIC_LOW = 0x34;

    public static final byte PROTOCOL_VERSION = 0x1;

    public static final byte MESSAGE_TYPE_REQUEST = 0x1;
    public static final byte MESSAGE_TYPE_ONEWAY = 0x2;
    public static final byte MESSAGE_TYPE_HEARTBEAT = 0x3;
    public static final byte MESSAGE_TYPE_RESPONSE = 0x4;
    public static final byte MESSAGE_TYPE_EVENT = 0x5;

    public static final byte SERIALIZATION_TYPE_PROTOBUF = 0x1;
    public static final byte SERIALIZATION_TYPE_KRYO = 0x2;
    public static final byte UNUSED = 0x0;
    public static final byte NORMAL_STATUS = 0x0;
}
