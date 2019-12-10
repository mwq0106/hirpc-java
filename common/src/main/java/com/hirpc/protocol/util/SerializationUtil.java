package com.hirpc.protocol.util;

/**
 * @author mwq0106
 * @date 2019/9/23
 */
public class SerializationUtil {

    private SerializationUtil() {

    }
    public static <T> byte[] serialize(T message) {
        return KryoSerializer.serialize(message);
    }
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        return KryoSerializer.deserialize(data);
    }
}
